package com.tradingbot.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Simple Current Price Check
 * Get the most recent Bitcoin price
 */
public class SimplePriceCheck {
    
    private static final String DELTA_API_URL = "https://api.delta.exchange/v2";
    private static final ObjectMapper mapper = new ObjectMapper();
    
    public static void main(String[] args) {
        System.out.println("💰 SIMPLE BITCOIN PRICE CHECK");
        System.out.println("=" + "=".repeat(40));
        System.out.println("⏰ Check Time: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println();
        
        try {
            // Get current time
            long now = System.currentTimeMillis() / 1000;
            long start = now - (10 * 15 * 60); // Just 10 candles = 2.5 hours
            
            System.out.println("📊 Fetching most recent Bitcoin price...");
            
            // Fetch recent Bitcoin data
            double currentPrice = fetchCurrentBitcoinPrice("BTCUSD", start, now);
            
            if (currentPrice > 0) {
                System.out.println("✅ Current Bitcoin Price: $" + String.format("%.2f", currentPrice));
                System.out.println("⏰ Fetched at: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                
                // Compare with user's price
                double userPrice = 115741.0;
                double difference = currentPrice - userPrice;
                double differencePercent = (difference / userPrice) * 100;
                
                System.out.println("\n📊 COMPARISON:");
                System.out.println("Your Price: $" + String.format("%.2f", userPrice));
                System.out.println("API Price: $" + String.format("%.2f", currentPrice));
                System.out.println("Difference: $" + String.format("%.2f", difference));
                System.out.println("Difference %: " + String.format("%.2f", differencePercent) + "%");
                
                if (Math.abs(differencePercent) > 1.0) {
                    System.out.println("⚠️ WARNING: API data may be delayed or incorrect!");
                } else {
                    System.out.println("✅ API data looks accurate");
                }
                
            } else {
                System.out.println("❌ Failed to fetch current price");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Fetch current Bitcoin price from Delta Exchange API
     */
    private static double fetchCurrentBitcoinPrice(String symbol, long start, long end) {
        try {
            String url = DELTA_API_URL + "/history/candles?resolution=15m&symbol=" + symbol + 
                        "&start=" + start + "&end=" + end;
            
            URL apiUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(5000); // 5 seconds timeout
            connection.setReadTimeout(5000); // 5 seconds timeout
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode != 200) {
                System.err.println("❌ API Error: " + responseCode);
                return 0;
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
                System.err.println("❌ No data received");
                return 0;
            }
            
            // Get the most recent candle (last one)
            JsonNode lastCandle = resultNode.get(resultNode.size() - 1);
            JsonNode closeNode = lastCandle.get("close");
            
            if (closeNode != null && closeNode.isNumber()) {
                return closeNode.asDouble();
            }
            
            return 0;
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching price: " + e.getMessage());
            return 0;
        }
    }
}
