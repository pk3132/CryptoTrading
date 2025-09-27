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
 * ETHUSD EMA 200 Check
 * Check if ETHUSD is currently above or below EMA 200
 */
public class ETHUSDEma200Check {
    
    private static final String DELTA_API_URL = "https://api.delta.exchange/v2";
    private static final ObjectMapper mapper = new ObjectMapper();
    
    public static void main(String[] args) {
        System.out.println("🔍 ETHUSD EMA 200 CHECK");
        System.out.println("=" + "=".repeat(50));
        System.out.println("⏰ Check Time: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println();
        
        try {
            // Get current time and enough candles for EMA 200 calculation
            long now = System.currentTimeMillis() / 1000;
            long start = now - (500 * 15 * 60); // 500 candles = 125 hours for EMA 200
            
            System.out.println("📊 Fetching ETHUSD data...");
            System.out.println("⏰ Time Range: " + (now - start) / 60 + " minutes ago to now");
            
            // Fetch ETHUSD data
            List<Double> closes = fetchETHUSDData("ETHUSD", start, now);
            
            if (closes == null || closes.isEmpty()) {
                System.out.println("❌ No data received for ETHUSD");
                return;
            }
            
            System.out.println("✅ Retrieved " + closes.size() + " candles");
            
            if (closes.size() < 200) {
                System.out.println("❌ Not enough data for EMA 200 calculation (need at least 200 candles)");
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
            
            System.out.println("\n📈 ETHUSD ANALYSIS");
            System.out.println("=" + "=".repeat(30));
            System.out.println("💰 Current Price: $" + String.format("%.2f", currentPrice));
            System.out.println("📊 EMA 200: $" + String.format("%.2f", currentEma200));
            System.out.println("📏 Difference: $" + String.format("%.2f", difference));
            System.out.println("📊 Difference %: " + String.format("%.2f", differencePercent) + "%");
            
            System.out.println("\n🎯 TREND ANALYSIS");
            System.out.println("=" + "=".repeat(30));
            
            if (currentPrice > currentEma200) {
                System.out.println("🟢 ETHUSD is ABOVE EMA 200");
                System.out.println("📈 Trend: BULLISH");
                System.out.println("💡 Price is " + String.format("%.2f", differencePercent) + "% above EMA 200");
            } else if (currentPrice < currentEma200) {
                System.out.println("🔴 ETHUSD is BELOW EMA 200");
                System.out.println("📉 Trend: BEARISH");
                System.out.println("💡 Price is " + String.format("%.2f", Math.abs(differencePercent)) + "% below EMA 200");
            } else {
                System.out.println("🟡 ETHUSD is AT EMA 200");
                System.out.println("📊 Trend: NEUTRAL");
            }
            
            // Additional analysis
            System.out.println("\n📊 ADDITIONAL ANALYSIS");
            System.out.println("=" + "=".repeat(30));
            System.out.println("📅 Data Points: " + closes.size() + " candles");
            System.out.println("⏰ Timeframe: 15-minute candles");
            System.out.println("🔄 EMA Period: 200");
            System.out.println("📊 Price vs EMA: " + (currentPrice > currentEma200 ? "Above" : "Below"));
            
            // Show recent trend
            if (closes.size() >= 5) {
                double price5CandlesAgo = closes.get(closes.size() - 5);
                double recentChange = ((currentPrice - price5CandlesAgo) / price5CandlesAgo) * 100;
                System.out.println("📈 Recent Change (5 candles): " + String.format("%.2f", recentChange) + "%");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Fetch ETHUSD data from Delta Exchange API
     */
    private static List<Double> fetchETHUSDData(String symbol, long start, long end) {
        try {
            String url = DELTA_API_URL + "/history/candles?resolution=15m&symbol=" + symbol + 
                        "&start=" + start + "&end=" + end;
            
            URL apiUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(10000); // 10 seconds timeout
            connection.setReadTimeout(10000); // 10 seconds timeout
            
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
            
            if (resultNode == null || !resultNode.isArray() || resultNode.size() == 0) {
                System.err.println("❌ No data received for " + symbol);
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
            System.err.println("❌ Error fetching " + symbol + " data: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Calculate EMA 200 using proper exponential moving average formula
     */
    private static List<Double> calculateEMA200Proper(List<Double> prices) {
        List<Double> ema = new ArrayList<>();
        int period = 200;
        
        if (prices.size() < period) {
            return ema;
        }
        
        // Calculate multiplier
        double multiplier = 2.0 / (period + 1);
        
        // First EMA value is SMA of first 200 values
        double sum = 0;
        for (int i = 0; i < period; i++) {
            sum += prices.get(i);
        }
        double firstEMA = sum / period;
        ema.add(firstEMA);
        
        // Calculate subsequent EMA values
        for (int i = period; i < prices.size(); i++) {
            double currentPrice = prices.get(i);
            double previousEMA = ema.get(ema.size() - 1);
            double currentEMA = (currentPrice * multiplier) + (previousEMA * (1 - multiplier));
            ema.add(currentEMA);
        }
        
        return ema;
    }
}
