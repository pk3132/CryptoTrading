package com.tradingbot.test;

import com.tradingbot.service.DeltaApiClient;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Bitcoin EMA 200 Check
 * Check if Bitcoin is currently above or below EMA 200
 */
public class BitcoinEmaCheck {
    
    public static void main(String[] args) {
        System.out.println("🔍 BITCOIN EMA 200 CHECK");
        System.out.println("=" + "=".repeat(50));
        
        try {
            // Create API client
            DeltaApiClient apiClient = new DeltaApiClient();
            
            // Get current time and 500 candles back (for EMA 200 calculation)
            long now = System.currentTimeMillis() / 1000;
            long start = now - (500 * 15 * 60); // 15 minutes per candle
            
            System.out.println("📊 Fetching Bitcoin data...");
            System.out.println("⏰ Time Range: " + (now - start) / 60 + " minutes");
            
            // Fetch Bitcoin data
            List<Map<String, Object>> candles = apiClient.fetchOhlcv("BTCUSD", "15m", start, now);
            
            if (candles == null || candles.isEmpty()) {
                System.out.println("❌ No data received for BTCUSD");
                return;
            }
            
            System.out.println("✅ Retrieved " + candles.size() + " candles");
            
            // Extract close prices
            List<Double> closes = new ArrayList<>();
            for (Map<String, Object> candle : candles) {
                Object closeObj = candle.get("close");
                if (closeObj instanceof Number) {
                    closes.add(((Number) closeObj).doubleValue());
                }
            }
            
            if (closes.size() < 200) {
                System.out.println("❌ Not enough data for EMA 200 calculation");
                return;
            }
            
            // Calculate EMA 200
            List<Double> ema200 = calculateEMA(closes, 200);
            
            // Get current price and EMA 200
            double currentPrice = closes.get(closes.size() - 1);
            double currentEma200 = ema200.get(ema200.size() - 1);
            
            // Calculate difference
            double difference = currentPrice - currentEma200;
            double differencePercent = (difference / currentEma200) * 100;
            
            System.out.println("\n📈 BITCOIN ANALYSIS");
            System.out.println("=" + "=".repeat(30));
            System.out.println("💰 Current Price: $" + String.format("%.2f", currentPrice));
            System.out.println("📊 EMA 200: $" + String.format("%.2f", currentEma200));
            System.out.println("📏 Difference: $" + String.format("%.2f", difference));
            System.out.println("📊 Difference %: " + String.format("%.2f", differencePercent) + "%");
            
            System.out.println("\n🎯 TREND ANALYSIS");
            System.out.println("=" + "=".repeat(30));
            
            if (currentPrice > currentEma200) {
                System.out.println("🟢 BITCOIN IS ABOVE EMA 200");
                System.out.println("📈 Trend: BULLISH");
                System.out.println("💡 Signal: BUY opportunities possible");
            } else {
                System.out.println("🔴 BITCOIN IS BELOW EMA 200");
                System.out.println("📉 Trend: BEARISH");
                System.out.println("💡 Signal: SELL opportunities possible");
            }
            
            // Show recent price action
            System.out.println("\n📊 RECENT PRICE ACTION");
            System.out.println("=" + "=".repeat(30));
            int recentCandles = Math.min(5, closes.size());
            for (int i = closes.size() - recentCandles; i < closes.size(); i++) {
                double price = closes.get(i);
                double ema = ema200.get(i);
                String status = price > ema ? "🟢 ABOVE" : "🔴 BELOW";
                System.out.println("Candle " + (i + 1) + ": $" + String.format("%.2f", price) + " " + status + " EMA 200");
            }
            
            System.out.println("\n✅ Analysis completed!");
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Calculate EMA (Exponential Moving Average)
     */
    private static List<Double> calculateEMA(List<Double> prices, int period) {
        List<Double> ema = new ArrayList<>();
        double multiplier = 2.0 / (period + 1);
        
        // Start with first price
        ema.add(prices.get(0));
        
        // Calculate EMA for remaining prices
        for (int i = 1; i < prices.size(); i++) {
            double currentEMA = (prices.get(i) - ema.get(i - 1)) * multiplier + ema.get(i - 1);
            ema.add(currentEMA);
        }
        
        return ema;
    }
}
