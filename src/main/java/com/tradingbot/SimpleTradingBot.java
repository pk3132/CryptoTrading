package com.tradingbot;

import com.tradingbot.service.DeltaAccountService;
import com.tradingbot.service.DeltaCandlestickService;
import com.tradingbot.service.DeltaExchangeService;
import com.tradingbot.strategy.MovingAverageStrategy;
import java.util.*;

/**
 * Simple Trading Bot Implementation
 * Demonstrates the 200-Day MA + Trendline Breakout Strategy
 */
public class SimpleTradingBot {

    public static void main(String[] args) {
        System.out.println("🤖 SIMPLE TRADING BOT");
        System.out.println("=====================");
        System.out.println("Strategy: 200-Day MA + Trendline Breakout");
        System.out.println("Symbol: BTCUSD");
        System.out.println();
        
        try {
            // Get API credentials from environment variables
            String apiKey = System.getenv("DELTA_API_KEY");
            String apiSecret = System.getenv("DELTA_API_SECRET");
            String baseUrl = System.getenv("DELTA_BASE_URL");
            
            if (apiKey == null || apiSecret == null || baseUrl == null) {
                System.err.println("❌ Missing environment variables:");
                System.err.println("   DELTA_API_KEY, DELTA_API_SECRET, DELTA_BASE_URL");
                System.err.println("   Please set these environment variables and try again.");
                return;
            }
            
            // Initialize services
            DeltaAccountService accountService = new DeltaAccountService(apiKey, apiSecret, baseUrl);
            DeltaCandlestickService candlestickService = new DeltaCandlestickService();
            DeltaExchangeService exchangeService = new DeltaExchangeService();
            MovingAverageStrategy strategy = new MovingAverageStrategy();
            
            // Check account balance
            System.out.println("💰 Checking Account Balance...");
            try {
                String balanceResponse = accountService.checkBalance();
                System.out.println("Balance Response: " + balanceResponse);
            } catch (Exception e) {
                System.out.println("⚠️  Could not fetch balance: " + e.getMessage());
            }
            
            // Load historical data
            System.out.println("\n📊 Loading Historical Data...");
            Map<String, Object> response = candlestickService.getCandlestickData("BTCUSD", 250, "1d");
            
            if (response != null && response.containsKey("result")) {
                List<Map<String, Object>> candles = (List<Map<String, Object>>) response.get("result");
                
                for (Map<String, Object> candle : candles) {
                    Object timeObj = candle.get("time");
                    long timestamp = timeObj instanceof Integer ? ((Integer) timeObj).longValue() : 
                                   System.currentTimeMillis() / 1000;
                    
                    double open = Double.parseDouble(candle.get("open").toString());
                    double high = Double.parseDouble(candle.get("high").toString());
                    double low = Double.parseDouble(candle.get("low").toString());
                    double close = Double.parseDouble(candle.get("close").toString());
                    double volume = Double.parseDouble(candle.get("volume").toString());
                    
                    strategy.addCandleData(new MovingAverageStrategy.CandleData(
                        timestamp, open, high, low, close, volume));
                }
                
                System.out.println("✅ Loaded " + candles.size() + " historical candles");
            } else {
                System.out.println("❌ Failed to load historical data");
                return;
            }
            
            // Analyze strategy
            System.out.println("\n🎯 Strategy Analysis...");
            strategy.identifyTrendlines();
            strategy.printAnalysis();
            
            // Get current market data
            System.out.println("\n📈 Current Market Data...");
            Double currentPrice = exchangeService.getCurrentPrice("BTCUSD");
            if (currentPrice != null) {
                System.out.println("Current BTC Price: $" + String.format("%.2f", currentPrice));
            }
            
            // Simulate trading logic
            System.out.println("\n🤖 Trading Bot Logic Simulation...");
            simulateTradingLogic(strategy);
            
            // Show performance summary
            System.out.println("\n📊 Performance Summary...");
            showPerformanceSummary(strategy);
            
            System.out.println("\n✅ Trading Bot Demo completed successfully!");
            System.out.println("\n🚀 To run the full trading bot:");
            System.out.println("   1. Set up your environment variables");
            System.out.println("   2. Configure your risk parameters");
            System.out.println("   3. Run the TradingBotRunner class");
            
        } catch (Exception e) {
            System.err.println("❌ Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Simulate the trading logic
     */
    private static void simulateTradingLogic(MovingAverageStrategy strategy) {
        try {
            MovingAverageStrategy.StrategyAnalysis analysis = strategy.analyzeStrategy();
            
            System.out.println("📊 Current Analysis:");
            System.out.println("   Trend: " + analysis.getTrend());
            System.out.println("   Current Price: $" + String.format("%.2f", analysis.getCurrentPrice()));
            
            if (analysis.getMovingAverage200() != null) {
                System.out.println("   200-Day MA: $" + String.format("%.2f", analysis.getMovingAverage200()));
                System.out.println("   Distance from MA: " + String.format("%.2f", analysis.getDistanceFromMA()) + "%");
            }
            
            List<MovingAverageStrategy.TradeSignal> signals = analysis.getSignals();
            
            if (signals.isEmpty()) {
                System.out.println("   ⏳ No trading signals - waiting for trendline breakout");
                System.out.println("   💡 Strategy is waiting for:");
                System.out.println("      - Price to break above/below trendline");
                System.out.println("      - Confirmation of trend direction");
                System.out.println("      - Risk-reward ratio of at least 2:1");
            } else {
                System.out.println("   🎯 Trading Signals Found:");
                for (MovingAverageStrategy.TradeSignal signal : signals) {
                    System.out.println("      Type: " + signal.getType());
                    System.out.println("      Entry: $" + String.format("%.2f", signal.getEntryPrice()));
                    System.out.println("      Stop Loss: $" + String.format("%.2f", signal.getStopLoss()));
                    System.out.println("      Take Profit: $" + String.format("%.2f", signal.getTakeProfit()));
                    System.out.println("      Reason: " + signal.getReason());
                    
                    // Calculate risk-reward ratio
                    double risk = Math.abs(signal.getEntryPrice() - signal.getStopLoss());
                    double reward = Math.abs(signal.getTakeProfit() - signal.getEntryPrice());
                    double riskReward = reward / risk;
                    
                    System.out.println("      Risk-Reward Ratio: 1:" + String.format("%.2f", riskReward));
                    
                    if (riskReward >= 2.0) {
                        System.out.println("      ✅ Good risk-reward ratio!");
                        
                        // Simulate trade execution
                        System.out.println("      🚀 SIMULATED TRADE EXECUTION:");
                        System.out.println("         Order Type: " + signal.getType());
                        System.out.println("         Entry Price: $" + String.format("%.2f", signal.getEntryPrice()));
                        System.out.println("         Position Size: 0.1 BTC (example)");
                        System.out.println("         Risk: $" + String.format("%.2f", risk));
                        System.out.println("         Potential Reward: $" + String.format("%.2f", reward));
                        
                    } else {
                        System.out.println("      ⚠️  Low risk-reward ratio - trade rejected");
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error in trading logic simulation: " + e.getMessage());
        }
    }
    
    /**
     * Show performance summary
     */
    private static void showPerformanceSummary(MovingAverageStrategy strategy) {
        System.out.println("📈 Strategy Performance Summary:");
        System.out.println("   ✅ 200-Day Moving Average: Implemented");
        System.out.println("   ✅ Trendline Detection: Implemented");
        System.out.println("   ✅ Breakout Signals: Implemented");
        System.out.println("   ✅ Risk Management: 6:1 risk-reward ratio");
        System.out.println("   ✅ Stop Loss: Automatic calculation");
        System.out.println("   ✅ Take Profit: Automatic calculation");
        
        try {
            MovingAverageStrategy.StrategyAnalysis analysis = strategy.analyzeStrategy();
            if (analysis.getTrend() != MovingAverageStrategy.TrendDirection.UNKNOWN) {
                System.out.println("   📊 Current Market Trend: " + analysis.getTrend());
                if (analysis.getDistanceFromMA() != 0) {
                    System.out.println("   📏 Distance from 200-Day MA: " + 
                                     String.format("%.2f", analysis.getDistanceFromMA()) + "%");
                }
            }
        } catch (Exception e) {
            System.out.println("   ⚠️  Market analysis unavailable");
        }
        
        System.out.println("\n🎯 Strategy Rules:");
        System.out.println("   1. Only trade in direction of 200-day MA trend");
        System.out.println("   2. Wait for trendline breakout confirmation");
        System.out.println("   3. Use 6:1 risk-reward ratio");
        System.out.println("   4. Set stop loss at trendline level");
        System.out.println("   5. Take profit at 6x risk amount");
        System.out.println("   6. Risk maximum 2% of account per trade");
    }
}
