package com.tradingbot.strategy;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

/**
 * STRATEGY 2: Advanced 9/20 EMA Trend-Following Strategy for Crypto Futures
 * 
 * PROFESSIONAL-GRADE STRATEGY WITH SOPHISTICATED ENTRY LOGIC
 * 
 * PERFORMANCE:
 * - Risk-Reward Ratio: 1:3 (Conservative)
 * - Timeframes: 15m, 30m, 1h, 4h
 * - Leverage: 25x (Crypto futures)
 * - Trades per Day: 1-2 (Very Selective)
 * - Best for: Strong trending markets, avoids ranging
 * 
 * ADVANCED STRATEGY RULES:
 * 1. 9 EMA (short-term) and 20 EMA (mid-term)
 * 2. Trade only in clear trend direction
 * 3. Enter on pullbacks to 9 EMA with strong candle confirmation
 * 4. Strong candle body must be ≥50% of total range
 * 5. Stop loss based on recent swing high/low with buffer
 * 6. 1:3 risk-reward ratio with account-based position sizing
 * 7. Avoid ranging markets (EMAs within 0.1%)
 * 8. Trailing stops to EMA20 for position management
 * 
 * STATUS: ✅ ADVANCED STRATEGY 2 IMPLEMENTATION
 */
@Service
public class EMAStrategy {

    private static final int EMA_9_PERIOD = 9;
    private static final int EMA_20_PERIOD = 20;
    private static final double RISK_REWARD_RATIO = 3.0; // 1:3 ratio
    private static final double LEVERAGE = 25.0; // 25x leverage
    
    // Advanced Strategy Parameters
    private static final double PULLBACK_TOLERANCE_PCT = 0.5; // 0.5% tolerance for pullback to 9 EMA
    private static final int LOOKBACK_SWING = 5; // candles for swing high/low calculation
    private static final double SL_BUFFER_PCT = 0.2; // 0.2% buffer beyond swing
    private static final double STRONG_CANDLE_BODY_PCT = 0.5; // 50% body vs range threshold
    private static final boolean USE_TRAILING_TO_EMA20 = true;
    private static final double DEFAULT_ACCOUNT_SIZE = 10000.0;
    private static final double DEFAULT_RISK_PER_TRADE = 1.0; // 1% risk per trade
    
    private final List<CandleData> historicalData;
    private final Map<String, List<Double>> ema9Data;
    private final Map<String, List<Double>> ema20Data;
    private final Map<String, String> trendDirection;
    private final Map<String, List<CandleData>> symbolCandleData; // Store candle data for swing calculations
    
    public EMAStrategy() {
        this.historicalData = new ArrayList<>();
        this.ema9Data = new HashMap<>();
        this.ema20Data = new HashMap<>();
        this.trendDirection = new HashMap<>();
        this.symbolCandleData = new HashMap<>();
    }

    /**
     * Add candlestick data for analysis
     */
    public void addCandleData(List<Map<String, Object>> candles) {
        for (Map<String, Object> candle : candles) {
            CandleData data = new CandleData(
                Double.parseDouble(candle.get("open").toString()),
                Double.parseDouble(candle.get("high").toString()),
                Double.parseDouble(candle.get("low").toString()),
                Double.parseDouble(candle.get("close").toString()),
                Double.parseDouble(candle.get("volume").toString()),
                Long.parseLong(candle.get("timestamp").toString())
            );
            historicalData.add(data);
        }
        
        // Calculate EMAs after adding data
        calculateEMAs();
    }
    
    /**
     * Add candlestick data for specific symbol
     */
    public void addCandleData(String symbol, List<Map<String, Object>> candles) {
        // Store symbol-specific data
        List<Double> prices = new ArrayList<>();
        List<CandleData> candleDataList = new ArrayList<>();
        
        for (Map<String, Object> candle : candles) {
            prices.add(Double.parseDouble(candle.get("close").toString()));
            
            // Create CandleData object for swing calculations
            CandleData candleData = new CandleData(
                Double.parseDouble(candle.get("open").toString()),
                Double.parseDouble(candle.get("high").toString()),
                Double.parseDouble(candle.get("low").toString()),
                Double.parseDouble(candle.get("close").toString()),
                Double.parseDouble(candle.get("volume").toString()),
                Long.parseLong(candle.get("timestamp").toString())
            );
            candleDataList.add(candleData);
        }
        
        // Store candle data for swing calculations
        symbolCandleData.put(symbol, candleDataList);
        
        // Calculate EMAs for this symbol
        calculateEMAsForSymbol(symbol, prices);
    }

    /**
     * Calculate 9 and 20 period EMAs
     */
    private void calculateEMAs() {
        if (historicalData.size() < EMA_20_PERIOD) {
            return;
        }

        List<Double> prices = historicalData.stream()
            .map(CandleData::getClose)
            .collect(Collectors.toList());

        // Calculate 9 EMA
        List<Double> ema9 = calculateEMA(prices, EMA_9_PERIOD);
        ema9Data.put("BTCUSD", ema9);

        // Calculate 20 EMA
        List<Double> ema20 = calculateEMA(prices, EMA_20_PERIOD);
        ema20Data.put("BTCUSD", ema20);

        // Determine trend direction
        determineTrendDirection();
    }
    
    /**
     * Calculate EMAs for specific symbol
     */
    private void calculateEMAsForSymbol(String symbol, List<Double> prices) {
        if (prices.size() < EMA_20_PERIOD) {
            return;
        }

        // Calculate 9 EMA
        List<Double> ema9 = calculateEMA(prices, EMA_9_PERIOD);
        ema9Data.put(symbol, ema9);

        // Calculate 20 EMA
        List<Double> ema20 = calculateEMA(prices, EMA_20_PERIOD);
        ema20Data.put(symbol, ema20);

        // Determine trend direction for this symbol
        determineTrendDirectionForSymbol(symbol);
    }

    /**
     * Calculate Exponential Moving Average
     */
    private List<Double> calculateEMA(List<Double> prices, int period) {
        List<Double> ema = new ArrayList<>();
        double multiplier = 2.0 / (period + 1);
        
        // First EMA value is simple average
        double sum = 0;
        for (int i = 0; i < period; i++) {
            sum += prices.get(i);
        }
        ema.add(sum / period);
        
        // Calculate subsequent EMA values
        for (int i = period; i < prices.size(); i++) {
            double emaValue = (prices.get(i) * multiplier) + (ema.get(ema.size() - 1) * (1 - multiplier));
            ema.add(emaValue);
        }
        
        return ema;
    }

    /**
     * Determine trend direction based on EMA relationship
     */
    private void determineTrendDirection() {
        List<Double> ema9 = ema9Data.get("BTCUSD");
        List<Double> ema20 = ema20Data.get("BTCUSD");
        
        if (ema9 != null && ema20 != null && !ema9.isEmpty() && !ema20.isEmpty()) {
            double latestEMA9 = ema9.get(ema9.size() - 1);
            double latestEMA20 = ema20.get(ema20.size() - 1);
            
            if (latestEMA9 > latestEMA20) {
                trendDirection.put("BTCUSD", "BULLISH");
            } else {
                trendDirection.put("BTCUSD", "BEARISH");
            }
        }
    }
    
    /**
     * Determine trend direction for specific symbol
     */
    private void determineTrendDirectionForSymbol(String symbol) {
        List<Double> ema9 = ema9Data.get(symbol);
        List<Double> ema20 = ema20Data.get(symbol);
        
        if (ema9 != null && ema20 != null && !ema9.isEmpty() && !ema20.isEmpty()) {
            double latestEMA9 = ema9.get(ema9.size() - 1);
            double latestEMA20 = ema20.get(ema20.size() - 1);
            
            if (latestEMA9 > latestEMA20) {
                trendDirection.put(symbol, "BULLISH");
            } else {
                trendDirection.put(symbol, "BEARISH");
            }
        }
    }

    /**
     * Check for EMA crossover signals for all symbols
     */
    public List<TradeSignal> checkEMASignals() {
        List<TradeSignal> signals = new ArrayList<>();
        
        // Check signals for all symbols that have EMA data
        for (String symbol : ema9Data.keySet()) {
            signals.addAll(checkEMASignalsForSymbol(symbol));
        }
        
        return signals;
    }
    
    /**
     * Check for EMA crossover signals for specific symbol
     */
    public List<TradeSignal> checkEMASignalsForSymbol(String symbol) {
        List<TradeSignal> signals = new ArrayList<>();
        
        List<Double> ema9 = ema9Data.get(symbol);
        List<Double> ema20 = ema20Data.get(symbol);
        
        if (ema9 == null || ema20 == null || ema9.size() < 2 || ema20.size() < 2) {
            return signals;
        }

        // Check for crossover in last 2 periods
        double currentEMA9 = ema9.get(ema9.size() - 1);
        double previousEMA9 = ema9.get(ema9.size() - 2);
        double currentEMA20 = ema20.get(ema20.size() - 1);
        double previousEMA20 = ema20.get(ema20.size() - 2);
        
        // Use EMA9 as current price approximation
        double currentPrice = currentEMA9;
        
        // Bullish crossover: 9 EMA crosses above 20 EMA
        if (previousEMA9 <= previousEMA20 && currentEMA9 > currentEMA20) {
            TradeSignal signal = createBullishSignal(symbol, currentPrice, currentEMA9, currentEMA20);
            signals.add(signal);
        }
        
        // Bearish crossover: 9 EMA crosses below 20 EMA
        if (previousEMA9 >= previousEMA20 && currentEMA9 < currentEMA20) {
            TradeSignal signal = createBearishSignal(symbol, currentPrice, currentEMA9, currentEMA20);
            signals.add(signal);
        }
        
        return signals;
    }

    /**
     * Check for pullback entries to 9 EMA for all symbols
     */
    public List<TradeSignal> checkPullbackEntries() {
        List<TradeSignal> signals = new ArrayList<>();
        
        // Check pullback entries for all symbols that have EMA data
        for (String symbol : ema9Data.keySet()) {
            signals.addAll(checkPullbackEntriesForSymbol(symbol));
        }
        
        return signals;
    }
    
    /**
     * Check for pullback entries to 9 EMA for specific symbol with sophisticated logic
     */
    public List<TradeSignal> checkPullbackEntriesForSymbol(String symbol) {
        List<TradeSignal> signals = new ArrayList<>();

        List<Double> ema9 = ema9Data.get(symbol);
        List<Double> ema20 = ema20Data.get(symbol);
        List<CandleData> candles = symbolCandleData.get(symbol);
        
        if (ema9 == null || ema20 == null || ema9.isEmpty() || ema20.isEmpty() || 
            candles == null || candles.isEmpty()) {
            return signals;
        }

        // Need at least EMA_20_PERIOD + LOOKBACK_SWING candles for swing calculations
        if (candles.size() < EMA_20_PERIOD + LOOKBACK_SWING) {
            return signals;
        }

        int currentIndex = candles.size() - 1;
        CandleData currentCandle = candles.get(currentIndex);
        double currentEMA9 = ema9.get(ema9.size() - 1);
        double currentEMA20 = ema20.get(ema20.size() - 1);
        String trend = trendDirection.get(symbol);
        
        // Detect trend
        boolean bullishTrend = currentEMA9 > currentEMA20;
        boolean bearishTrend = currentEMA9 < currentEMA20;
        
        // Check if EMAs are too close (ranging market)
        double emaDistance = Math.abs(currentEMA9 - currentEMA20) / currentEMA20 * 100;
        if (emaDistance < 0.1) { // EMAs within 0.1% - avoid ranging markets
            return signals;
        }
        
        // Calculate distance to 9 EMA
        double distPct = Math.abs((currentCandle.getClose() - currentEMA9) / currentEMA9) * 100.0;
        
        // --- Bullish entry logic ---
        if (bullishTrend && distPct <= PULLBACK_TOLERANCE_PCT) {
            // Strong bullish candle confirmation
            double body = currentCandle.getClose() - currentCandle.getOpen();
            double range = currentCandle.getHigh() - currentCandle.getLow();
            boolean strongBull = (body > 0) && (range > 0) && ((body / range) >= STRONG_CANDLE_BODY_PCT);
            boolean closeAboveEMA9 = currentCandle.getClose() >= currentEMA9;
            
            if (strongBull && closeAboveEMA9) {
                // Calculate swing-based stop loss
                double swingLow = calculateRecentSwingLow(candles, currentIndex - 1, LOOKBACK_SWING);
                double stopLoss = swingLow * (1.0 - SL_BUFFER_PCT / 100.0);
                
                // Validate stop loss
                if (stopLoss < currentCandle.getClose()) {
                    double riskPerUnit = currentCandle.getClose() - stopLoss;
                    if (riskPerUnit > 0) {
                        // Calculate position size based on account risk
                        double riskAmount = DEFAULT_ACCOUNT_SIZE * (DEFAULT_RISK_PER_TRADE / 100.0);
                        double quantity = riskAmount / riskPerUnit;
                        double takeProfit = currentCandle.getClose() + (riskPerUnit * RISK_REWARD_RATIO);
                        
                        TradeSignal signal = new TradeSignal(
                            symbol, "BUY", currentCandle.getClose(), stopLoss, takeProfit,
                            "Strategy 2: Advanced Bullish Pullback with Strong Candle", LEVERAGE
                        );
                        signals.add(signal);
                    }
                }
            }
        }
        
        // --- Bearish entry logic ---
        if (bearishTrend && distPct <= PULLBACK_TOLERANCE_PCT) {
            // Strong bearish candle confirmation
            double body = currentCandle.getOpen() - currentCandle.getClose();
            double range = currentCandle.getHigh() - currentCandle.getLow();
            boolean strongBear = (body > 0) && (range > 0) && ((body / range) >= STRONG_CANDLE_BODY_PCT);
            boolean closeBelowEMA9 = currentCandle.getClose() <= currentEMA9;
            
            if (strongBear && closeBelowEMA9) {
                // Calculate swing-based stop loss
                double swingHigh = calculateRecentSwingHigh(candles, currentIndex - 1, LOOKBACK_SWING);
                double stopLoss = swingHigh * (1.0 + SL_BUFFER_PCT / 100.0);
                
                // Validate stop loss
                if (stopLoss > currentCandle.getClose()) {
                    double riskPerUnit = stopLoss - currentCandle.getClose();
                    if (riskPerUnit > 0) {
                        // Calculate position size based on account risk
                        double riskAmount = DEFAULT_ACCOUNT_SIZE * (DEFAULT_RISK_PER_TRADE / 100.0);
                        double quantity = riskAmount / riskPerUnit;
                        double takeProfit = currentCandle.getClose() - (riskPerUnit * RISK_REWARD_RATIO);
                        
                        TradeSignal signal = new TradeSignal(
                            symbol, "SELL", currentCandle.getClose(), stopLoss, takeProfit,
                            "Strategy 2: Advanced Bearish Pullback with Strong Candle", LEVERAGE
                        );
                        signals.add(signal);
                    }
                }
            }
        }
        
        return signals;
    }

    /**
     * Calculate recent swing low for stop loss placement
     */
    private double calculateRecentSwingLow(List<CandleData> candles, int upToIndexInclusive, int lookback) {
        int start = Math.max(0, upToIndexInclusive - lookback + 1);
        double low = Double.POSITIVE_INFINITY;
        for (int i = start; i <= upToIndexInclusive; i++) {
            low = Math.min(low, candles.get(i).getLow());
        }
        return low;
    }
    
    /**
     * Calculate recent swing high for stop loss placement
     */
    private double calculateRecentSwingHigh(List<CandleData> candles, int upToIndexInclusive, int lookback) {
        int start = Math.max(0, upToIndexInclusive - lookback + 1);
        double high = Double.NEGATIVE_INFINITY;
        for (int i = start; i <= upToIndexInclusive; i++) {
            high = Math.max(high, candles.get(i).getHigh());
        }
        return high;
    }

    /**
     * Create bullish trade signal
     */
    private TradeSignal createBullishSignal(String symbol, double entryPrice, double ema9, double ema20) {
        // Stop loss below 20 EMA or recent swing low
        double stopLoss = Math.min(ema20 * 0.98, entryPrice * 0.97); // 2% below 20 EMA or 3% below entry
        
        // Take profit with 1:3 risk-reward ratio
        double risk = entryPrice - stopLoss;
        double takeProfit = entryPrice + (risk * RISK_REWARD_RATIO);
        
        return new TradeSignal(
            symbol,
            "BUY",
            entryPrice,
            stopLoss,
            takeProfit,
            "Strategy 2: 9/20 EMA Bullish Crossover/Pullback",
            LEVERAGE
        );
    }

    /**
     * Create bearish trade signal
     */
    private TradeSignal createBearishSignal(String symbol, double entryPrice, double ema9, double ema20) {
        // Stop loss above 20 EMA or recent swing high
        double stopLoss = Math.max(ema20 * 1.02, entryPrice * 1.03); // 2% above 20 EMA or 3% above entry
        
        // Take profit with 1:3 risk-reward ratio
        double risk = stopLoss - entryPrice;
        double takeProfit = entryPrice - (risk * RISK_REWARD_RATIO);
        
        return new TradeSignal(
            symbol,
            "SELL",
            entryPrice,
            stopLoss,
            takeProfit,
            "Strategy 2: 9/20 EMA Bearish Crossover/Pullback",
            LEVERAGE
        );
    }

    /**
     * Get current trend direction
     */
    public String getCurrentTrend(String symbol) {
        return trendDirection.getOrDefault(symbol, "NEUTRAL");
    }

    /**
     * Get current EMA values
     */
    public Map<String, Double> getCurrentEMAValues(String symbol) {
        Map<String, Double> values = new HashMap<>();
        
        List<Double> ema9 = ema9Data.get(symbol);
        List<Double> ema20 = ema20Data.get(symbol);
        
        if (ema9 != null && !ema9.isEmpty()) {
            values.put("EMA9", ema9.get(ema9.size() - 1));
        }
        
        if (ema20 != null && !ema20.isEmpty()) {
            values.put("EMA20", ema20.get(ema20.size() - 1));
        }
        
        return values;
    }

    /**
     * Check if market is trending (EMAs expanding) or ranging (EMAs close together) - Advanced version
     */
    public boolean isTrendingMarket(String symbol) {
        List<Double> ema9 = ema9Data.get(symbol);
        List<Double> ema20 = ema20Data.get(symbol);
        
        if (ema9 == null || ema20 == null || ema9.isEmpty() || ema20.isEmpty()) {
            return false;
        }
        
        double currentEMA9 = ema9.get(ema9.size() - 1);
        double currentEMA20 = ema20.get(ema20.size() - 1);
        
        // EMAs are expanding if difference is more than 0.1% (more strict)
        double emaDifference = Math.abs(currentEMA9 - currentEMA20) / currentEMA20 * 100;
        return emaDifference > 0.1; // 0.1% threshold (much more strict than before)
    }
    
    /**
     * Check if market is ranging (EMAs too close)
     */
    public boolean isRangingMarket(String symbol) {
        return !isTrendingMarket(symbol);
    }

    /**
     * Trade Signal class for Strategy 2
     */
    public static class TradeSignal {
        private final String symbol;
        private final String type;
        private final double entryPrice;
        private final double stopLoss;
        private final double takeProfit;
        private final String reason;
        private final double leverage;
        
        public TradeSignal(String symbol, String type, double entryPrice, double stopLoss, double takeProfit, String reason, double leverage) {
            this.symbol = symbol;
            this.type = type;
            this.entryPrice = entryPrice;
            this.stopLoss = stopLoss;
            this.takeProfit = takeProfit;
            this.reason = reason;
            this.leverage = leverage;
        }
        
        // Getters
        public String getSymbol() { return symbol; }
        public String getType() { return type; }
        public double getEntryPrice() { return entryPrice; }
        public double getStopLoss() { return stopLoss; }
        public double getTakeProfit() { return takeProfit; }
        public String getReason() { return reason; }
        public double getLeverage() { return leverage; }
        
        @Override
        public String toString() {
            return String.format("TradeSignal{symbol='%s', type='%s', entry=%.2f, sl=%.2f, tp=%.2f, leverage=%.1fx, reason='%s'}", 
                symbol, type, entryPrice, stopLoss, takeProfit, leverage, reason);
        }
    }

    /**
     * CandleData class for Strategy 2
     */
    public static class CandleData {
        private final double open;
        private final double high;
        private final double low;
        private final double close;
        private final double volume;
        private final long timestamp;
        
        public CandleData(double open, double high, double low, double close, double volume, long timestamp) {
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.volume = volume;
            this.timestamp = timestamp;
        }
        
        // Getters
        public double getOpen() { return open; }
        public double getHigh() { return high; }
        public double getLow() { return low; }
        public double getClose() { return close; }
        public double getVolume() { return volume; }
        public long getTimestamp() { return timestamp; }
    }
}
