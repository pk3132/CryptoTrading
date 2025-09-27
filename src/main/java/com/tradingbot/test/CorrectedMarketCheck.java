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
 * Corrected Market Check - Uses CURRENT MARK PRICE like the trading system
 * This matches how the actual trading strategy makes decisions
 */
public class CorrectedMarketCheck {
    
    private static final String DELTA_API_URL = "https://api.delta.exchange/v2";
    private static final ObjectMapper mapper = new ObjectMapper();
    
    public static void main(String[] args) {
        System.out.println("🔍 CORRECTED MARKET CHECK - USING CURRENT MARK PRICE");
        System.out.println("=" + "=".repeat(70));
        System.out.println("⏰ Check Time: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("💡 This matches how the trading system makes decisions");
        System.out.println();
        
        // Check BTC
        System.out.println("🟠 BITCOIN (BTCUSD) ANALYSIS");
        System.out.println("=" + "=".repeat(50));
        checkSymbol("BTCUSD");
        
        System.out.println("\n" + "=".repeat(70) + "\n");
        
        // Check ETH
        System.out.println("🔵 ETHEREUM (ETHUSD) ANALYSIS");
        System.out.println("=" + "=".repeat(50));
        checkSymbol("ETHUSD");
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("✅ Corrected market check completed!");
        System.out.println("💡 This analysis uses CURRENT MARK PRICE like the trading system");
    }
    
    private static void checkSymbol(String symbol) {
        try {
            // Get current time and enough candles for EMA 200
            long now = System.currentTimeMillis() / 1000;
            long start = now - (300 * 15 * 60); // 300 candles = 75 hours
            
            System.out.println("📊 Fetching " + symbol + " data...");
            System.out.println("⏰ Time Range: " + (now - start) / 60 + " minutes ago to now");
            
            // Fetch candle data for EMA calculation
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
            
            // Calculate EMA 200
            List<Double> ema200 = calculateEMA200Proper(closes);
            double currentEma200 = ema200.get(ema200.size() - 1);
            
            // Get CURRENT MARK PRICE (like the trading system does)
            double currentMarkPrice = getCurrentMarkPrice(symbol);
            
            if (currentMarkPrice <= 0) {
                System.out.println("❌ Could not get current mark price for " + symbol);
                return;
            }
            
            // Show comparison
            double lastClosePrice = closes.get(closes.size() - 1);
            
            System.out.println("\n📊 PRICE COMPARISON");
            System.out.println("-" + "-".repeat(40));
            System.out.println("💰 Current Mark Price: $" + String.format("%.2f", currentMarkPrice));
            System.out.println("📈 Last Close Price: $" + String.format("%.2f", lastClosePrice));
            System.out.println("📊 EMA 200: $" + String.format("%.2f", currentEma200));
            System.out.println("📏 Price Difference: $" + String.format("%.2f", currentMarkPrice - lastClosePrice));
            
            // Calculate differences
            double markDifference = currentMarkPrice - currentEma200;
            double markDifferencePercent = (markDifference / currentEma200) * 100;
            
            double closeDifference = lastClosePrice - currentEma200;
            double closeDifferencePercent = (closeDifference / currentEma200) * 100;
            
            System.out.println("\n🎯 TREND ANALYSIS (USING CURRENT MARK PRICE)");
            System.out.println("-" + "-".repeat(40));
            System.out.println("📊 Mark Price vs EMA 200: " + String.format("%.2f", markDifferencePercent) + "%");
            System.out.println("📊 Close Price vs EMA 200: " + String.format("%.2f", closeDifferencePercent) + "%");
            
            if (currentMarkPrice > currentEma200) {
                System.out.println("🟢 " + symbol + " is ABOVE EMA 200 (using mark price)");
                System.out.println("📈 Trend: BULLISH");
                System.out.println("💡 Trading Signal: BUY opportunities possible");
            } else if (currentMarkPrice < currentEma200) {
                System.out.println("🔴 " + symbol + " is BELOW EMA 200 (using mark price)");
                System.out.println("📉 Trend: BEARISH");
                System.out.println("💡 Trading Signal: SELL opportunities possible");
            } else {
                System.out.println("🟡 " + symbol + " is AT EMA 200 (using mark price)");
                System.out.println("📊 Trend: NEUTRAL");
            }
            
            // Show what the old analysis would show
            System.out.println("\n⚠️ COMPARISON WITH OLD ANALYSIS");
            System.out.println("-" + "-".repeat(40));
            if (lastClosePrice > currentEma200) {
                System.out.println("📈 Old Analysis (using close price): ABOVE EMA 200");
            } else {
                System.out.println("📉 Old Analysis (using close price): BELOW EMA 200");
            }
            
            if (currentMarkPrice > currentEma200) {
                System.out.println("📈 Corrected Analysis (using mark price): ABOVE EMA 200");
            } else {
                System.out.println("📉 Corrected Analysis (using mark price): BELOW EMA 200");
            }
            
            // Show recent trend
            if (closes.size() >= 10) {
                double price10CandlesAgo = closes.get(closes.size() - 10);
                double recentChange = ((currentMarkPrice - price10CandlesAgo) / price10CandlesAgo) * 100;
                System.out.println("📈 Recent Change (10 candles): " + String.format("%.2f", recentChange) + "%");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error checking " + symbol + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get current mark price from tickers endpoint (like the trading system)
     */
    private static double getCurrentMarkPrice(String symbol) {
        try {
            String url = DELTA_API_URL + "/tickers?symbol=" + symbol;
            
            URL apiUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode != 200) {
                System.err.println("❌ Ticker API Error: " + responseCode);
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
                System.err.println("❌ No ticker data received for " + symbol);
                return 0;
            }
            
            // Find the correct symbol in the list
            for (JsonNode ticker : resultNode) {
                String tickerSymbol = ticker.get("symbol").asText();
                if (symbol.equals(tickerSymbol)) {
                    JsonNode markPriceNode = ticker.get("mark_price");
                    if (markPriceNode != null && markPriceNode.isNumber()) {
                        return markPriceNode.asDouble();
                    }
                }
            }
            
            return 0;
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching mark price for " + symbol + ": " + e.getMessage());
            return 0;
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
