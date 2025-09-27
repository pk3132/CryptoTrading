package com.tradingbot.test;

import com.tradingbot.strategy.EMA200TrendlineStrategy;
import com.tradingbot.service.DeltaApiClient;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * EMA 200 Analysis Test - Shows exactly how EMA 200 is calculated and checked
 */
public class EMA200AnalysisTest {
    
    public static void main(String[] args) {
        System.out.println("🔍 EMA 200 ANALYSIS TEST");
        System.out.println("=" + "=".repeat(60));
        System.out.println("⏰ Check Time: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("💡 Shows exactly how the trading system calculates and checks EMA 200");
        System.out.println();
        
        // Create services
        EMA200TrendlineStrategy strategy = new EMA200TrendlineStrategy();
        DeltaApiClient apiClient = new DeltaApiClient();
        
        // Test BTC
        System.out.println("🟠 BITCOIN (BTCUSD) EMA 200 ANALYSIS");
        System.out.println("=" + "=".repeat(50));
        analyzeEMA200("BTCUSD", strategy, apiClient);
        
        System.out.println("\n" + "=".repeat(60) + "\n");
        
        // Test ETH
        System.out.println("🔵 ETHEREUM (ETHUSD) EMA 200 ANALYSIS");
        System.out.println("=" + "=".repeat(50));
        analyzeEMA200("ETHUSD", strategy, apiClient);
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("✅ EMA 200 analysis completed!");
    }
    
    private static void analyzeEMA200(String symbol, EMA200TrendlineStrategy strategy, DeltaApiClient apiClient) {
        try {
            // Get historical data (same as trading system)
            long now = System.currentTimeMillis() / 1000;
            long start = now - (500 * 1 * 60); // 500 candles, 1 minute each
            
            System.out.println("📊 Fetching historical data...");
            System.out.println("⏰ Time Range: " + (now - start) / 60 + " minutes ago to now");
            System.out.println("📈 Timeframe: 1-minute candles (same as trading system)");
            
            List<Map<String, Object>> candles = apiClient.fetchOhlcv(symbol, "1m", start, now);
            
            if (candles == null || candles.isEmpty()) {
                System.out.println("❌ No data received for " + symbol);
                return;
            }
            
            System.out.println("✅ Retrieved " + candles.size() + " candles");
            
            if (candles.size() < 200) {
                System.out.println("❌ Not enough data for EMA 200 calculation (need at least 200 candles)");
                return;
            }
            
            // Add data to strategy (same as trading system)
            strategy.addCandleData(symbol, candles);
            
            // Get EMA 200 (same as trading system)
            Double ema200 = strategy.getLastEma200(symbol);
            
            if (ema200 == null) {
                System.out.println("❌ Could not calculate EMA 200 for " + symbol);
                return;
            }
            
            // Get current mark price (same as trading system)
            Double currentMark = apiClient.getCurrentMarkPrice(symbol);
            
            if (currentMark == null) {
                System.out.println("❌ Could not get current mark price for " + symbol);
                return;
            }
            
            System.out.println("\n📊 EMA 200 CALCULATION DETAILS:");
            System.out.println("-" + "-".repeat(40));
            System.out.println("📅 Data Points: " + candles.size() + " candles");
            System.out.println("⏰ Timeframe: 1-minute candles");
            System.out.println("🔄 EMA Period: 200");
            System.out.println("📊 EMA 200 Value: $" + String.format("%.2f", ema200));
            
            System.out.println("\n💰 PRICE COMPARISON:");
            System.out.println("-" + "-".repeat(40));
            System.out.println("📈 Current Mark Price: $" + String.format("%.2f", currentMark));
            System.out.println("📊 EMA 200: $" + String.format("%.2f", ema200));
            
            double difference = currentMark - ema200;
            double differencePercent = (difference / ema200) * 100;
            
            System.out.println("📏 Difference: $" + String.format("%.2f", difference));
            System.out.println("📊 Difference %: " + String.format("%.2f", differencePercent) + "%");
            
            System.out.println("\n🎯 TRADING SYSTEM LOGIC:");
            System.out.println("-" + "-".repeat(40));
            
            if (currentMark > ema200) {
                System.out.println("🟢 " + symbol + " is ABOVE EMA 200");
                System.out.println("📈 Trading Logic: BUY signals possible");
                System.out.println("💡 Condition: currentPrice > emaValue ✅");
            } else if (currentMark < ema200) {
                System.out.println("🔴 " + symbol + " is BELOW EMA 200");
                System.out.println("📉 Trading Logic: SELL signals possible");
                System.out.println("💡 Condition: currentPrice < emaValue ✅");
            } else {
                System.out.println("🟡 " + symbol + " is AT EMA 200");
                System.out.println("📊 Trading Logic: NEUTRAL");
            }
            
            System.out.println("\n🔧 EMA 200 CALCULATION METHOD:");
            System.out.println("-" + "-".repeat(40));
            System.out.println("1️⃣ Initial SMA: Average of first 200 candle closes");
            System.out.println("2️⃣ Multiplier: 2.0 / (200 + 1) = " + String.format("%.6f", 2.0 / 201));
            System.out.println("3️⃣ EMA Formula: (Current Close - Previous EMA) × Multiplier + Previous EMA");
            System.out.println("4️⃣ Data Source: 1-minute candle close prices");
            System.out.println("5️⃣ Update Frequency: Every 1 minute (new candle)");
            
            System.out.println("\n📋 TRADING SYSTEM USAGE:");
            System.out.println("-" + "-".repeat(40));
            System.out.println("🔍 Signal Generation: Uses EMA 200 as filter");
            System.out.println("📊 Price Comparison: Current mark price vs EMA 200");
            System.out.println("🎯 BUY Signal: currentPrice > ema200 AND resistance breakout");
            System.out.println("🎯 SELL Signal: currentPrice < ema200 AND support breakout");
            
        } catch (Exception e) {
            System.err.println("❌ Error analyzing EMA 200 for " + symbol + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
