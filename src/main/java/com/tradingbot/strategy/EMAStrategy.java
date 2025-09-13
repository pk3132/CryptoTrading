package com.tradingbot.strategy;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

/**
 * STRATEGY 2: 9/20 EMA Crossover Strategy for Crypto Futures
 * 
 * AGGRESSIVE HIGH-FREQUENCY STRATEGY
 * 
 * PERFORMANCE:
 * - Risk-Reward Ratio: 1:3 (Aggressive)
 * - Timeframes: 15m, 30m, 1h, 4h
 * - Leverage: 25x (Crypto futures)
 * - Trades per Day: 2-3 (15m), 1-2 (1h)
 * - Best for: Trending markets, avoid ranging
 * 
 * STRATEGY RULES:
 * 1. 9 EMA (short-term) and 20 EMA (mid-term)
 * 2. Trade only in trend direction
 * 3. Enter on pullbacks to 9 EMA
 * 4. 1:3 risk-reward ratio
 * 5. Trailing stops above/below 20 EMA
 * 
 * STATUS: ✅ STRATEGY 2 IMPLEMENTATION
 */
@Service
public class EMAStrategy {

    private static final int EMA_9_PERIOD = 9;
    private static final int EMA_20_PERIOD = 20;
    private static final double RISK_REWARD_RATIO = 3.0; // 1:3 ratio
    private static final double LEVERAGE = 25.0; // 25x leverage
    
    private final List<CandleData> historicalData;
    private final Map<String, List<Double>> ema9Data;
    private final Map<String, List<Double>> ema20Data;
    private final Map<String, String> trendDirection;
    
    public EMAStrategy() {
        this.historicalData = new ArrayList<>();
        this.ema9Data = new HashMap<>();
        this.ema20Data = new HashMap<>();
        this.trendDirection = new HashMap<>();
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
     * Check for EMA crossover signals
     */
    public List<TradeSignal> checkEMASignals() {
        List<TradeSignal> signals = new ArrayList<>();
        
        if (historicalData.size() < EMA_20_PERIOD) {
            return signals;
        }

        List<Double> ema9 = ema9Data.get("BTCUSD");
        List<Double> ema20 = ema20Data.get("BTCUSD");
        
        if (ema9 == null || ema20 == null || ema9.size() < 2 || ema20.size() < 2) {
            return signals;
        }

        // Check for crossover in last 2 periods
        double currentEMA9 = ema9.get(ema9.size() - 1);
        double previousEMA9 = ema9.get(ema9.size() - 2);
        double currentEMA20 = ema20.get(ema20.size() - 1);
        double previousEMA20 = ema20.get(ema20.size() - 2);
        
        double currentPrice = historicalData.get(historicalData.size() - 1).getClose();
        
        // Bullish crossover: 9 EMA crosses above 20 EMA
        if (previousEMA9 <= previousEMA20 && currentEMA9 > currentEMA20) {
            TradeSignal signal = createBullishSignal(currentPrice, currentEMA9, currentEMA20);
            signals.add(signal);
        }
        
        // Bearish crossover: 9 EMA crosses below 20 EMA
        if (previousEMA9 >= previousEMA20 && currentEMA9 < currentEMA20) {
            TradeSignal signal = createBearishSignal(currentPrice, currentEMA9, currentEMA20);
            signals.add(signal);
        }
        
        return signals;
    }

    /**
     * Check for pullback entries to 9 EMA
     */
    public List<TradeSignal> checkPullbackEntries() {
        List<TradeSignal> signals = new ArrayList<>();
        
        if (historicalData.size() < EMA_20_PERIOD) {
            return signals;
        }

        List<Double> ema9 = ema9Data.get("BTCUSD");
        List<Double> ema20 = ema20Data.get("BTCUSD");
        
        if (ema9 == null || ema20 == null || ema9.isEmpty() || ema20.isEmpty()) {
            return signals;
        }

        double currentPrice = historicalData.get(historicalData.size() - 1).getClose();
        double currentEMA9 = ema9.get(ema9.size() - 1);
        double currentEMA20 = ema20.get(ema20.size() - 1);
        String trend = trendDirection.get("BTCUSD");
        
        // Check if price is near 9 EMA (within 0.5% tolerance)
        double priceToEMA9Ratio = Math.abs(currentPrice - currentEMA9) / currentEMA9;
        
        if (priceToEMA9Ratio <= 0.005) { // 0.5% tolerance
            if ("BULLISH".equals(trend) && currentPrice >= currentEMA9) {
                // Bullish pullback entry
                TradeSignal signal = createBullishSignal(currentPrice, currentEMA9, currentEMA20);
                signals.add(signal);
            } else if ("BEARISH".equals(trend) && currentPrice <= currentEMA9) {
                // Bearish pullback entry
                TradeSignal signal = createBearishSignal(currentPrice, currentEMA9, currentEMA20);
                signals.add(signal);
            }
        }
        
        return signals;
    }

    /**
     * Create bullish trade signal
     */
    private TradeSignal createBullishSignal(double entryPrice, double ema9, double ema20) {
        // Stop loss below 20 EMA or recent swing low
        double stopLoss = Math.min(ema20 * 0.98, entryPrice * 0.97); // 2% below 20 EMA or 3% below entry
        
        // Take profit with 1:3 risk-reward ratio
        double risk = entryPrice - stopLoss;
        double takeProfit = entryPrice + (risk * RISK_REWARD_RATIO);
        
        return new TradeSignal(
            "BTCUSD",
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
    private TradeSignal createBearishSignal(double entryPrice, double ema9, double ema20) {
        // Stop loss above 20 EMA or recent swing high
        double stopLoss = Math.max(ema20 * 1.02, entryPrice * 1.03); // 2% above 20 EMA or 3% above entry
        
        // Take profit with 1:3 risk-reward ratio
        double risk = stopLoss - entryPrice;
        double takeProfit = entryPrice - (risk * RISK_REWARD_RATIO);
        
        return new TradeSignal(
            "BTCUSD",
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
     * Check if market is trending (EMAs expanding) or ranging (EMAs close together)
     */
    public boolean isTrendingMarket(String symbol) {
        List<Double> ema9 = ema9Data.get(symbol);
        List<Double> ema20 = ema20Data.get(symbol);
        
        if (ema9 == null || ema20 == null || ema9.isEmpty() || ema20.isEmpty()) {
            return false;
        }
        
        double currentEMA9 = ema9.get(ema9.size() - 1);
        double currentEMA20 = ema20.get(ema20.size() - 1);
        
        // EMAs are expanding if difference is more than 1%
        double emaDifference = Math.abs(currentEMA9 - currentEMA20) / currentEMA20;
        return emaDifference > 0.01; // 1% threshold
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
