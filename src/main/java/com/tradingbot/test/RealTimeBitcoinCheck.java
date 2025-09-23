package com.tradingbot.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Real-Time Bitcoin Check
 * Fetch current real-time data from Delta Exchange
 */
public class RealTimeBitcoinCheck {
    
    private static final String DELTA_API_URL = "https://api.delta.exchange/v2";
    private static final ObjectMapper mapper = new ObjectMapper();
    
    public static void main(String[] args) {
        System.out.println("🔍 REAL-TIME BITCOIN CHECK");
        System.out.println("=" + "=".repeat(50));
        System.out.println("⏰ Current Time: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println();
        
        try {
            // Get current time
            long now = System.currentTimeMillis() / 1000;
            long start = now - (200 * 15 * 60); // 200 candles = 50 hours for EMA 200
            
            System.out.println("📊 Fetching REAL-TIME Bitcoin data...");
            System.out.println("⏰ Time Range: " + (now - start) / 60 + " minutes ago to now");
            
            // Fetch real-time Bitcoin data
            List<Double> closes = fetchRealTimeBitcoinData("BTCUSD", start, now);
            
            if (closes == null || closes.isEmpty()) {
                System.out.println("❌ No real-time data received for BTCUSD");
                return;
            }
            
            System.out.println("✅ Retrieved " + closes.size() + " real-time candles");
            
            if (closes.size() < 200) {
                System.out.println("❌ Not enough data for EMA 200 calculation");
                return;
            }
            
            // Calculate EMA 200 using proper method
            List<Double> ema200 = calculateEMA200Proper(closes);
            
            // Get current price and EMA 200
            double currentPrice = closes.get(closes.size() - 1);
            double currentEma200 = ema200.get(ema200.size() - 1);
            
            // Calculate difference
            double difference = currentPrice - currentEma200;
            double differencePercent = (difference / currentEma200) * 100;
            
            System.out.println("\n📈 REAL-TIME BITCOIN ANALYSIS");
            System.out.println("=" + "=".repeat(30));
            System.out.println("💰 Current Price: $" + String.format("%.2f", currentPrice));
            System.out.println("📊 EMA 200: $" + String.format("%.2f", currentEma200));
            System.out.println("📏 Difference: $" + String.format("%.2f", difference));
            System.out.println("📊 Difference %: " + String.format("%.2f", differencePercent) + "%");
            
            System.out.println("\n🎯 REAL-TIME TREND ANALYSIS");
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
            
            // Show last 5 candles with EMA comparison
            System.out.println("\n📊 LAST 5 REAL-TIME CANDLES");
            System.out.println("=" + "=".repeat(30));
            int startIndex = Math.max(0, closes.size() - 5);
            int emaStartIndex = Math.max(0, ema200.size() - 5);
            
            for (int i = 0; i < 5; i++) {
                if (startIndex + i < closes.size() && emaStartIndex + i < ema200.size()) {
                    double price = closes.get(startIndex + i);
                    double ema = ema200.get(emaStartIndex + i);
                    String status = price > ema ? "🟢 ABOVE" : "🔴 BELOW";
                    System.out.println("Candle " + (startIndex + i + 1) + ": $" + String.format("%.2f", price) + " " + status + " EMA 200 ($" + String.format("%.2f", ema) + ")");
                }
            }
            
            // Show current market status
            System.out.println("\n📊 CURRENT MARKET STATUS");
            System.out.println("=" + "=".repeat(30));
            System.out.println("⏰ Check Time: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            System.out.println("💰 Bitcoin Price: $" + String.format("%.2f", currentPrice));
            System.out.println("📊 EMA 200: $" + String.format("%.2f", currentEma200));
            System.out.println("🎯 Trend: " + (currentPrice > currentEma200 ? "BULLISH" : "BEARISH"));
            System.out.println("📈 Signal: " + (currentPrice > currentEma200 ? "BUY" : "SELL"));
            
            System.out.println("\n✅ Real-time analysis completed!");
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Fetch real-time Bitcoin data from Delta Exchange API
     */
    private static List<Double> fetchRealTimeBitcoinData(String symbol, long start, long end) {
        try {
            String url = DELTA_API_URL + "/history/candles?resolution=15m&symbol=" + symbol + 
                        "&start=" + start + "&end=" + end;
            
            System.out.println("🔗 API URL: " + url);
            
            URL apiUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(10000); // 10 seconds timeout
            connection.setReadTimeout(10000); // 10 seconds timeout
            
            int responseCode = connection.getResponseCode();
            System.out.println("📡 API Response Code: " + responseCode);
            
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
            
            System.out.println("📊 Parsed " + closes.size() + " candles from API");
            return closes;
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching real-time data: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Calculate EMA 200 using proper method
     */
    private static List<Double> calculateEMA200Proper(List<Double> prices) {
        List<Double> ema = new ArrayList<>();
        int period = 200;
        double multiplier = 2.0 / (period + 1);
        
        // For first 200 values, use SMA as starting point
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
