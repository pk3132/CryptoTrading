package com.tradingbot.strategy;

import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Aggressive Chart Technical Strategy
 * Generates more signals by being less selective
 */
@Service
public class AggressiveChartStrategy {

    private static final int BOLLINGER_PERIOD = 10;  // Reduced from 20
    private static final double BOLLINGER_STD = 1.5;  // Reduced from 2.0
    private static final int TRENDLINE_LOOKBACK = 5;  // Reduced from 10
    private static final double RISK_REWARD_RATIO = 2.0;
    private static final double SL_BUFFER_PCT = 0.05; // Reduced from 0.1

    private final Map<String, List<Double>> closesData;
    private final Map<String, List<Double>> highsData;
    private final Map<String, List<Double>> lowsData;
    private final Map<String, List<Double>> opensData;

    public AggressiveChartStrategy() {
        this.closesData = new HashMap<>();
        this.highsData = new HashMap<>();
        this.lowsData = new HashMap<>();
        this.opensData = new HashMap<>();
    }

    public void addCandleData(String symbol, List<Map<String, Object>> candles) {
        List<Double> closes = new ArrayList<>();
        List<Double> highs = new ArrayList<>();
        List<Double> lows = new ArrayList<>();
        List<Double> opens = new ArrayList<>();

        for (Map<String, Object> c : candles) {
            closes.add(((Number) c.get("close")).doubleValue());
            highs.add(((Number) c.get("high")).doubleValue());
            lows.add(((Number) c.get("low")).doubleValue());
            opens.add(((Number) c.get("open")).doubleValue());
        }
        closesData.put(symbol, closes);
        highsData.put(symbol, highs);
        lowsData.put(symbol, lows);
        opensData.put(symbol, opens);
    }

    public List<TradeSignal> checkSignals(String symbol) {
        List<TradeSignal> signals = new ArrayList<>();

        List<Double> closes = closesData.get(symbol);
        List<Double> highs = highsData.get(symbol);
        List<Double> lows = lowsData.get(symbol);
        List<Double> opens = opensData.get(symbol);

        if (closes == null || closes.size() < BOLLINGER_PERIOD) {
            return signals;
        }

        // More aggressive signal generation
        signals.addAll(checkPriceMovementSignals(symbol, closes));
        signals.addAll(checkBollingerBandSignals(symbol, closes));
        signals.addAll(checkSupportResistanceSignals(symbol, closes, highs, lows));
        signals.addAll(checkCandlestickPatternSignals(symbol, opens, highs, lows, closes));
        signals.addAll(checkTrendSignals(symbol, closes));

        return signals;
    }

    /**
     * Check for simple price movement signals
     */
    private List<TradeSignal> checkPriceMovementSignals(String symbol, List<Double> closes) {
        List<TradeSignal> signals = new ArrayList<>();
        
        if (closes.size() < 3) return signals;
        
        double currentPrice = closes.get(closes.size() - 1);
        double prevPrice = closes.get(closes.size() - 2);
        double prev2Price = closes.get(closes.size() - 3);
        
        // BUY signal: Price going up for 2 consecutive candles
        if (currentPrice > prevPrice && prevPrice > prev2Price) {
            double entryPrice = currentPrice;
            double stopLoss = entryPrice * 0.995; // 0.5% SL
            double takeProfit = entryPrice + (Math.abs(entryPrice - stopLoss) * RISK_REWARD_RATIO);
            signals.add(new TradeSignal(symbol, "BUY", entryPrice, stopLoss, takeProfit, "Price Uptrend"));
        }
        
        // SELL signal: Price going down for 2 consecutive candles
        if (currentPrice < prevPrice && prevPrice < prev2Price) {
            double entryPrice = currentPrice;
            double stopLoss = entryPrice * 1.005; // 0.5% SL
            double takeProfit = entryPrice - (Math.abs(entryPrice - stopLoss) * RISK_REWARD_RATIO);
            signals.add(new TradeSignal(symbol, "SELL", entryPrice, stopLoss, takeProfit, "Price Downtrend"));
        }
        
        return signals;
    }

    /**
     * Check for Bollinger Band signals (more aggressive)
     */
    private List<TradeSignal> checkBollingerBandSignals(String symbol, List<Double> closes) {
        List<TradeSignal> signals = new ArrayList<>();
        
        if (closes.size() < BOLLINGER_PERIOD) return signals;
        
        BollingerBands bb = calculateBollingerBands(closes);
        double currentPrice = closes.get(closes.size() - 1);
        
        // BUY signal: Price near lower band
        if (currentPrice <= bb.lowerBand.get(bb.lowerBand.size() - 1) * 1.01) {
            double entryPrice = currentPrice;
            double stopLoss = bb.lowerBand.get(bb.lowerBand.size() - 1) * 0.999;
            double takeProfit = entryPrice + (Math.abs(entryPrice - stopLoss) * RISK_REWARD_RATIO);
            signals.add(new TradeSignal(symbol, "BUY", entryPrice, stopLoss, takeProfit, "Bollinger Lower Band"));
        }
        
        // SELL signal: Price near upper band
        if (currentPrice >= bb.upperBand.get(bb.upperBand.size() - 1) * 0.99) {
            double entryPrice = currentPrice;
            double stopLoss = bb.upperBand.get(bb.upperBand.size() - 1) * 1.001;
            double takeProfit = entryPrice - (Math.abs(entryPrice - stopLoss) * RISK_REWARD_RATIO);
            signals.add(new TradeSignal(symbol, "SELL", entryPrice, stopLoss, takeProfit, "Bollinger Upper Band"));
        }
        
        return signals;
    }

    /**
     * Check for support/resistance signals (more aggressive)
     */
    private List<TradeSignal> checkSupportResistanceSignals(String symbol, List<Double> closes, List<Double> highs, List<Double> lows) {
        List<TradeSignal> signals = new ArrayList<>();
        
        if (closes.size() < 10) return signals;
        
        double currentPrice = closes.get(closes.size() - 1);
        
        // Find recent support and resistance
        double resistance = findResistanceLevel(highs);
        double support = findSupportLevel(lows);
        
        // BUY signal: Price near support
        if (currentPrice <= support * 1.02 && currentPrice >= support * 0.98) {
            double entryPrice = currentPrice;
            double stopLoss = support * 0.995;
            double takeProfit = entryPrice + (Math.abs(entryPrice - stopLoss) * RISK_REWARD_RATIO);
            signals.add(new TradeSignal(symbol, "BUY", entryPrice, stopLoss, takeProfit, "Support Level"));
        }
        
        // SELL signal: Price near resistance
        if (currentPrice >= resistance * 0.98 && currentPrice <= resistance * 1.02) {
            double entryPrice = currentPrice;
            double stopLoss = resistance * 1.005;
            double takeProfit = entryPrice - (Math.abs(entryPrice - stopLoss) * RISK_REWARD_RATIO);
            signals.add(new TradeSignal(symbol, "SELL", entryPrice, stopLoss, takeProfit, "Resistance Level"));
        }
        
        return signals;
    }

    /**
     * Check for candlestick pattern signals (more aggressive)
     */
    private List<TradeSignal> checkCandlestickPatternSignals(String symbol, List<Double> opens, List<Double> highs, 
                                                           List<Double> lows, List<Double> closes) {
        List<TradeSignal> signals = new ArrayList<>();
        
        if (closes.size() < 2) return signals;
        
        double currentClose = closes.get(closes.size() - 1);
        double currentOpen = opens.get(opens.size() - 1);
        double prevClose = closes.get(closes.size() - 2);
        double prevOpen = opens.get(opens.size() - 2);
        
        // BUY signal: Green candle after red candle
        if (currentClose > currentOpen && prevClose < prevOpen) {
            double entryPrice = currentClose;
            double stopLoss = entryPrice * 0.995;
            double takeProfit = entryPrice + (Math.abs(entryPrice - stopLoss) * RISK_REWARD_RATIO);
            signals.add(new TradeSignal(symbol, "BUY", entryPrice, stopLoss, takeProfit, "Green After Red"));
        }
        
        // SELL signal: Red candle after green candle
        if (currentClose < currentOpen && prevClose > prevOpen) {
            double entryPrice = currentClose;
            double stopLoss = entryPrice * 1.005;
            double takeProfit = entryPrice - (Math.abs(entryPrice - stopLoss) * RISK_REWARD_RATIO);
            signals.add(new TradeSignal(symbol, "SELL", entryPrice, stopLoss, takeProfit, "Red After Green"));
        }
        
        return signals;
    }

    /**
     * Check for trend signals
     */
    private List<TradeSignal> checkTrendSignals(String symbol, List<Double> closes) {
        List<TradeSignal> signals = new ArrayList<>();
        
        if (closes.size() < 5) return signals;
        
        double currentPrice = closes.get(closes.size() - 1);
        double sma5 = calculateSMA(closes, 5).get(0);
        
        // BUY signal: Price above SMA5
        if (currentPrice > sma5) {
            double entryPrice = currentPrice;
            double stopLoss = sma5 * 0.999;
            double takeProfit = entryPrice + (Math.abs(entryPrice - stopLoss) * RISK_REWARD_RATIO);
            signals.add(new TradeSignal(symbol, "BUY", entryPrice, stopLoss, takeProfit, "Price Above SMA5"));
        }
        
        // SELL signal: Price below SMA5
        if (currentPrice < sma5) {
            double entryPrice = currentPrice;
            double stopLoss = sma5 * 1.001;
            double takeProfit = entryPrice - (Math.abs(entryPrice - stopLoss) * RISK_REWARD_RATIO);
            signals.add(new TradeSignal(symbol, "SELL", entryPrice, stopLoss, takeProfit, "Price Below SMA5"));
        }
        
        return signals;
    }

    /**
     * Calculate Bollinger Bands
     */
    private BollingerBands calculateBollingerBands(List<Double> closes) {
        List<Double> sma = calculateSMA(closes, BOLLINGER_PERIOD);
        List<Double> upperBand = new ArrayList<>();
        List<Double> lowerBand = new ArrayList<>();
        
        for (int i = BOLLINGER_PERIOD - 1; i < closes.size(); i++) {
            double mean = sma.get(i - BOLLINGER_PERIOD + 1);
            double sum = 0;
            
            for (int j = i - BOLLINGER_PERIOD + 1; j <= i; j++) {
                sum += Math.pow(closes.get(j) - mean, 2);
            }
            
            double std = Math.sqrt(sum / BOLLINGER_PERIOD);
            upperBand.add(mean + (BOLLINGER_STD * std));
            lowerBand.add(mean - (BOLLINGER_STD * std));
        }
        
        return new BollingerBands(sma, upperBand, lowerBand);
    }

    /**
     * Calculate Simple Moving Average
     */
    private List<Double> calculateSMA(List<Double> prices, int period) {
        List<Double> sma = new ArrayList<>();
        
        for (int i = period - 1; i < prices.size(); i++) {
            double sum = 0;
            for (int j = i - period + 1; j <= i; j++) {
                sum += prices.get(j);
            }
            sma.add(sum / period);
        }
        
        return sma;
    }

    /**
     * Find support level
     */
    private double findSupportLevel(List<Double> lows) {
        if (lows.size() < 5) return 0;
        List<Double> recentLows = lows.subList(Math.max(0, lows.size() - 10), lows.size());
        return recentLows.stream().mapToDouble(Double::doubleValue).min().orElse(0);
    }

    /**
     * Find resistance level
     */
    private double findResistanceLevel(List<Double> highs) {
        if (highs.size() < 5) return 0;
        List<Double> recentHighs = highs.subList(Math.max(0, highs.size() - 10), highs.size());
        return recentHighs.stream().mapToDouble(Double::doubleValue).max().orElse(0);
    }

    /**
     * Bollinger Bands data structure
     */
    private static class BollingerBands {
        public final List<Double> sma;
        public final List<Double> upperBand;
        public final List<Double> lowerBand;

        public BollingerBands(List<Double> sma, List<Double> upperBand, List<Double> lowerBand) {
            this.sma = sma;
            this.upperBand = upperBand;
            this.lowerBand = lowerBand;
        }
    }

    public static class TradeSignal {
        private final String symbol;
        private final String type;
        private final double entryPrice;
        private final double stopLoss;
        private final double takeProfit;
        private final String reason;

        public TradeSignal(String symbol, String type, double entryPrice, double stopLoss, double takeProfit, String reason) {
            this.symbol = symbol;
            this.type = type;
            this.entryPrice = entryPrice;
            this.stopLoss = stopLoss;
            this.takeProfit = takeProfit;
            this.reason = reason;
        }

        public String getSymbol() { return symbol; }
        public String getType() { return type; }
        public double getEntryPrice() { return entryPrice; }
        public double getStopLoss() { return stopLoss; }
        public double getTakeProfit() { return takeProfit; }
        public String getReason() { return reason; }

        @Override
        public String toString() {
            return String.format("TradeSignal{symbol='%s', type='%s', entry=%.2f, sl=%.2f, tp=%.2f, reason='%s'}",
                symbol, type, entryPrice, stopLoss, takeProfit, reason);
        }
    }
}
