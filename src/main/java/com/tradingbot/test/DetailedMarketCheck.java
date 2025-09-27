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
 * Detailed Market Check - Shows raw data for verification
 * Check both BTC and ETH with detailed information
 */
public class DetailedMarketCheck {
    
    private static final String DELTA_API_URL = "https://api.delta.exchange/v2";
    private static final ObjectMapper mapper = new ObjectMapper();
    
    public static void main(String[] args) {
        System.out.println("🔍 DETAILED MARKET CHECK - BTC & ETH");
        System.out.println("=" + "=".repeat(60));
        System.out.println("⏰ Check Time: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println();
        
        // Check BTC
        System.out.println("🟠 BITCOIN (BTCUSD) ANALYSIS");
        System.out.println("=" + "=".repeat(40));
        checkSymbol("BTCUSD");
        
        System.out.println("\n" + "=".repeat(60) + "\n");
        
        // Check ETH
        System.out.println("🔵 ETHEREUM (ETHUSD) ANALYSIS");
        System.out.println("=" + "=".repeat(40));
        checkSymbol("ETHUSD");
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("✅ Detailed market check completed!");
    }
    
    private static void checkSymbol(String symbol) {
        try {
            // Get current time and enough candles for EMA 200
            long now = System.currentTimeMillis() / 1000;
            long start = now - (300 * 15 * 60); // 300 candles = 75 hours
            
            System.out.println("📊 Fetching " + symbol + " data...");
            System.out.println("⏰ Time Range: " + (now - start) / 60 + " minutes ago to now");
            System.out.println("🔗 API URL: " + DELTA_API_URL + "/history/candles?resolution=15m&symbol=" + symbol + "&start=" + start + "&end=" + now);
            
            // Fetch data
            List<Double> closes = fetchSymbolData(symbol, start, now);
            
            if (closes == null || closes.isEmpty()) {
                System.out.println("❌ No data received for " + symbol);
                return;
            }
            
            System.out.println("✅ Retrieved " + closes.size() + " candles");
            
            if (closes.size() < 200) {
                System.out.println("❌ Not enough data for EMA 200 calculation (need at least 200 candles)");
                return;
            }
            
            // Show last 5 candles for verification
            System.out.println("\n📈 LAST 5 CANDLES:");
            System.out.println("-" + "-".repeat(30));
            for (int i = Math.max(0, closes.size() - 5); i < closes.size(); i++) {
                System.out.println("Candle " + (i + 1) + ": $" + String.format("%.2f", closes.get(i)));
            }
            
            // Calculate EMA 200
            List<Double> ema200 = calculateEMA200Proper(closes);
            
            // Get current price and EMA 200
            double currentPrice = closes.get(closes.size() - 1);
            double currentEma200 = ema200.get(ema200.size() - 1);
            
            // Calculate difference
            double difference = currentPrice - currentEma200;
            double differencePercent = (difference / currentEma200) * 100;
            
            System.out.println("\n📊 DETAILED ANALYSIS");
            System.out.println("-" + "-".repeat(30));
            System.out.println("💰 Current Price: $" + String.format("%.2f", currentPrice));
            System.out.println("📊 EMA 200: $" + String.format("%.2f", currentEma200));
            System.out.println("📏 Difference: $" + String.format("%.2f", difference));
            System.out.println("📊 Difference %: " + String.format("%.2f", differencePercent) + "%");
            
            System.out.println("\n🎯 TREND ANALYSIS");
            System.out.println("-" + "-".repeat(30));
            
            if (currentPrice > currentEma200) {
                System.out.println("🟢 " + symbol + " is ABOVE EMA 200");
                System.out.println("📈 Trend: BULLISH");
                System.out.println("💡 Price is " + String.format("%.2f", differencePercent) + "% above EMA 200");
            } else if (currentPrice < currentEma200) {
                System.out.println("🔴 " + symbol + " is BELOW EMA 200");
                System.out.println("📉 Trend: BEARISH");
                System.out.println("💡 Price is " + String.format("%.2f", Math.abs(differencePercent)) + "% below EMA 200");
            } else {
                System.out.println("🟡 " + symbol + " is AT EMA 200");
                System.out.println("📊 Trend: NEUTRAL");
            }
            
            // Show EMA calculation details
            System.out.println("\n🔢 EMA CALCULATION DETAILS");
            System.out.println("-" + "-".repeat(30));
            System.out.println("📅 Data Points: " + closes.size() + " candles");
            System.out.println("⏰ Timeframe: 15-minute candles");
            System.out.println("🔄 EMA Period: 200");
            System.out.println("📊 First EMA Value: $" + String.format("%.2f", ema200.get(0)));
            System.out.println("📊 Last EMA Value: $" + String.format("%.2f", currentEma200));
            
            // Show recent trend
            if (closes.size() >= 10) {
                double price10CandlesAgo = closes.get(closes.size() - 10);
                double recentChange = ((currentPrice - price10CandlesAgo) / price10CandlesAgo) * 100;
                System.out.println("📈 Recent Change (10 candles): " + String.format("%.2f", recentChange) + "%");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error checking " + symbol + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Fetch symbol data from Delta Exchange API
     */
    private static List<Double> fetchSymbolData(String symbol, long start, long end) {
        try {
            String url = DELTA_API_URL + "/history/candles?resolution=15m&symbol=" + symbol + 
                        "&start=" + start + "&end=" + end;
            
            URL apiUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            System.out.println("🔗 API Response Code: " + responseCode);
            
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
            
            System.out.println("📊 Parsed " + resultNode.size() + " candles from API");
            
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
