package com.tradingbot.test;

import com.tradingbot.strategy.EMA200TrendlineStrategy;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Dynamic Strategy Test
 * Test the new dynamic chart strategy with automatic trend detection
 */
public class DynamicStrategyTest {
    
    private static final String DELTA_API_URL = "https://api.delta.exchange/v2";
    private static final ObjectMapper mapper = new ObjectMapper();
    
    public static void main(String[] args) {
        System.out.println("🎯 DYNAMIC CHART STRATEGY TEST");
        System.out.println("=" + "=".repeat(50));
        System.out.println("📊 Strategy: Dynamic Chart Technical Analysis");
        System.out.println("🔍 Features: EMA 200 Trend Filter, Auto Detection");
        System.out.println("⏰ Period: Last 7 days");
        System.out.println("📈 Timeframe: 15 minutes");
        System.out.println("⚡ Risk-Reward: 1:2");
        System.out.println();
        
        try {
            // Create strategy
            EMA200TrendlineStrategy strategy = new EMA200TrendlineStrategy();
            
            // Test symbols
            String[] symbols = {"BTCUSD", "ETHUSD", "SOLUSDT"};
            
            for (String symbol : symbols) {
                System.out.println("🔍 TESTING: " + symbol);
                System.out.println("=" + "=".repeat(30));
                
                // Get historical data
                long now = System.currentTimeMillis() / 1000;
                long start = now - (500 * 15 * 60); // 500 candles = 125 hours
                
                System.out.println("📊 Fetching historical data for " + symbol + "...");
                List<Map<String, Object>> candles = fetchBitcoinData(symbol, start, now);
                
                if (candles == null || candles.isEmpty()) {
                    System.out.println("❌ No data received for " + symbol);
                    continue;
                }
                
                System.out.println("✅ Retrieved " + candles.size() + " candles");
                
                // Add data to strategy
                strategy.addCandleData(symbol, candles);
                
                // Check for signals
                System.out.println("🔍 Analyzing signals for " + symbol + "...");
                List<EMA200TrendlineStrategy.TradeSignal> signals = strategy.checkSignals(symbol);
                
                if (!signals.isEmpty()) {
                    System.out.println("🚨 SIGNALS DETECTED: " + signals.size() + " signals for " + symbol);
                    for (EMA200TrendlineStrategy.TradeSignal signal : signals) {
                        System.out.println("   🎯 " + signal.getType() + " Signal: $" + 
                            String.format("%.2f", signal.getEntryPrice()) + 
                            " | SL: $" + String.format("%.2f", signal.getStopLoss()) + 
                            " | TP: $" + String.format("%.2f", signal.getTakeProfit()) + 
                            " | Reason: " + signal.getReason());
                    }
                } else {
                    System.out.println("ℹ️ No signals for " + symbol);
                }
                
                System.out.println("✅ " + symbol + " completed: " + signals.size() + " signals");
                System.out.println();
            }
            
            System.out.println("🎉 DYNAMIC STRATEGY TEST COMPLETED!");
            System.out.println("📊 Strategy automatically detects market trend using EMA 200");
            System.out.println("🎯 BUY signals only when price > EMA 200");
            System.out.println("🎯 SELL signals only when price < EMA 200");
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Fetch data from Delta Exchange API
     */
    private static List<Map<String, Object>> fetchBitcoinData(String symbol, long start, long end) {
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
            
            List<Map<String, Object>> candles = new ArrayList<>();
            for (JsonNode candle : resultNode) {
                Map<String, Object> candleMap = mapper.convertValue(candle, Map.class);
                candles.add(candleMap);
            }
            
            return candles;
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching data: " + e.getMessage());
            return null;
        }
    }
}
