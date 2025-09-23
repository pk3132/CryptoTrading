package com.tradingbot.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Correct Bitcoin EMA 200 Check
 * Check if Bitcoin is currently above or below EMA 200
 * Using proper EMA calculation
 */
public class CorrectBitcoinEmaCheck {
    
    private static final String DELTA_API_URL = "https://api.delta.exchange/v2";
    private static final ObjectMapper mapper = new ObjectMapper();
    
    public static void main(String[] args) {
        System.out.println("🔍 CORRECT BITCOIN EMA 200 CHECK");
        System.out.println("=" + "=".repeat(50));
        
        try {
            // Get current time and more candles for accurate EMA 200
            long now = System.currentTimeMillis() / 1000;
            long start = now - (300 * 15 * 60); // 300 candles = 75 hours
            
            System.out.println("📊 Fetching Bitcoin data...");
            System.out.println("⏰ Time Range: " + (now - start) / 60 + " minutes");
            
            // Fetch Bitcoin data
            List<Double> closes = fetchBitcoinData("BTCUSD", start, now);
            
            if (closes == null || closes.isEmpty()) {
                System.out.println("❌ No data received for BTCUSD");
                return;
            }
            
            System.out.println("✅ Retrieved " + closes.size() + " candles");
            
            if (closes.size() < 200) {
                System.out.println("❌ Not enough data for EMA 200 calculation");
                return;
            }
            
            // Calculate EMA 200 using proper method
            List<Double> ema200 = calculateEMA200(closes);
            
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
            int recentCandles = Math.min(10, closes.size());
            int emaStartIndex = Math.max(0, closes.size() - ema200.size());
            for (int i = closes.size() - recentCandles; i < closes.size(); i++) {
                double price = closes.get(i);
                int emaIndex = i - emaStartIndex;
                if (emaIndex >= 0 && emaIndex < ema200.size()) {
                    double ema = ema200.get(emaIndex);
                    String status = price > ema ? "🟢 ABOVE" : "🔴 BELOW";
                    System.out.println("Candle " + (i + 1) + ": $" + String.format("%.2f", price) + " " + status + " EMA 200");
                }
            }
            
            // Show EMA 200 values for last few candles
            System.out.println("\n📊 EMA 200 VALUES");
            System.out.println("=" + "=".repeat(30));
            for (int i = Math.max(0, ema200.size() - 5); i < ema200.size(); i++) {
                double ema = ema200.get(i);
                System.out.println("EMA " + (i + 1) + ": $" + String.format("%.2f", ema));
            }
            
            System.out.println("\n✅ Analysis completed!");
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Fetch Bitcoin data from Delta Exchange API
     */
    private static List<Double> fetchBitcoinData(String symbol, long start, long end) {
        try {
            String url = DELTA_API_URL + "/history/candles?resolution=15m&symbol=" + symbol + 
                        "&start=" + start + "&end=" + end;
            
            URL apiUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                System.err.println("❌ API Error: " + responseCode);
                return null;
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            // Parse JSON response
            JsonNode rootNode = mapper.readTree(response.toString());
            JsonNode resultNode = rootNode.get("result");
            
            if (resultNode == null || !resultNode.isArray()) {
                System.err.println("❌ Invalid response format");
                return null;
            }
            
            List<Double> closes = new ArrayList<>();
            for (JsonNode candle : resultNode) {
                JsonNode closeNode = candle.get("close");
                if (closeNode != null && closeNode.isNumber()) {
                    closes.add(closeNode.asDouble());
                }
            }
            
            return closes;
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching data: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Calculate EMA 200 using proper method
     */
    private static List<Double> calculateEMA200(List<Double> prices) {
        List<Double> ema = new ArrayList<>();
        int period = 200;
        double multiplier = 2.0 / (period + 1);
        
        // Start with SMA for first 200 values
        if (prices.size() >= period) {
            double sum = 0;
            for (int i = 0; i < period; i++) {
                sum += prices.get(i);
            }
            double sma = sum / period;
            ema.add(sma);
            
            // Calculate EMA for remaining values
            for (int i = period; i < prices.size(); i++) {
                double currentEMA = (prices.get(i) - ema.get(ema.size() - 1)) * multiplier + ema.get(ema.size() - 1);
                ema.add(currentEMA);
            }
        } else {
            // If not enough data, use simple EMA
            ema.add(prices.get(0));
            for (int i = 1; i < prices.size(); i++) {
                double currentEMA = (prices.get(i) - ema.get(ema.size() - 1)) * multiplier + ema.get(ema.size() - 1);
                ema.add(currentEMA);
            }
        }
        
        return ema;
    }
}
