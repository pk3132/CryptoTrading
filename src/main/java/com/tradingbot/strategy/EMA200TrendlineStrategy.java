package com.tradingbot.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingbot.service.PositionChecker;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.ArrayList;

@Service
public class EMA200TrendlineStrategy {
    private static final Logger logger = LoggerFactory.getLogger(EMA200TrendlineStrategy.class);
    
    @Autowired
    private PositionChecker positionChecker;
    
    // Configuration
    private static final int LOOKBACK_PERIOD = 50;
    private static final int EMA_PERIOD = 200;
    private static final double STOP_LOSS_PCT = 0.002; // 0.20%
    private static final double TAKE_PROFIT_PCT = 0.002; // 0.20%
    private static final int SWING_POINTS_FOR_TRENDLINE = 3; // Use last 3 swing points for more robust trendlines
    private static final int SWING_LOOKBACK = 5; // Check 5 candles on each side for stronger swing detection
    private static final boolean REQUIRE_CLOSE_BREAKOUT = true; // Require close candle breakout, not wick
    private static final double FRESH_TRENDLINE_MIN_DISTANCE = 0.002; // 0.2% minimum distance for NEW trendlines 
    private static final int MIN_CANDLES_BETWEEN_FORMATIONS = 5; // Minimum candles before considering new trendline
    
    // ================== NEW STATE TRACKERS ==================
    private Map<String, String> activePosition = new HashMap<>(); // symbol -> BUY/SELL/NONE
    private Map<String, Integer> lastExitCandleIndex = new HashMap<>(); // symbol -> candle index at exit
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

    @Autowired
    private com.tradingbot.service.DeltaApiClient deltaApiClient;

    
    public EMA200TrendlineStrategy() {
        this.candlesData = new HashMap<>();
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public Double getHigherTimeframeEma200(String symbol, String resolution) {
        try {
            long now = System.currentTimeMillis() / 1000;
            long secondsPerCandle;
            switch (resolution) {
                case "15m": secondsPerCandle = 900L; break;
                case "5m": secondsPerCandle = 300L; break;
                case "1h": secondsPerCandle = 3600L; break;
                default: secondsPerCandle = 60L; // 1m
            }
            long start = now - (500L * secondsPerCandle);
            List<Map<String, Object>> ohlcv = deltaApiClient.fetchOhlcv(symbol, resolution, start, now);
            if (ohlcv == null || ohlcv.size() < EMA_PERIOD) {
                logger.warn("⚠️ {} {} - Insufficient candles: {}", symbol, resolution, ohlcv == null ? "null" : ohlcv.size());
                return null;
            }
            // Build close prices list
            List<Double> closes = new ArrayList<>(ohlcv.size());
            for (Map<String, Object> c : ohlcv) {
                Object close = c.get("close");
                if (close instanceof Number) {
                    closes.add(((Number) close).doubleValue());
                }
            }
            if (closes.size() < EMA_PERIOD) return null;
            
            double ema = EMA200Calculator.getEMA200(closes.subList(Math.max(0, closes.size() - 500), closes.size()));
            logger.info("📊 {} {} EMA200 calculated: {} (from {} candles)", symbol, resolution, String.format("%.2f", ema), closes.size());
            return ema;
        } catch (Exception e) {
            logger.error("❌ Error calculating EMA200 for {} {}: {}", symbol, resolution, e.getMessage());
            return null;
        }
    }

    // SL/TP now strictly percentage-based (rollback from points)

    public void addCandleData(String symbol, List<Map<String, Object>> rawCandles) {
        List<Candle> newCandles = new ArrayList<>();
        for (Map<String, Object> c : rawCandles) {
            newCandles.add(new Candle(
                ((Number) c.get("time")).longValue(),
                ((Number) c.get("open")).doubleValue(),
                ((Number) c.get("high")).doubleValue(),
                ((Number) c.get("low")).doubleValue(),
                ((Number) c.get("close")).doubleValue(),
                ((Number) c.get("volume")).doubleValue()
            ));
        }
        
        // 🔧 FIX: Track candle shifts to maintain correct exit indices
        List<Candle> oldCandles = candlesData.get(symbol);
        if (oldCandles != null && !oldCandles.isEmpty() && lastExitCandleIndex.containsKey(symbol)) {
            long oldLatestTime = oldCandles.get(oldCandles.size() - 1).time;
            long newLatestTime = newCandles.get(newCandles.size() - 1).time;
            
            if (newLatestTime > oldLatestTime) {
                // Candle resolution in seconds (1m = 60s)
                int resolutionSeconds = 60;
                int newCandlesCount = (int) ((newLatestTime - oldLatestTime) / resolutionSeconds);
                
                int currentExitIndex = lastExitCandleIndex.get(symbol);
                int adjustedExitIndex = currentExitIndex - newCandlesCount;
                
                if (adjustedExitIndex >= 0) {
                    lastExitCandleIndex.put(symbol, adjustedExitIndex);
                    logger.debug("🔄 {} - Adjusted exit index: {} → {} (new candles: {})",
                                symbol, currentExitIndex, adjustedExitIndex, newCandlesCount);
                } else {
                    logger.info("🗑️ {} - Exit candle expired from window, clearing cooldown", symbol);
                    lastExitCandleIndex.remove(symbol);
                }
            }
        }
        
        candlesData.put(symbol, newCandles);
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
        
        // 📌 Position State Logging - Always log current state
        int lastExitIndex = lastExitCandleIndex.getOrDefault(symbol, -1);
        int candlesSinceExitForLog = (lastExitIndex == -1) ? -1 : (x_now - lastExitIndex);
        logger.info("📌 {} Position State: {}, Candles Since Exit: {}/{}", 
                    symbol, currentPos, (candlesSinceExitForLog >= 0 ? candlesSinceExitForLog : 0), COOLDOWN_CANDLES);
        
        // 🛡️ Auto-reset Safeguard - Check for state mismatch (bidirectional)
        try {
            boolean dbHasPosition = positionChecker.hasOpenPosition(symbol);
            
            // Case 1: Memory says position exists, but DB says NO
            if (!dbHasPosition && !"NONE".equals(currentPos)) {
                logger.warn("⚠️ State mismatch detected for {}. Bot memory says position={}, " +
                           "but DB shows NONE. Resetting state.", symbol, currentPos);
                activePosition.put(symbol, "NONE");
                lastExitCandleIndex.remove(symbol); // Clear cooldown
                logger.info("✅ {} state auto-reset to NONE - Ready for new trades", symbol);
            }
            
            // Case 2: DB says position exists, but Memory says NONE (e.g., after restart)
            if (dbHasPosition && "NONE".equals(currentPos)) {
                logger.warn("⚠️ Reverse mismatch detected for {}. DB has OPEN position, " +
                           "but bot memory says NONE. This can happen after restart.", symbol);
                logger.info("ℹ️ {} - Skipping new trades as DB position exists. " +
                           "Wait for SL/TP to close it.", symbol);
                // Don't generate new signals if DB has open position
                return signals; // Return empty signals
            }
        } catch (Exception e) {
            logger.error("❌ Error checking position state for {}: {}", symbol, e.getMessage());
        }
        
        // ================== COOLDOWN CHECK (index-based) ==================
        if (lastExitCandleIndex.containsKey(symbol)) {
            int lastExit = lastExitCandleIndex.get(symbol);
            int candlesSinceExit = x_now - lastExit;
            
            if (candlesSinceExit < 0) {
                // Defensive: reset state if data regression happened
                logger.warn("⚠️ {} - Inconsistent candle indexing: currentIndex={} < lastExitIndex={}. Resetting cooldown.",
                            symbol, x_now, lastExit);
                lastExitCandleIndex.remove(symbol);
            } else if (candlesSinceExit < COOLDOWN_CANDLES) {
                logger.info("⏸️ {} - Cooldown active, skipping new trade (candles: {}/{})",
                            symbol, candlesSinceExit, COOLDOWN_CANDLES);
                return signals; // block trade
            } else {
                logger.info("✅ {} - Cooldown expired, trade signals enabled (candles: {}/{})",
                            symbol, candlesSinceExit, COOLDOWN_CANDLES);
                // clear cooldown so we don't re-evaluate every cycle
                lastExitCandleIndex.remove(symbol);
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
        double emaValue = lastCandle.ema200;

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
            logger.info("🔴 Resistance Line: Using {} points, Slope={}, Intercept={}", 
                       recentHighs.size(), String.format("%.6f", resistanceLine.slope), String.format("%.2f", resistanceLine.intercept));
        }
        
        if (supportLine != null) {
            logger.info("🟢 Support Line: Using {} points, Slope={}, Intercept={}", 
                       recentLows.size(), String.format("%.6f", supportLine.slope), String.format("%.2f", supportLine.intercept));
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
        logger.info("📊 {} trendline analysis - Resistance: {}, Support: {}", 
                   symbol, String.format("%.2f", resistanceValue), String.format("%.2f", supportValue));
        
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
        
        // BUY Logic: Price above EMA200 + Resistance Breakout on 15m
        if (currentPrice > emaValue) {
            // STEP 1 PASSED: BTC > EMA200 ✅ 
            // STEP 2: Check resistance breakout
            if (resistanceLine != null && hasNewResistance) {
                boolean breakoutConfirmed = false;

                if (REQUIRE_CLOSE_BREAKOUT) {
                    if (lastCandle.close > resistanceValue) {
                        breakoutConfirmed = true;
                        logger.info("✅ {} BUY Breakout Confirmed by CLOSE: Close={} > Resistance={}", 
                                    symbol, lastCandle.close, resistanceValue);
                    } else {
                        logger.info("❌ {} BUY Breakout Rejected: Candle closed below resistance. "
                                   + "Close={} <= Resistance={} (Wick may have crossed)", 
                                    symbol, lastCandle.close, resistanceValue);
                    }
                } else {
                    if (currentPrice > resistanceValue) {
                        breakoutConfirmed = true;
                        logger.info("✅ {} BUY Breakout Confirmed by PRICE Wick: Price={} > Resistance={}", 
                                    symbol, currentPrice, resistanceValue);
                    } else {
                        logger.info("❌ {} BUY Breakout Rejected: Price did not cross resistance. "
                                   + "Price={} <= Resistance={}", 
                                    symbol, currentPrice, resistanceValue);
                    }
                }

                if (breakoutConfirmed && !currentPos.equals("BUY")) {
                    double stopLoss = currentPrice * (1 - STOP_LOSS_PCT);
                    double takeProfit = currentPrice * (1 + TAKE_PROFIT_PCT);
                    signals.add(new TradeSignal("BUY", currentPrice, stopLoss, takeProfit,
                        String.format("BUY: %s > EMA200 + Resistance Breakout at Close %.2f vs Resistance %.2f", 
                                      symbol, lastCandle.close, resistanceValue)));
                    
                    // Record position
                    activePosition.put(symbol, "BUY");
                    logger.info("🎯 {} position set to BUY", symbol);
                } else if (currentPos.equals("BUY")) {
                    logger.debug("🔄 {} - Already in BUY position, skipping duplicate", symbol);
                }
            }
        }
        
        // SELL Logic: Price below EMA200 + Support Breakout on 15m
        if (currentPrice < emaValue) {
            // STEP 1 PASSED: BTC < EMA200 ✅  
            // STEP 2: Check support breakout
            if (supportLine != null && hasNewSupport) {
                boolean breakoutConfirmed = false;

                if (REQUIRE_CLOSE_BREAKOUT) {
                    if (lastCandle.close < supportValue) {
                        breakoutConfirmed = true;
                        logger.info("✅ {} SELL Breakout Confirmed by CLOSE: Close={} < Support={}", 
                                    symbol, lastCandle.close, supportValue);
                    } else {
                        logger.info("❌ {} SELL Breakout Rejected: Candle closed above support. "
                                   + "Close={} >= Support={} (Wick may have crossed)", 
                                    symbol, lastCandle.close, supportValue);
                    }
                } else {
                    if (currentPrice < supportValue) {
                        breakoutConfirmed = true;
                        logger.info("✅ {} SELL Breakout Confirmed by PRICE Wick: Price={} < Support={}", 
                                    symbol, currentPrice, supportValue);
                    } else {
                        logger.info("❌ {} SELL Breakout Rejected: Price did not cross support. "
                                   + "Price={} >= Support={}", 
                                    symbol, currentPrice, supportValue);
                    }
                }

                if (breakoutConfirmed && !currentPos.equals("SELL")) {
                    double stopLoss = currentPrice * (1 + STOP_LOSS_PCT);
                    double takeProfit = currentPrice * (1 - TAKE_PROFIT_PCT);
                    signals.add(new TradeSignal("SELL", currentPrice, stopLoss, takeProfit,
                        String.format("SELL: %s < EMA200 + Support Breakdown at Close %.2f vs Support %.2f", 
                                      symbol, lastCandle.close, supportValue)));
                    
                    // Record position
                    activePosition.put(symbol, "SELL");
                    logger.info("🎯 {} position set to SELL", symbol);
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
        
        // For EMA calculation, use 500 candles for both symbols
        // This method is called with all candles, but we'll use optimal range
        int optimalCandles = 500; // Use 500 candles for both BTC and ETH
        int startIndex = Math.max(0, candles.size() - optimalCandles);
        
        // Extract close prices from optimal range
        List<Double> closePrices = new ArrayList<>();
        for (int i = startIndex; i < candles.size(); i++) {
            closePrices.add(candles.get(i).close);
        }
        
        // Use the professional EMA calculator
        double[] emaValues = EMA200Calculator.calculateEMA(closePrices, period);
        
        // Store EMA values back in candles (only for the optimal range)
        for (int i = 0; i < emaValues.length; i++) {
            int candleIndex = startIndex + i;
            if (candleIndex < candles.size()) {
                candles.get(candleIndex).ema200 = emaValues[i];
            }
        }
    }
    
    public List<SwingPoint>[] findSwingPoints(List<Candle> candles) {
        List<SwingPoint> highs = new ArrayList<>();
        List<SwingPoint> lows = new ArrayList<>();
        
        // Enhanced swing point detection - check multiple candles on each side
        for (int i = SWING_LOOKBACK; i < candles.size() - SWING_LOOKBACK; i++) {
            boolean isSwingHigh = true;
            boolean isSwingLow = true;
            
            // Check if current candle is higher/lower than SWING_LOOKBACK candles on each side
            for (int j = 1; j <= SWING_LOOKBACK; j++) {
                if (candles.get(i).high <= candles.get(i - j).high || 
                    candles.get(i).high <= candles.get(i + j).high) {
                    isSwingHigh = false;
                }
                if (candles.get(i).low >= candles.get(i - j).low || 
                    candles.get(i).low >= candles.get(i + j).low) {
                    isSwingLow = false;
                }
            }
            
            if (isSwingHigh) {
                highs.add(new SwingPoint(i, candles.get(i).high));
                candles.get(i).swingHigh = candles.get(i).high;
            }
            
            if (isSwingLow) {
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
                logger.info("📊 {} fresh resistance gap {}", symbol, String.format("%.3f%%", resistanceGap * 100));
                freshFactors++;
            }
        } else if (currentResistance > 0) {
            freshFactors++; // First-time formation
        }
        
        if (lastUsedSupport != null && currentSupport > 0) {
            double supportGap = Math.abs(currentSupport - lastUsedSupport) / lastUsedSupport;
            if (supportGap > FRESH_TRENDLINE_MIN_DISTANCE) {
                logger.info("📊 {} fresh support gap {}", symbol, String.format("%.3f%%", supportGap * 100));
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
     * Uses the professional EMA200Calculator for accurate results.
     */
    public Double getLastEma200(String symbol) {
        List<Candle> candles = candlesData.get(symbol);
        if (candles == null || candles.size() < EMA_PERIOD) {
            return null;
        }
        
        // For EMA calculation, use 500 candles for both symbols
        int optimalCandles = 500;
        int startIndex = Math.max(0, candles.size() - optimalCandles);
        
        // Extract close prices from optimal range
        List<Double> closePrices = new ArrayList<>();
        for (int i = startIndex; i < candles.size(); i++) {
            closePrices.add(candles.get(i).close);
        }
        
        try {
            // Use professional EMA calculator with optimal candles
            return EMA200Calculator.getEMA200(closePrices);
        } catch (Exception e) {
            logger.warn("Error calculating EMA 200 for {}: {}", symbol, e.getMessage());
            return null;
        }
    }
    
    /**
     * ================== NEW METHOD ==================
     * Called when trade closes to track exit and start cooldown
     */
    public void closeTrade(String symbol, String reason) {
        activePosition.put(symbol, "NONE"); // reset position
        List<Candle> candles = candlesData.get(symbol);
        if (candles != null && !candles.isEmpty()) {
            int exitIndex = candles.size() - 1; // index of the exit candle
            lastExitCandleIndex.put(symbol, exitIndex);
            
            logger.info("✅ {} trade closed at candle index {}. Reason: {}. Cooldown started.",
                        symbol, exitIndex, reason);
            logger.info("   📅 Exit candle time: {}", 
                       LocalDateTime.ofEpochSecond(candles.get(exitIndex).time, 0, ZoneOffset.UTC).toString());
        } else {
            // if no candle present, remove any previous cooldown to avoid permanent block
            lastExitCandleIndex.remove(symbol);
            logger.warn("⚠️ {} trade closed but no candle data available. Reason: {}", symbol, reason);
        }
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
        if (lastExitCandleIndex.containsKey(symbol)) {
            List<Candle> candles = candlesData.get(symbol);
            if (candles != null) {
                int currentIndex = candles.size() - 1;
                int lastExit = lastExitCandleIndex.get(symbol);
                int candlesSinceExit = currentIndex - lastExit;
                int remainingCandles = COOLDOWN_CANDLES - candlesSinceExit;
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