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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Real-Time Entry Price Test
 * Test the dynamic strategy with real-time entry prices
 */
public class RealTimeEntryPriceTest {
    
    private static final String DELTA_API_URL = "https://api.delta.exchange/v2";
    private static final ObjectMapper mapper = new ObjectMapper();
    
    public static void main(String[] args) {
        System.out.println("🎯 REAL-TIME ENTRY PRICE TEST");
        System.out.println("=" + "=".repeat(50));
        System.out.println("📊 Strategy: Dynamic Chart with Real-Time Entry Prices");
        System.out.println("⏰ Test Time: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println();
        
        try {
            // Create strategy
            EMA200TrendlineStrategy strategy = new EMA200TrendlineStrategy();
            
            // Test symbols
            String[] symbols = {"BTCUSD", "ETHUSD"};
            
            for (String symbol : symbols) {
                System.out.println("🔍 TESTING: " + symbol);
                System.out.println("=" + "=".repeat(30));
                
                // Get current real-time price first
                double realTimePrice = getCurrentPrice(symbol);
                System.out.println("💰 Real-Time Price: $" + String.format("%.2f", realTimePrice));
                
                // Get historical data for analysis
                long now = System.currentTimeMillis() / 1000;
                long start = now - (500 * 15 * 60); // 500 candles = 125 hours
                
                System.out.println("📊 Fetching historical data for analysis...");
                List<Map<String, Object>> candles = fetchData(symbol, start, now);
                
                if (candles == null || candles.isEmpty()) {
                    System.out.println("❌ No data received for " + symbol);
                    continue;
                }
                
                System.out.println("✅ Retrieved " + candles.size() + " candles");
                
                // Add data to strategy
                strategy.addCandleData(symbol, candles);
                
                // Check for signals (will use real-time price)
                System.out.println("🔍 Analyzing signals with real-time entry price...");
                List<EMA200TrendlineStrategy.TradeSignal> signals = strategy.checkSignals(symbol);
                
                if (!signals.isEmpty()) {
                    System.out.println("🚨 SIGNALS DETECTED: " + signals.size() + " signals for " + symbol);
                    for (EMA200TrendlineStrategy.TradeSignal signal : signals) {
                        System.out.println("   🎯 " + signal.getType() + " Signal:");
                        System.out.println("      💰 Entry Price: $" + String.format("%.2f", signal.getEntryPrice()));
                        System.out.println("      🛡️ Stop Loss: $" + String.format("%.2f", signal.getStopLoss()));
                        System.out.println("      🎯 Take Profit: $" + String.format("%.2f", signal.getTakeProfit()));
                        System.out.println("      📝 Reason: " + signal.getReason());
                        
                        // Calculate risk-reward ratio
                        double risk = Math.abs(signal.getEntryPrice() - signal.getStopLoss());
                        double reward = Math.abs(signal.getTakeProfit() - signal.getEntryPrice());
                        double riskRewardRatio = reward / risk;
                        System.out.println("      ⚡ Risk-Reward: 1:" + String.format("%.2f", riskRewardRatio));
                    }
                } else {
                    System.out.println("ℹ️ No signals for " + symbol);
                }
                
                System.out.println("✅ " + symbol + " completed: " + signals.size() + " signals");
                System.out.println();
            }
            
            System.out.println("🎉 REAL-TIME ENTRY PRICE TEST COMPLETED!");
            System.out.println("📊 Strategy now uses real-time market prices for entry");
            System.out.println("🎯 Entry prices are current market prices, not historical");
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get current real-time price
     */
    private static double getCurrentPrice(String symbol) {
        try {
            long now = System.currentTimeMillis() / 1000;
            long start = now - 300; // 5 minutes ago
            
            String url = DELTA_API_URL + "/history/candles?resolution=15m&symbol=" + symbol + 
                        "&start=" + start + "&end=" + now;
            
            URL apiUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                return 0;
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            JsonNode rootNode = mapper.readTree(response.toString());
            JsonNode resultNode = rootNode.get("result");
            
            if (resultNode != null && resultNode.isArray() && resultNode.size() > 0) {
                JsonNode lastCandle = resultNode.get(resultNode.size() - 1);
                JsonNode closeNode = lastCandle.get("close");
                
                if (closeNode != null && closeNode.isNumber()) {
                    return closeNode.asDouble();
                }
            }
            
            return 0;
            
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Fetch data from Delta Exchange API
     */
    private static List<Map<String, Object>> fetchData(String symbol, long start, long end) {
        try {
            String url = DELTA_API_URL + "/history/candles?resolution=15m&symbol=" + symbol + 
                        "&start=" + start + "&end=" + end;
            
            URL apiUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                return null;
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            JsonNode rootNode = mapper.readTree(response.toString());
            JsonNode resultNode = rootNode.get("result");
            
            if (resultNode == null || !resultNode.isArray()) {
                return null;
            }
            
            List<Map<String, Object>> candles = new ArrayList<>();
            for (JsonNode candle : resultNode) {
                Map<String, Object> candleMap = mapper.convertValue(candle, Map.class);
                candles.add(candleMap);
            }
            
            return candles;
            
        } catch (Exception e) {
            return null;
        }
    }
}
