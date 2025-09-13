package com.tradingbot;

import com.tradingbot.strategy.EMAStrategy;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Strategy 2 Backtest - 9/20 EMA Crossover Strategy
 * 
 * Tests the performance of Strategy 2 over different time periods:
 * - 1 week backtest
 * - 1 month backtest
 * - 3 months backtest
 * 
 * Analyzes:
 * - Win rate
 * - Total trades
 * - Profit/Loss
 * - Risk-reward ratio
 * - Drawdown
 */
public class Strategy2Backtest {

    private static final String DELTA_API_BASE = "https://api.india.delta.exchange/v2/history/candles";
    private final RestTemplate restTemplate;
    private final EMAStrategy emaStrategy;
    
    // Strategy 2 parameters
    private static final double INITIAL_CAPITAL = 10000.0;
    private static final double LEVERAGE = 25.0;
    private static final double RISK_PER_TRADE = 0.02; // 2% risk per trade
    private static final double RISK_REWARD_RATIO = 3.0; // 1:3 ratio
    
    // Backtest results
    private double currentCapital = INITIAL_CAPITAL;
    private int totalTrades = 0;
    private int winningTrades = 0;
    private int losingTrades = 0;
    private double totalProfit = 0.0;
    private double maxDrawdown = 0.0;
    private double peakCapital = INITIAL_CAPITAL;
    private List<TradeResult> tradeHistory = new ArrayList<>();
    
    public Strategy2Backtest() {
        this.restTemplate = new RestTemplate();
        this.emaStrategy = new EMAStrategy();
    }

    /**
     * Run 1-week backtest for Strategy 2
     */
    public void runOneWeekBacktest() {
        System.out.println("🚀 STRATEGY 2 BACKTEST - 1 WEEK");
        System.out.println("=" + "=".repeat(50));
        
        resetBacktestData();
        
        // Calculate timestamps for 1 week ago
        long endTime = System.currentTimeMillis() / 1000;
        long startTime = endTime - (7 * 24 * 60 * 60); // 7 days ago
        
        runBacktest("BTCUSD", "15m", startTime, endTime, "1 Week");
        printBacktestResults("1 Week");
    }

    /**
     * Run 1-month backtest for Strategy 2
     */
    public void runOneMonthBacktest() {
        System.out.println("\n🚀 STRATEGY 2 BACKTEST - 1 MONTH");
        System.out.println("=" + "=".repeat(50));
        
        resetBacktestData();
        
        // Calculate timestamps for 1 month ago
        long endTime = System.currentTimeMillis() / 1000;
        long startTime = endTime - (30 * 24 * 60 * 60); // 30 days ago
        
        runBacktest("BTCUSD", "15m", startTime, endTime, "1 Month");
        printBacktestResults("1 Month");
    }

    /**
     * Run 3-month backtest for Strategy 2
     */
    public void runThreeMonthBacktest() {
        System.out.println("\n🚀 STRATEGY 2 BACKTEST - 3 MONTHS");
        System.out.println("=" + "=".repeat(50));
        
        resetBacktestData();
        
        // Calculate timestamps for 3 months ago
        long endTime = System.currentTimeMillis() / 1000;
        long startTime = endTime - (90 * 24 * 60 * 60); // 90 days ago
        
        runBacktest("BTCUSD", "15m", startTime, endTime, "3 Months");
        printBacktestResults("3 Months");
    }

    /**
     * Run comprehensive backtest across multiple timeframes
     */
    public void runComprehensiveBacktest() {
        System.out.println("🎯 STRATEGY 2 COMPREHENSIVE BACKTEST");
        System.out.println("📊 9/20 EMA Crossover Strategy Analysis");
        System.out.println("=" + "=".repeat(60));
        
        runOneWeekBacktest();
        runOneMonthBacktest();
        runThreeMonthBacktest();
        
        System.out.println("\n📈 STRATEGY 2 SUMMARY");
        System.out.println("=" + "=".repeat(30));
        System.out.println("✅ Strategy 2: 9/20 EMA Crossover");
        System.out.println("⚡ Risk-Reward: 1:3");
        System.out.println("🔥 Leverage: 25x");
        System.out.println("📊 Timeframe: 15 minutes");
        System.out.println("🎯 Focus: Trending markets");
        System.out.println("\n💡 Best for: High-frequency trading in trending conditions");
    }

    /**
     * Core backtest logic
     */
    private void runBacktest(String symbol, String resolution, long startTime, long endTime, String period) {
        try {
            System.out.println("📊 Fetching historical data for " + period + "...");
            
            // Fetch candlestick data
            List<Map<String, Object>> candles = fetchHistoricalData(symbol, resolution, startTime, endTime);
            
            if (candles == null || candles.size() < 50) {
                System.out.println("❌ Not enough data for " + period + " backtest");
                return;
            }
            
            System.out.println("📈 Analyzing " + candles.size() + " candles...");
            
            // Add data to EMA strategy
            emaStrategy.addCandleData(candles);
            
            // Simulate trading
            simulateTrading(candles, symbol, period);
            
        } catch (Exception e) {
            System.err.println("❌ Error in " + period + " backtest: " + e.getMessage());
        }
    }

    /**
     * Fetch historical candlestick data
     */
    private List<Map<String, Object>> fetchHistoricalData(String symbol, String resolution, long startTime, long endTime) {
        try {
            String url = DELTA_API_BASE + "?symbol=" + symbol + 
                        "&resolution=" + resolution + 
                        "&start=" + startTime + 
                        "&end=" + endTime;
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
                
                if (responseBody.containsKey("result") && responseBody.get("result") instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> candles = (List<Map<String, Object>>) responseBody.get("result");
                    return candles;
                }
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("❌ Error fetching historical data: " + e.getMessage());
            return null;
        }
    }

    /**
     * Simulate trading based on Strategy 2 signals
     */
    private void simulateTrading(List<Map<String, Object>> candles, String symbol, String period) {
        boolean inPosition = false;
        String positionType = null;
        double entryPrice = 0.0;
        double stopLoss = 0.0;
        double takeProfit = 0.0;
        double positionSize = 0.0;
        int entryIndex = 0;
        
        for (int i = 20; i < candles.size() - 1; i++) { // Start after 20 candles for EMA calculation
            
            if (!inPosition) {
                // Check for entry signals
                List<EMAStrategy.TradeSignal> signals = checkSignalsAtCandle(candles, i);
                
                if (!signals.isEmpty()) {
                    EMAStrategy.TradeSignal signal = signals.get(0);
                    
                    // Check if market is trending (avoid ranging markets)
                    if (emaStrategy.isTrendingMarket(symbol)) {
                        // Enter position
                        inPosition = true;
                        positionType = signal.getType();
                        entryPrice = signal.getEntryPrice();
                        stopLoss = signal.getStopLoss();
                        takeProfit = signal.getTakeProfit();
                        entryIndex = i;
                        
                        // Calculate position size based on risk
                        double riskAmount = currentCapital * RISK_PER_TRADE;
                        double riskPerUnit = Math.abs(entryPrice - stopLoss);
                        positionSize = (riskAmount / riskPerUnit) * LEVERAGE;
                        
                        System.out.println("🎯 Entry: " + positionType + " at $" + String.format("%.2f", entryPrice) + 
                                         " (Risk: $" + String.format("%.2f", riskAmount) + ")");
                    }
                }
            } else {
                // Check for exit conditions
                double currentPrice = Double.parseDouble(candles.get(i).get("close").toString());
                boolean shouldExit = false;
                String exitReason = "";
                
                if (positionType.equals("BUY")) {
                    if (currentPrice <= stopLoss) {
                        shouldExit = true;
                        exitReason = "Stop Loss Hit";
                    } else if (currentPrice >= takeProfit) {
                        shouldExit = true;
                        exitReason = "Take Profit Hit";
                    }
                } else { // SELL position
                    if (currentPrice >= stopLoss) {
                        shouldExit = true;
                        exitReason = "Stop Loss Hit";
                    } else if (currentPrice <= takeProfit) {
                        shouldExit = true;
                        exitReason = "Take Profit Hit";
                    }
                }
                
                if (shouldExit) {
                    // Calculate P&L
                    double pnl;
                    if (positionType.equals("BUY")) {
                        pnl = (currentPrice - entryPrice) * positionSize;
                    } else {
                        pnl = (entryPrice - currentPrice) * positionSize;
                    }
                    
                    // Update capital and statistics
                    currentCapital += pnl;
                    totalTrades++;
                    
                    if (pnl > 0) {
                        winningTrades++;
                        System.out.println("✅ Exit: " + exitReason + " - Profit: $" + String.format("%.2f", pnl));
                    } else {
                        losingTrades++;
                        System.out.println("❌ Exit: " + exitReason + " - Loss: $" + String.format("%.2f", Math.abs(pnl)));
                    }
                    
                    // Record trade
                    tradeHistory.add(new TradeResult(
                        totalTrades, positionType, entryPrice, currentPrice, pnl, exitReason, i - entryIndex
                    ));
                    
                    // Update drawdown tracking
                    if (currentCapital > peakCapital) {
                        peakCapital = currentCapital;
                    } else {
                        double drawdown = (peakCapital - currentCapital) / peakCapital;
                        maxDrawdown = Math.max(maxDrawdown, drawdown);
                    }
                    
                    totalProfit += pnl;
                    
                    // Reset position
                    inPosition = false;
                }
            }
        }
        
        // Close any remaining position
        if (inPosition) {
            double finalPrice = Double.parseDouble(candles.get(candles.size() - 1).get("close").toString());
            double pnl;
            
            if (positionType.equals("BUY")) {
                pnl = (finalPrice - entryPrice) * positionSize;
            } else {
                pnl = (entryPrice - finalPrice) * positionSize;
            }
            
            currentCapital += pnl;
            totalTrades++;
            totalProfit += pnl;
            
            if (pnl > 0) {
                winningTrades++;
            } else {
                losingTrades++;
            }
            
            tradeHistory.add(new TradeResult(
                totalTrades, positionType, entryPrice, finalPrice, pnl, "End of Period", candles.size() - 1 - entryIndex
            ));
        }
    }

    /**
     * Check for Strategy 2 signals at specific candle
     */
    private List<EMAStrategy.TradeSignal> checkSignalsAtCandle(List<Map<String, Object>> candles, int index) {
        // Create a temporary EMA strategy with data up to this point
        EMAStrategy tempStrategy = new EMAStrategy();
        List<Map<String, Object>> tempCandles = new ArrayList<>(candles.subList(0, index + 1));
        tempStrategy.addCandleData(tempCandles);
        
        // Check for signals
        List<EMAStrategy.TradeSignal> signals = new ArrayList<>();
        signals.addAll(tempStrategy.checkEMASignals());
        signals.addAll(tempStrategy.checkPullbackEntries());
        
        return signals;
    }

    /**
     * Print backtest results
     */
    private void printBacktestResults(String period) {
        System.out.println("\n📊 STRATEGY 2 RESULTS - " + period.toUpperCase());
        System.out.println("=" + "=".repeat(40));
        
        // Basic statistics
        double winRate = totalTrades > 0 ? (double) winningTrades / totalTrades * 100 : 0;
        double lossRate = totalTrades > 0 ? (double) losingTrades / totalTrades * 100 : 0;
        double returnPercentage = ((currentCapital - INITIAL_CAPITAL) / INITIAL_CAPITAL) * 100;
        
        System.out.println("💰 Initial Capital: $" + String.format("%.2f", INITIAL_CAPITAL));
        System.out.println("💰 Final Capital: $" + String.format("%.2f", currentCapital));
        System.out.println("📈 Total Return: " + String.format("%.2f", returnPercentage) + "%");
        System.out.println("💵 Total P&L: $" + String.format("%.2f", totalProfit));
        System.out.println();
        
        System.out.println("📊 Trade Statistics:");
        System.out.println("🔄 Total Trades: " + totalTrades);
        System.out.println("✅ Winning Trades: " + winningTrades + " (" + String.format("%.1f", winRate) + "%)");
        System.out.println("❌ Losing Trades: " + losingTrades + " (" + String.format("%.1f", lossRate) + "%)");
        System.out.println();
        
        if (totalTrades > 0) {
            // Calculate average win/loss
            double avgWin = winningTrades > 0 ? totalProfit / winningTrades : 0;
            double avgLoss = losingTrades > 0 ? Math.abs(totalProfit - (winningTrades * avgWin)) / losingTrades : 0;
            double profitFactor = avgLoss > 0 ? avgWin / avgLoss : 0;
            
            System.out.println("📈 Performance Metrics:");
            System.out.println("🎯 Average Win: $" + String.format("%.2f", avgWin));
            System.out.println("🎯 Average Loss: $" + String.format("%.2f", avgLoss));
            System.out.println("⚡ Profit Factor: " + String.format("%.2f", profitFactor));
            System.out.println("📉 Max Drawdown: " + String.format("%.2f", maxDrawdown * 100) + "%");
            System.out.println();
            
            // Risk metrics
            System.out.println("🛡️ Risk Management:");
            System.out.println("⚡ Leverage Used: " + LEVERAGE + "x");
            System.out.println("🎯 Risk per Trade: " + (RISK_PER_TRADE * 100) + "%");
            System.out.println("📊 Risk-Reward Ratio: 1:" + RISK_REWARD_RATIO);
        }
        
        System.out.println("\n" + "=".repeat(40));
    }

    /**
     * Reset backtest data for new run
     */
    private void resetBacktestData() {
        currentCapital = INITIAL_CAPITAL;
        totalTrades = 0;
        winningTrades = 0;
        losingTrades = 0;
        totalProfit = 0.0;
        maxDrawdown = 0.0;
        peakCapital = INITIAL_CAPITAL;
        tradeHistory.clear();
    }

    /**
     * Trade result record
     */
    private static class TradeResult {
        final int tradeNumber;
        final String type;
        final double entryPrice;
        final double exitPrice;
        final double pnl;
        final String exitReason;
        final int candlesHeld;
        
        TradeResult(int tradeNumber, String type, double entryPrice, double exitPrice, 
                   double pnl, String exitReason, int candlesHeld) {
            this.tradeNumber = tradeNumber;
            this.type = type;
            this.entryPrice = entryPrice;
            this.exitPrice = exitPrice;
            this.pnl = pnl;
            this.exitReason = exitReason;
            this.candlesHeld = candlesHeld;
        }
    }

    /**
     * Main method to run Strategy 2 backtest
     */
    public static void main(String[] args) {
        Strategy2Backtest backtest = new Strategy2Backtest();
        backtest.runComprehensiveBacktest();
    }
}
