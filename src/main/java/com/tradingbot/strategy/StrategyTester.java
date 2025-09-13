package com.tradingbot.strategy;

import com.tradingbot.service.DeltaCandlestickService;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import java.util.*;

/**
 * Strategy Tester - Integrates with Delta Exchange API to test the Moving Average Strategy
 */
public class StrategyTester {

    private final MovingAverageStrategy strategy;
    private final DeltaCandlestickService candlestickService;

    public StrategyTester() {
        this.strategy = new MovingAverageStrategy();
        this.candlestickService = new DeltaCandlestickService();
    }

    /**
     * Load historical data for strategy testing
     */
    public void loadHistoricalData(String symbol, int days) {
        System.out.println("Loading " + days + " days of historical data for " + symbol + "...");
        
        try {
            // Get daily candles for the specified period
            Map<String, Object> response = candlestickService.getCandlestickData(symbol, days, "1d");
            
            if (response != null && response.containsKey("result")) {
                List<Map<String, Object>> candles = (List<Map<String, Object>>) response.get("result");
                
                // Convert API data to strategy format
                for (Map<String, Object> candle : candles) {
                    // Handle timestamp
                    Object timeObj = candle.get("time");
                    long timestamp;
                    if (timeObj instanceof Integer) {
                        timestamp = ((Integer) timeObj).longValue();
                    } else {
                        timestamp = System.currentTimeMillis() / 1000;
                    }
                    
                    // Extract OHLCV data
                    double open = Double.parseDouble(candle.get("open").toString());
                    double high = Double.parseDouble(candle.get("high").toString());
                    double low = Double.parseDouble(candle.get("low").toString());
                    double close = Double.parseDouble(candle.get("close").toString());
                    double volume = Double.parseDouble(candle.get("volume").toString());
                    
                    // Add to strategy
                    MovingAverageStrategy.CandleData candleData = 
                        new MovingAverageStrategy.CandleData(timestamp, open, high, low, close, volume);
                    strategy.addCandleData(candleData);
                }
                
                System.out.println("Loaded " + candles.size() + " candles successfully!");
            } else {
                System.out.println("Failed to load historical data");
            }
        } catch (Exception e) {
            System.out.println("Error loading historical data: " + e.getMessage());
        }
    }

    /**
     * Test strategy with recent data
     */
    public void testStrategy(String symbol) {
        System.out.println("=== Testing Moving Average Strategy for " + symbol + " ===");
        
        // Load 250 days of data (more than 200 for MA calculation)
        loadHistoricalData(symbol, 250);
        
        // Identify trendlines
        strategy.identifyTrendlines();
        
        // Print analysis
        strategy.printAnalysis();
        
        // Test with different timeframes
        testMultipleTimeframes(symbol);
    }

    /**
     * Test strategy with multiple timeframes
     */
    public void testMultipleTimeframes(String symbol) {
        System.out.println("\n=== Multi-Timeframe Analysis ===");
        
        String[] timeframes = {"1h", "4h", "1d"};
        
        for (String timeframe : timeframes) {
            System.out.println("\n--- " + timeframe + " Analysis ---");
            
            try {
                // Get recent candles for this timeframe
                int limit = timeframe.equals("1h") ? 200 : (timeframe.equals("4h") ? 50 : 30);
                Map<String, Object> response = candlestickService.getCandlestickData(symbol, limit, timeframe);
                
                if (response != null && response.containsKey("result")) {
                    List<Map<String, Object>> candles = (List<Map<String, Object>>) response.get("result");
                    
                    // Create a temporary strategy instance for this timeframe
                    MovingAverageStrategy tfStrategy = new MovingAverageStrategy();
                    
                    // Add data
                    for (Map<String, Object> candle : candles) {
                        Object timeObj = candle.get("time");
                        long timestamp = timeObj instanceof Integer ? ((Integer) timeObj).longValue() : 
                                       System.currentTimeMillis() / 1000;
                        
                        double open = Double.parseDouble(candle.get("open").toString());
                        double high = Double.parseDouble(candle.get("high").toString());
                        double low = Double.parseDouble(candle.get("low").toString());
                        double close = Double.parseDouble(candle.get("close").toString());
                        double volume = Double.parseDouble(candle.get("volume").toString());
                        
                        tfStrategy.addCandleData(new MovingAverageStrategy.CandleData(
                            timestamp, open, high, low, close, volume));
                    }
                    
                    // Analyze this timeframe
                    MovingAverageStrategy.StrategyAnalysis analysis = tfStrategy.analyzeStrategy();
                    
                    System.out.println("Trend: " + analysis.getTrend());
                    if (analysis.getMovingAverage200() != null) {
                        System.out.println("MA200: $" + String.format("%.2f", analysis.getMovingAverage200()));
                        System.out.println("Distance: " + String.format("%.2f", analysis.getDistanceFromMA()) + "%");
                    }
                    
                    if (!analysis.getSignals().isEmpty()) {
                        System.out.println("Signals: " + analysis.getSignals().size());
                        for (MovingAverageStrategy.TradeSignal signal : analysis.getSignals()) {
                            System.out.println("  " + signal);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Error analyzing " + timeframe + ": " + e.getMessage());
            }
        }
    }

    /**
     * Backtest strategy performance
     */
    public void backtestStrategy(String symbol, int days) {
        System.out.println("\n=== Backtesting Strategy for " + symbol + " ===");
        System.out.println("Period: " + days + " days");
        
        loadHistoricalData(symbol, days);
        
        // Simulate trades
        List<TradeResult> trades = new ArrayList<>();
        List<MovingAverageStrategy.CandleData> candles = new ArrayList<>();
        
        // Get all candles for backtesting
        try {
            Map<String, Object> response = candlestickService.getCandlestickData(symbol, days, "1d");
            if (response != null && response.containsKey("result")) {
                List<Map<String, Object>> apiCandles = (List<Map<String, Object>>) response.get("result");
                
                for (Map<String, Object> candle : apiCandles) {
                    Object timeObj = candle.get("time");
                    long timestamp = timeObj instanceof Integer ? ((Integer) timeObj).longValue() : 
                                   System.currentTimeMillis() / 1000;
                    
                    double open = Double.parseDouble(candle.get("open").toString());
                    double high = Double.parseDouble(candle.get("high").toString());
                    double low = Double.parseDouble(candle.get("low").toString());
                    double close = Double.parseDouble(candle.get("close").toString());
                    double volume = Double.parseDouble(candle.get("volume").toString());
                    
                    candles.add(new MovingAverageStrategy.CandleData(timestamp, open, high, low, close, volume));
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading backtest data: " + e.getMessage());
            return;
        }
        
        // Run backtest
        MovingAverageStrategy backtestStrategy = new MovingAverageStrategy();
        MovingAverageStrategy.TradeSignal currentSignal = null;
        
        for (int i = 0; i < candles.size(); i++) {
            MovingAverageStrategy.CandleData candle = candles.get(i);
            backtestStrategy.addCandleData(candle);
            
            // Only analyze after we have enough data for MA calculation
            if (i >= 200) {
                backtestStrategy.identifyTrendlines();
                List<MovingAverageStrategy.TradeSignal> signals = backtestStrategy.checkBreakoutSignals();
                
                // Check for new signals
                if (!signals.isEmpty() && currentSignal == null) {
                    currentSignal = signals.get(0);
                    System.out.println("Entry Signal at " + new Date(candle.getTimestamp() * 1000) + 
                                     ": " + currentSignal);
                }
                
                // Check for exit conditions
                if (currentSignal != null) {
                    boolean shouldExit = false;
                    double exitPrice = 0;
                    String exitReason = "";
                    
                    // Check stop loss
                    if (currentSignal.getType() == MovingAverageStrategy.SignalType.BUY) {
                        if (candle.getLow() <= currentSignal.getStopLoss()) {
                            shouldExit = true;
                            exitPrice = currentSignal.getStopLoss();
                            exitReason = "Stop Loss";
                        } else if (candle.getHigh() >= currentSignal.getTakeProfit()) {
                            shouldExit = true;
                            exitPrice = currentSignal.getTakeProfit();
                            exitReason = "Take Profit";
                        }
                    } else {
                        if (candle.getHigh() >= currentSignal.getStopLoss()) {
                            shouldExit = true;
                            exitPrice = currentSignal.getStopLoss();
                            exitReason = "Stop Loss";
                        } else if (candle.getLow() <= currentSignal.getTakeProfit()) {
                            shouldExit = true;
                            exitPrice = currentSignal.getTakeProfit();
                            exitReason = "Take Profit";
                        }
                    }
                    
                    if (shouldExit) {
                        double profit = 0;
                        if (currentSignal.getType() == MovingAverageStrategy.SignalType.BUY) {
                            profit = exitPrice - currentSignal.getEntryPrice();
                        } else {
                            profit = currentSignal.getEntryPrice() - exitPrice;
                        }
                        
                        TradeResult result = new TradeResult(
                            currentSignal, exitPrice, exitReason, profit, candle.getTimestamp()
                        );
                        trades.add(result);
                        
                        System.out.println("Exit at " + new Date(candle.getTimestamp() * 1000) + 
                                         ": Price=$" + String.format("%.2f", exitPrice) + 
                                         ", Reason=" + exitReason + 
                                         ", P&L=$" + String.format("%.2f", profit));
                        
                        currentSignal = null;
                    }
                }
            }
        }
        
        // Print backtest results
        printBacktestResults(trades);
    }

    /**
     * Print backtest results
     */
    private void printBacktestResults(List<TradeResult> trades) {
        System.out.println("\n=== Backtest Results ===");
        System.out.println("Total Trades: " + trades.size());
        
        if (trades.isEmpty()) {
            System.out.println("No trades executed during backtest period.");
            return;
        }
        
        double totalProfit = 0;
        int winningTrades = 0;
        double maxWin = 0;
        double maxLoss = 0;
        
        for (TradeResult trade : trades) {
            totalProfit += trade.getProfit();
            if (trade.getProfit() > 0) {
                winningTrades++;
                maxWin = Math.max(maxWin, trade.getProfit());
            } else {
                maxLoss = Math.min(maxLoss, trade.getProfit());
            }
        }
        
        double winRate = (double) winningTrades / trades.size() * 100;
        
        System.out.println("Total Profit: $" + String.format("%.2f", totalProfit));
        System.out.println("Win Rate: " + String.format("%.1f", winRate) + "%");
        System.out.println("Winning Trades: " + winningTrades + "/" + trades.size());
        System.out.println("Max Win: $" + String.format("%.2f", maxWin));
        System.out.println("Max Loss: $" + String.format("%.2f", maxLoss));
        
        if (maxLoss != 0) {
            double profitFactor = Math.abs(totalProfit) / Math.abs(maxLoss);
            System.out.println("Profit Factor: " + String.format("%.2f", profitFactor));
        }
    }

    /**
     * Trade result for backtesting
     */
    public static class TradeResult {
        private final MovingAverageStrategy.TradeSignal signal;
        private final double exitPrice;
        private final String exitReason;
        private final double profit;
        private final long timestamp;

        public TradeResult(MovingAverageStrategy.TradeSignal signal, double exitPrice, 
                          String exitReason, double profit, long timestamp) {
            this.signal = signal;
            this.exitPrice = exitPrice;
            this.exitReason = exitReason;
            this.profit = profit;
            this.timestamp = timestamp;
        }

        public MovingAverageStrategy.TradeSignal getSignal() { return signal; }
        public double getExitPrice() { return exitPrice; }
        public String getExitReason() { return exitReason; }
        public double getProfit() { return profit; }
        public long getTimestamp() { return timestamp; }
    }

    public static void main(String[] args) {
        StrategyTester tester = new StrategyTester();
        
        // Test with BTCUSD
        tester.testStrategy("BTCUSD");
        
        // Run backtest
        tester.backtestStrategy("BTCUSD", 365); // 1 year backtest
    }
}
