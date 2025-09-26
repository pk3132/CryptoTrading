package com.tradingbot.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Service
public class EMA200TrendlineStrategy {
    private static final Logger logger = LoggerFactory.getLogger(EMA200TrendlineStrategy.class);
    
    // Configuration
    private static final int LOOKBACK_PERIOD = 50;
    private static final int EMA_PERIOD = 200;
    private static final double STOP_LOSS_PCT = 0.005; // 0.50%
    private static final double TAKE_PROFIT_PCT = 0.01; // 1.00%
    private static final int SWING_POINTS_FOR_TRENDLINE = 3; // Use last 3 swing points for more robust trendlines
    private static final boolean REQUIRE_CLOSE_BREAKOUT = true; // Require close candle breakout, not wick
    private static final double FRESH_TRENDLINE_MIN_DISTANCE = 0.002; // 0.2% minimum distance for NEW trendlines 
    private static final int MIN_CANDLES_BETWEEN_FORMATIONS = 5; // Minimum candles before considering new trendline
    
    // ================== NEW STATE TRACKERS ==================
    private Map<String, String> activePosition = new HashMap<>(); // symbol -> BUY/SELL/NONE
    private Map<String, Integer> lastExitIndex = new HashMap<>(); // symbol -> last exit candle index
    private static final int COOLDOWN_CANDLES = 5; // wait 5 candles after exit
    // ========================================================
    
    // Data structures
    public static class Candle {
        public long time;
        public double open, high, low, close, volume;
        public LocalDateTime datetime;
        public double ema200;
        public int signal;
        public int position;
        public double swingHigh = Double.NaN;
        public double swingLow = Double.NaN;
        public double resistanceLine = Double.NaN;
        public double supportLine = Double.NaN;
        
        public Candle(long time, double open, double high, double low, double close, double volume) {
            this.time = time;
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.volume = volume;
            this.datetime = LocalDateTime.ofInstant(Instant.ofEpochSecond(time), ZoneOffset.UTC);
        }
    }
    
    public static class SwingPoint {
        public int index;
        public double price;
        
        public SwingPoint(int index, double price) {
            this.index = index;
            this.price = price;
        }
    }
    
    public static class TradeSignal {
        private String type;
        private double entryPrice;
        private double stopLoss;
        private double takeProfit;
        private String reason;

        public TradeSignal(String type, double entryPrice, double stopLoss, double takeProfit, String reason) {
            this.type = type;
            this.entryPrice = entryPrice;
            this.stopLoss = stopLoss;
            this.takeProfit = takeProfit;
            this.reason = reason;
        }

        // Getters
        public String getType() { return type; }
        public double getEntryPrice() { return entryPrice; }
        public double getStopLoss() { return stopLoss; }
        public double getTakeProfit() { return takeProfit; }
        public String getReason() { return reason; }
    }
    
    public static class TrendLine {
        public double slope;
        public double intercept;
        
        public TrendLine(double slope, double intercept) {
            this.slope = slope;
            this.intercept = intercept;
        }
        
        public double getValue(int x) {
            return slope * x + intercept;
        }
    }
    
    private final Map<String, List<Candle>> candlesData;
    private HttpClient httpClient;
    private ObjectMapper objectMapper;
    
    public EMA200TrendlineStrategy() {
        this.candlesData = new HashMap<>();
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public void addCandleData(String symbol, List<Map<String, Object>> rawCandles) {
        List<Candle> candles = new ArrayList<>();
        for (Map<String, Object> c : rawCandles) {
            candles.add(new Candle(
                ((Number) c.get("time")).longValue(),
                ((Number) c.get("open")).doubleValue(),
                ((Number) c.get("high")).doubleValue(),
                ((Number) c.get("low")).doubleValue(),
                ((Number) c.get("close")).doubleValue(),
                ((Number) c.get("volume")).doubleValue()
            ));
        }
        candlesData.put(symbol, candles);
    }

    public List<TradeSignal> checkSignals(String symbol) {
        List<TradeSignal> signals = new ArrayList<>();
        List<Candle> candles = candlesData.get(symbol);

        if (candles == null || candles.size() < EMA_PERIOD + LOOKBACK_PERIOD) {
            return signals;
        }

        calculateEMA(candles, EMA_PERIOD);
        List<SwingPoint>[] swingPoints = findSwingPoints(candles);
        List<SwingPoint> highs = swingPoints[0];
        List<SwingPoint> lows = swingPoints[1];

        // ✅ Position state check
        String currentPos = activePosition.getOrDefault(symbol, "NONE");
        int x_now = candles.size() - 1;
        
        // ✅ Cooldown check
        if (lastExitIndex.containsKey(symbol)) {
            int lastExit = lastExitIndex.get(symbol);
            if (x_now - lastExit < COOLDOWN_CANDLES) {
                logger.info("⏸️ {} - Cooldown active, skipping new trade (candles: {}/{})", 
                           symbol, x_now - lastExit, COOLDOWN_CANDLES);
                return signals;
            }
        }

        // Get current real-time price from candles (more reliable than tickers endpoint)
        double currentPrice = 0;
        try {
            // Use the getCurrentMarketPrice method which uses candles endpoint
            double marketPrice = getCurrentMarketPrice(symbol);
            if (marketPrice > 0) {
                currentPrice = marketPrice;
            }
        } catch (Exception ignored) {}
        
        // IMPORTANT: Use current mark price, not last candle close, for accurate EMA200 comparison
        if (currentPrice <= 0) {
            // If we can't get current mark price, skip this signal generation
            logger.warn("⚠️ Cannot get current market price for {}, skipping signal generation", symbol);
            return signals;
        }

        Candle lastCandle = candles.get(x_now);
        double emaValue = lastCandle.ema200; // Use EMA from last full candle

        // Build trendlines with last N swing highs/lows for more robust trendlines
        List<SwingPoint> recentHighs = new ArrayList<>();
        List<SwingPoint> recentLows = new ArrayList<>();
        
        // Get last N swing highs for resistance trendline (minimum 2 required)
        int nHighs = Math.min(SWING_POINTS_FOR_TRENDLINE, highs.size());
        if (nHighs >= 2) {
            for (int i = highs.size() - nHighs; i < highs.size(); i++) {
                recentHighs.add(highs.get(i));
            }
        }
        
        // Get last N swing lows for support trendline (minimum 2 required)
        int nLows = Math.min(SWING_POINTS_FOR_TRENDLINE, lows.size());
        if (nLows >= 2) {
            for (int i = lows.size() - nLows; i < lows.size(); i++) {
                recentLows.add(lows.get(i));
            }
        }

        // Fit trendlines
        TrendLine resistanceLine = fitTrendline(recentHighs);
        TrendLine supportLine = fitTrendline(recentLows);

        // Log trendline details for verification
        logger.info("📊 Trendline Analysis for {}: Highs={}, Lows={}, Using {} swing points each", 
                   symbol, highs.size(), lows.size(), SWING_POINTS_FOR_TRENDLINE);
        
        if (resistanceLine != null) {
            logger.info("🔴 Resistance Line: Using {} points, Slope={:.6f}, Intercept={:.2f}", 
                       recentHighs.size(), resistanceLine.slope, resistanceLine.intercept);
        }
        
        if (supportLine != null) {
            logger.info("🟢 Support Line: Using {} points, Slope={:.6f}, Intercept={:.2f}", 
                       recentLows.size(), supportLine.slope, supportLine.intercept);
        }

        // Calculate trendline values at current position (index of the last candle)
        // Index alignment: x_now is already defined above (candles.size() - 1)

        // Compute trendline values using formula
        double supportValue = 0;
        double resistanceValue = 0;
        
        if (supportLine != null) {
            supportValue = supportLine.getValue(x_now);
        }
        
        if (resistanceLine != null) {
            resistanceValue = resistanceLine.getValue(x_now);
        }
        
        // STEP 0: CRITICAL NEW TRENDLINE PREPARATION VALIDATION  
        // Must have NEW trendline built before accepting ANY signals
        if (resistanceLine == null && supportLine == null) {
            logger.info("⏰ {} - No trendlines prepared yet, waiting for swing point formation", symbol);
            return signals;
        }
        
        // Enhanced validation: Smart trendline freshness check
        boolean isMarketStructureFresh = isMarketReadyForNewTrendline(symbol, resistanceValue, supportValue, 0.0, 0.0);
        
        if (resistanceLine != null && supportLine != null && resistanceValue == supportValue && resistanceValue > 0) {
            logger.info("⚠️ {} - Perfectly aligned levels - wait for better trend differentiation", symbol);
            return signals;
        }
        
        // Only continue if we detect fresh trendline structure is prepared
        logger.info("📊 {} trendline analysis - Resistance: {:.2f}, Support: {:.2f}", 
                   symbol, resistanceValue, supportValue);
        
        // Use smarter market-adapted logic instead of rigid percentage requirements  
        if (!isMarketStructureFresh && resistanceLine != null && supportLine != null) {
            logger.info("⏸️ {} - Market not ready: Waiting for new swing point formation/evolution", symbol);
            return signals;
        }
        
        boolean hasNewResistance = (resistanceLine != null) && isMarketStructureFresh;
        boolean hasNewSupport = (supportLine != null) && isMarketStructureFresh;  
        
        logger.info("🎯 {} trendline status: New Resistance: {}, New Support: {}", 
                   symbol, hasNewResistance, hasNewSupport);

        // Trading Logic: THREE-STEP PROCESS  
        // STEP 1: Check EMA200 Filter FIRST
        // STEP 2: Confirm FRESH trendline is prepared
        // STEP 3: Only then, check trendline breakout
        
        // BUY Logic: BTC > EMA200 FIRST, then check Resistance Breakout
        if (currentPrice > emaValue) {
            // STEP 1 PASSED: BTC > EMA200 ✅ 
            // STEP 2: Check resistance breakout
            if (resistanceLine != null) {
                boolean breakoutConfirmed = false;
                String breakoutType = "";
                
                if (REQUIRE_CLOSE_BREAKOUT) {
                    // Require close candle breakout (not wick)
                    breakoutConfirmed = lastCandle.close > resistanceValue;
                    breakoutType = "Close Breakout";
                } else {
                    // Allow wick breakout
                    breakoutConfirmed = currentPrice > resistanceValue;
                    breakoutType = "Price Breakout";
                }
                
                if (breakoutConfirmed && !currentPos.equals("BUY")) {  // prevent duplicate BUY
                    // CONDITIONS MET: BTC > EMA200 + Resistance Breakout = BUY
                    logger.info("✅ BUY Signal Confirmed: breakout at {} vs resistance {}", 
                               lastCandle.close, resistanceValue);
                    double stopLoss = currentPrice * (1 - STOP_LOSS_PCT);
                    double takeProfit = currentPrice * (1 + TAKE_PROFIT_PCT);
                    signals.add(new TradeSignal("BUY", currentPrice, stopLoss, takeProfit,
                        String.format("BUY Signal: BTC > EMA200 (%.2f > %.2f) + Resistance %s (Close: %.2f > %.2f)", 
                            currentPrice, emaValue, breakoutType, lastCandle.close, resistanceValue)));
                    
                    // Record position
                    activePosition.put(symbol, "BUY");
                    logger.info("🎯 {} position set to BUY", symbol);
                } else if (!breakoutConfirmed) {
                    logger.debug("❌ BUY Signal Blocked: Resistance breakout not confirmed (Close: %.2f, Resistance: %.2f)", 
                                lastCandle.close, resistanceValue);
                } else if (currentPos.equals("BUY")) {
                    logger.debug("🔄 {} - Already in BUY position, skipping duplicate", symbol);
                }
            }
        }
        
        // SELL Logic: BTC < EMA200 FIRST, then check Support Breakout
        if (currentPrice < emaValue) {
            // STEP 1 PASSED: BTC < EMA200 ✅  
            // STEP 2: Check support breakout
            if (supportLine != null) {
                boolean breakoutConfirmed = false;
                String breakoutType = "";
                
                if (REQUIRE_CLOSE_BREAKOUT) {
                    // Require close candle breakout (not wick)
                    breakoutConfirmed = lastCandle.close < supportValue;
                    breakoutType = "Close Breakout";
                } else {
                    // Allow wick breakout
                    breakoutConfirmed = currentPrice < supportValue;
                    breakoutType = "Price Breakout";
                }
                
                if (breakoutConfirmed && !currentPos.equals("SELL")) {  // prevent duplicate SELL
                    // CONDITIONS MET: BTC < EMA200 + Support Breakout = SELL
                    logger.info("✅ SELL Signal Confirmed: breakout at {} vs support {}", 
                               lastCandle.close, supportValue);
                    double stopLoss = currentPrice * (1 + STOP_LOSS_PCT);
                    double takeProfit = currentPrice * (1 - TAKE_PROFIT_PCT);
                    signals.add(new TradeSignal("SELL", currentPrice, stopLoss, takeProfit,
                        String.format("SELL Signal: BTC < EMA200 (%.2f < %.2f) + Support %s (Close: %.2f < %.2f)", 
                            currentPrice, emaValue, breakoutType, lastCandle.close, supportValue)));
                    
                    // Record position
                    activePosition.put(symbol, "SELL");
                    logger.info("🎯 {} position set to SELL", symbol);
                } else if (!breakoutConfirmed) {
                    logger.debug("❌ SELL Signal Blocked: Support breakout not confirmed (Close: %.2f, Support: %.2f)", 
                                lastCandle.close, supportValue);
                } else if (currentPos.equals("SELL")) {
                    logger.debug("🔄 {} - Already in SELL position, skipping duplicate", symbol);
                }
            }
        }
        return signals;
    }

    private double getCurrentMarketPrice(String symbol) {
        try {
            long now = System.currentTimeMillis() / 1000;
            String url = "https://api.india.delta.exchange/v2/history/candles?resolution=1m&symbol=" + symbol + 
                        "&start=" + (now - 300) + 
                        "&end=" + now;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode rootNode = objectMapper.readTree(response.body());
                JsonNode resultNode = rootNode.get("result");
                
                if (resultNode != null && resultNode.isArray() && resultNode.size() > 0) {
                    JsonNode lastCandle = resultNode.get(resultNode.size() - 1);
                    JsonNode closeNode = lastCandle.get("close");
                    
                    if (closeNode != null && closeNode.isNumber()) {
                        return closeNode.asDouble();
                    }
                }
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    public void calculateEMA(List<Candle> candles, int period) {
        if (candles.size() < period) return;
        
        double multiplier = 2.0 / (period + 1);
        
        // Calculate initial SMA for first EMA value
        double sum = 0;
        for (int i = 0; i < period; i++) {
            sum += candles.get(i).close;
        }
        candles.get(period - 1).ema200 = sum / period;
        
        // Calculate EMA for remaining candles
        for (int i = period; i < candles.size(); i++) {
            double ema = (candles.get(i).close - candles.get(i - 1).ema200) * multiplier + candles.get(i - 1).ema200;
            candles.get(i).ema200 = ema;
        }
    }
    
    public List<SwingPoint>[] findSwingPoints(List<Candle> candles) {
        List<SwingPoint> highs = new ArrayList<>();
        List<SwingPoint> lows = new ArrayList<>();
        
        // Improved swing point detection based on sample code
        for (int i = 1; i < candles.size() - 1; i++) {
            // Swing High: high > previous high AND high > next high
            if (candles.get(i).high > candles.get(i-1).high &&
                candles.get(i).high > candles.get(i+1).high) {
                highs.add(new SwingPoint(i, candles.get(i).high));
                candles.get(i).swingHigh = candles.get(i).high;
            }
            
            // Swing Low: low < previous low AND low < next low
            if (candles.get(i).low < candles.get(i-1).low &&
                candles.get(i).low < candles.get(i+1).low) {
                lows.add(new SwingPoint(i, candles.get(i).low));
                candles.get(i).swingLow = candles.get(i).low;
            }
        }
        
        @SuppressWarnings("unchecked")
        List<SwingPoint>[] result = new List[]{highs, lows};
        return result;
    }
    
    public TrendLine fitTrendline(List<SwingPoint> points) {
        if (points.size() < 2) return null;
        
        SimpleRegression regression = new SimpleRegression();
        for (SwingPoint point : points) {
            regression.addData(point.index, point.price);
        }
        
        return new TrendLine(regression.getSlope(), regression.getIntercept());
    }
    
    /**
     * Enhanced freshness detection using multiple market indicators
     */
    private boolean isFreshTrendline(String symbol, double resistanceValue, double supportValue, 
                                   Double previousResistanceLevel, Double previousSupportLevel) {
        
        boolean resistanceIsFresh = true;
        boolean supportIsFresh = true;
        
        if (resistanceValue > 0 && previousResistanceLevel != null) {
            double resistanceDiff = Math.abs(resistanceValue - previousResistanceLevel) / previousResistanceLevel;
            resistanceIsFresh = resistanceDiff >= FRESH_TRENDLINE_MIN_DISTANCE;
            
            logger.debug("🔍 Resistance Validation: Current={:.2f}, Previous={:.2f}, Diff={:.3%} (Fresh: {})", 
                        resistanceValue, previousResistanceLevel, resistanceDiff, resistanceIsFresh);
        }
        
        if (supportValue > 0 && previousSupportLevel != null) {
            double supportDiff = Math.abs(supportValue - previousSupportLevel) / previousSupportLevel;
            supportIsFresh = supportDiff >= FRESH_TRENDLINE_MIN_DISTANCE;
            
            logger.debug("🔍 Support Validation: Current={:.2f}, Previous={:.2f}, Diff={:.3%} (Fresh: {})", 
                        supportValue, previousSupportLevel, supportDiff, supportIsFresh);
        }
        
        // Enhanced validation: Also check if enough time has passed for fresh formation
        boolean adequateTiming = true; // Will be enhanced with time-based logic
        
        return (resistanceIsFresh || supportIsFresh) && adequateTiming;
    }
    
    /**
     * Smart trendline freshness that adapts to market closeness
     */
    private boolean isMarketReadyForNewTrendline(String symbol, double currentResistance, double currentSupport, 
                                               Double lastUsedResistance, Double lastUsedSupport) {
        
        // Multi-factor approach:
        // 1. Basic distance check (relaxed for tight markets)
        // 2. Price action diversity (swing point variety) 
        // 3. Market structure evolution
        byte freshFactors = 0;
        
        // Distance factor (relaxed)
        if (lastUsedResistance != null && currentResistance > 0) {
            double resistanceGap = Math.abs(currentResistance - lastUsedResistance) / lastUsedResistance;
            if (resistanceGap > FRESH_TRENDLINE_MIN_DISTANCE) {
                logger.info("📊 {} fresh resistance gap {:.3%}", symbol, resistanceGap);
                freshFactors++;
            }
        } else if (currentResistance > 0) {
            freshFactors++; // First-time formation
        }
        
        if (lastUsedSupport != null && currentSupport > 0) {
            double supportGap = Math.abs(currentSupport - lastUsedSupport) / lastUsedSupport;
            if (supportGap > FRESH_TRENDLINE_MIN_DISTANCE) {
                logger.info("📊 {} fresh support gap {:.3%}", symbol, supportGap);
                freshFactors++;
            }
        } else if (currentSupport > 0) {
            freshFactors++; // First-time formation
        }
        
        // Check market structure maturity (count new swing points)
        boolean structureIsEvolving = checkMarketStructureEvolution(symbol);
        
        if (structureIsEvolving) {
            logger.info("🔄 {} - New structure forming (possible edge-sensing scenario)", symbol);
            return true; // Allow trendline/signal even for closer moves  
        }
        
        return freshFactors >= 1; // Only need 1 factor + enough variance OR first round
    }
    
    /**
     * Check if market structure is evolving with new swing points
     */
    private boolean checkMarketStructureEvolution(String symbol) {
        List<Candle> candles = candlesData.get(symbol);
        if (candles == null || candles.size() < MIN_CANDLES_BETWEEN_FORMATIONS) {
            return false;
        }
        
        int recentNewSwingPoints = 0;
        for (int i = Math.max(0, candles.size() - 10); i < candles.size(); i++) {
            Candle c = candles.get(i);
            if (Double.isFinite(c.swingHigh) || Double.isFinite(c.swingLow)) {
                recentNewSwingPoints++;
            }
        }
        
        // Market evolving if recent swing formation has developed = enough price action diversity
        return recentNewSwingPoints >= 2;
    }
    
    /**
     * Advanced pre-signal validation for fresh trendlines 
     */
    private boolean isValidForTrading(String symbol, boolean hasResistance, boolean hasSupport, 
                                    Double previousResistance, Double previousSupport) {
        
        if (!hasResistance && !hasSupport) {
            logger.info("⏰ {} - No trendlines detected yet, waiting for swing point formation", symbol);
            return false; // Must have at least one trendline
        }
        
        // Check if we have significant new structural changes
        if (hasResistance && hasSupport) {
            logger.info("🛡️ {} - BOTH trendlines active - checking freshness", symbol);
            return true; // If both are detected fresh, good for both BUY/SELL signals
        }
        
        if (hasResistance) {
            boolean resistanceChanges = (previousResistance == null) || 
                (previousResistance > 0 && 
                 Math.abs(getCurrentResistanceValue(symbol) - previousResistance) / previousResistance >= FRESH_TRENDLINE_MIN_DISTANCE);
            if (resistanceChanges) {
                logger.info("🚀 {} - NEW resistance structure prepared at {}", symbol, getCurrentResistanceValue(symbol));
                return true;
            }
        }
        
        if (hasSupport) {
            boolean supportChanges = (previousSupport == null) ||
                (previousSupport > 0 && 
                 Math.abs(getCurrentSupportValue(symbol) - previousSupport) / previousSupport >= FRESH_TRENDLINE_MIN_DISTANCE);
            if (supportChanges) {
                logger.info("🛡️ {} - NEW support structure prepared at {}", symbol, getCurrentSupportValue(symbol));
                return true;
            }
        }
        
        logger.info("⏸️ {} - Reusing same trendline levels - waiting for fresh structure", symbol);
        return false;
    }
    
    // Helper to get resistance value for validation
    private double getCurrentResistanceValue(String symbol) {
        List<Candle> candles = candlesData.get(symbol);
        if (candles == null || candles.isEmpty()) return 0;
        
        TrendLine resistanceLine = getOrBuildResistanceLine(candles);
        if (resistanceLine != null) {
            return resistanceLine.getValue(candles.size() - 1);
        }
        return 0;
    }
    
    // Helper to get support value for validation
    private double getCurrentSupportValue(String symbol) {
        List<Candle> candles = candlesData.get(symbol);
        if (candles == null || candles.isEmpty()) return 0;
        
        TrendLine supportLine = getOrBuildSupportLine(candles);
        if (supportLine != null) {
            return supportLine.getValue(candles.size() - 1);
        }
        return 0;
    }
    
    // Build resistance line
    private TrendLine getOrBuildResistanceLine(List<Candle> candles) {
        List<SwingPoint>[] swings = findSwingPoints(candles);
        List<SwingPoint> highs = swings[0];
        List<SwingPoint> recentHighs = new ArrayList<>();
        
        int nHighs = Math.min(SWING_POINTS_FOR_TRENDLINE, highs.size());
        if (nHighs >= 2) {
            for (int i = highs.size() - nHighs; i < highs.size(); i++) {
                recentHighs.add(highs.get(i));
            }
        }
        
        return fitTrendline(recentHighs);
    }
    
    // Build support line  
    private TrendLine getOrBuildSupportLine(List<Candle> candles) {
        List<SwingPoint>[] swings = findSwingPoints(candles);
        List<SwingPoint> lows = swings[1];
        List<SwingPoint> recentLows = new ArrayList<>();
        
        int nLows = Math.min(SWING_POINTS_FOR_TRENDLINE, lows.size());
        if (nLows >= 2) {
            for (int i = lows.size() - nLows; i < lows.size(); i++) {
                recentLows.add(lows.get(i));
            }
        }
        
        return fitTrendline(recentLows);
    }

    /**
     * Return last EMA-200 value computed for the given symbol, if available.
     * Requires that candle data has been added; will compute EMA if missing.
     */
    public Double getLastEma200(String symbol) {
        List<Candle> candles = candlesData.get(symbol);
        if (candles == null || candles.size() < EMA_PERIOD) {
            return null;
        }
        // Ensure EMA is populated
        if (candles.get(candles.size() - 1).ema200 == 0.0) {
            calculateEMA(candles, EMA_PERIOD);
        }
        return candles.get(candles.size() - 1).ema200;
    }
    
    /**
     * ================== NEW METHOD ==================
     * Called when trade closes to track exit and start cooldown
     */
    public void closeTrade(String symbol, String reason) {
        activePosition.put(symbol, "NONE");                 // reset position
        List<Candle> candles = candlesData.get(symbol);
        if (candles != null && !candles.isEmpty()) {
            lastExitIndex.put(symbol, candles.size() - 1); // mark exit candle
        }
        logger.info("✅ {} trade closed. Reason: {}. Cooldown started ({} candles required).", 
                     symbol, reason, COOLDOWN_CANDLES);
    }
    
    /**
     * Check current position state for symbol
     */
    public String getCurrentPosition(String symbol) {
        return activePosition.getOrDefault(symbol, "NONE");
    }
    
    /**
     * Get cooldown info
     */
    public String getCooldownInfo(String symbol) {
        if (lastExitIndex.containsKey(symbol)) {
            List<Candle> candles = candlesData.get(symbol);
            if (candles != null) {
                int currentIndex = candles.size() - 1;
                int lastExit = lastExitIndex.get(symbol);
                int remainingCandles = COOLDOWN_CANDLES - (currentIndex - lastExit);
                if (remainingCandles > 0) {
                    return String.format("%s: %d/%d candles remaining", symbol, remainingCandles, COOLDOWN_CANDLES);
                }
            }
        }
        return symbol + ": No cooldown active";
    }
    
    /**
     * Validates trendline freshness across the system using adaptive logic
     * Multi-factor freshness instead of rigid percentage thresholds
     */
    public boolean validateFreshTrendlineStructure(String symbol) {
        List<Candle> candles = candlesData.get(symbol);
        if (candles == null || candles.isEmpty()) return false;
            
        // Use our enhanced smart validation with minimal gaps (0.2% not 2%)
        boolean newResistanceReady = isMarketReadyForNewTrendline(symbol, 0, 0, 0.0, 0.0);
        boolean newSupportReady = isMarketReadyForNewTrendline(symbol, 0, 0, 0.0, 0.0);
        
        // Also check recent structural evolution for more trading opportunities
        boolean structureEvolving = checkMarketStructureEvolution(symbol);
        
        return newResistanceReady || newSupportReady || structureEvolving;
    }
}