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
 * ETHUSD Current Price Check
 * Get the most recent Ethereum price
 */
public class ETHPriceCheck {
    
    private static final String DELTA_API_URL = "https://api.delta.exchange/v2";
    private static final ObjectMapper mapper = new ObjectMapper();
    
    public static void main(String[] args) {
        System.out.println("💰 ETHUSD CURRENT PRICE CHECK");
        System.out.println("=" + "=".repeat(40));
        System.out.println("⏰ Check Time: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println();
        
        try {
            // Get current time
            long now = System.currentTimeMillis() / 1000;
            long start = now - (10 * 15 * 60); // Just 10 candles = 2.5 hours
            
            System.out.println("📊 Fetching most recent ETHUSD price...");
            
            // Fetch recent ETHUSD data
            double currentPrice = fetchCurrentPrice("ETHUSD", start, now);
            
            if (currentPrice > 0) {
                System.out.println("✅ Current ETHUSD Price: $" + String.format("%.2f", currentPrice));
                System.out.println("⏰ Fetched at: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                
                // Show trend analysis
                System.out.println("\n📊 ETHUSD ANALYSIS:");
                System.out.println("Price: $" + String.format("%.2f", currentPrice));
                System.out.println("Status: Live data from Delta Exchange");
                System.out.println("API: Working correctly");
                
            } else {
                System.out.println("❌ Failed to fetch ETHUSD price");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Fetch current price from Delta Exchange API
     */
    private static double fetchCurrentPrice(String symbol, long start, long end) {
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
                System.err.println("❌ No data received for " + symbol);
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
            System.err.println("❌ Error fetching " + symbol + " price: " + e.getMessage());
            return 0;
        }
    }
}
