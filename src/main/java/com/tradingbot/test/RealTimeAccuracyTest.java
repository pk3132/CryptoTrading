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
 * Real-Time Accuracy Test
 * Test the accuracy of real-time price fetching
 */
public class RealTimeAccuracyTest {
    
    private static final String DELTA_API_URL = "https://api.delta.exchange/v2";
    private static final ObjectMapper mapper = new ObjectMapper();
    
    public static void main(String[] args) {
        System.out.println("🎯 REAL-TIME ACCURACY TEST");
        System.out.println("=" + "=".repeat(50));
        System.out.println("📊 Testing real-time price accuracy");
        System.out.println("⏰ Test Time: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println();
        
        try {
            // Test current price accuracy
            String symbol = "BTCUSD";
            
            System.out.println("🔍 TESTING: " + symbol);
            System.out.println("=" + "=".repeat(30));
            
            // Get real-time price using 1-minute candles
            double realTimePrice = getRealTimePrice(symbol);
            System.out.println("💰 Real-Time Price (1m): $" + String.format("%.2f", realTimePrice));
            
            // Get price using 15-minute candles
            double price15m = getPrice15m(symbol);
            System.out.println("💰 Price (15m): $" + String.format("%.2f", price15m));
            
            // Get price using 5-minute candles
            double price5m = getPrice5m(symbol);
            System.out.println("💰 Price (5m): $" + String.format("%.2f", price5m));
            
            // Calculate differences
            double diff15m = Math.abs(realTimePrice - price15m);
            double diff5m = Math.abs(realTimePrice - price5m);
            
            System.out.println("\n📊 ACCURACY ANALYSIS:");
            System.out.println("   Real-Time (1m): $" + String.format("%.2f", realTimePrice));
            System.out.println("   15m Price: $" + String.format("%.2f", price15m) + " (Diff: $" + String.format("%.2f", diff15m) + ")");
            System.out.println("   5m Price: $" + String.format("%.2f", price5m) + " (Diff: $" + String.format("%.2f", diff5m) + ")");
            
            // Determine best resolution
            if (diff5m < diff15m) {
                System.out.println("   ✅ 5m resolution is more accurate");
            } else {
                System.out.println("   ✅ 1m resolution is most accurate");
            }
            
            // Test strategy with real-time price
            System.out.println("\n🔍 STRATEGY TEST:");
            EMA200TrendlineStrategy strategy = new EMA200TrendlineStrategy();
            
            // Get historical data for analysis
            long now = System.currentTimeMillis() / 1000;
            long start = now - (500 * 15 * 60); // 500 candles = 125 hours
            
            List<Map<String, Object>> candles = fetchData(symbol, start, now);
            if (candles != null && !candles.isEmpty()) {
                strategy.addCandleData(symbol, candles);
                List<EMA200TrendlineStrategy.TradeSignal> signals = strategy.checkSignals(symbol);
                
                if (!signals.isEmpty()) {
                    for (EMA200TrendlineStrategy.TradeSignal signal : signals) {
                        System.out.println("   🎯 Signal: " + signal.getType() + " at $" + String.format("%.2f", signal.getEntryPrice()));
                        System.out.println("   📊 Entry Price: $" + String.format("%.2f", signal.getEntryPrice()));
                        System.out.println("   🛡️ Stop Loss: $" + String.format("%.2f", signal.getStopLoss()));
                        System.out.println("   🎯 Take Profit: $" + String.format("%.2f", signal.getTakeProfit()));
                    }
                } else {
                    System.out.println("   ℹ️ No signals generated");
                }
            }
            
            System.out.println("\n✅ Real-time accuracy test completed!");
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get real-time price using 1-minute candles
     */
    private static double getRealTimePrice(String symbol) {
        try {
            long now = System.currentTimeMillis() / 1000;
            String url = DELTA_API_URL + "/history/candles?resolution=1m&symbol=" + symbol + 
                        "&start=" + (now - 60) + "&end=" + now;
            
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
     * Get price using 15-minute candles
     */
    private static double getPrice15m(String symbol) {
        try {
            long now = System.currentTimeMillis() / 1000;
            String url = DELTA_API_URL + "/history/candles?resolution=15m&symbol=" + symbol + 
                        "&start=" + (now - 900) + "&end=" + now;
            
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
     * Get price using 5-minute candles
     */
    private static double getPrice5m(String symbol) {
        try {
            long now = System.currentTimeMillis() / 1000;
            String url = DELTA_API_URL + "/history/candles?resolution=5m&symbol=" + symbol + 
                        "&start=" + (now - 300) + "&end=" + now;
            
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
