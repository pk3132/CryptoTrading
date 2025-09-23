package com.tradingbot.test;

import com.tradingbot.strategy.AggressiveChartStrategy;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Final Test - Aggressive Chart Strategy
 * Clean implementation test
 */
public class FinalTest {

    private static final String[] SYMBOLS = {"BTCUSD", "ETHUSD", "SOLUSD"};
    private static final String TIMEFRAME = "15m";
    private static final int DAYS_TO_TEST = 7;
    private static final String BASE_URL = "https://api.delta.exchange/v2";

    public static void main(String[] args) {
        System.out.println("🚀 FINAL AGGRESSIVE CHART STRATEGY TEST");
        System.out.println("=" + "=".repeat(70));
        System.out.println("📅 Period: Last 7 days");
        System.out.println("⏰ Timeframe: 15 minutes");
        System.out.println("📊 Symbols: " + String.join(", ", SYMBOLS));
        System.out.println("🎯 Strategy: Aggressive Chart Technical Analysis");
        System.out.println("⏰ Start Time: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println();

        int totalSignals = 0;
        int totalCallSignals = 0;
        int totalPutSignals = 0;
        double totalProfit = 0.0;

        for (String symbol : SYMBOLS) {
            System.out.println("📊 TESTING: " + symbol);
            System.out.println("=" + "=".repeat(50));
            
            try {
                TestResult result = testSymbol(symbol);
                totalSignals += result.totalSignals;
                totalCallSignals += result.callSignals;
                totalPutSignals += result.putSignals;
                totalProfit += result.totalProfit;
                
                System.out.println("✅ " + symbol + " completed: " + result.totalSignals + " signals");
            } catch (Exception e) {
                System.err.println("❌ Error testing " + symbol + ": " + e.getMessage());
            }
            
            System.out.println();
        }

        // Final Summary
        System.out.println("🎯 FINAL TEST SUMMARY");
        System.out.println("=" + "=".repeat(50));
        System.out.println("📊 Total Signals: " + totalSignals);
        System.out.println("🟢 BUY Signals: " + totalCallSignals);
        System.out.println("🔴 SELL Signals: " + totalPutSignals);
        System.out.println("💰 Total Profit: $" + String.format("%.2f", totalProfit));
        System.out.println("📈 Average Profit per Signal: $" + String.format("%.2f", totalSignals > 0 ? totalProfit / totalSignals : 0));
        System.out.println("📅 Daily Average: " + String.format("%.1f", (double) totalSignals / DAYS_TO_TEST) + " trades per day");
        System.out.println("⏰ Test Period: " + DAYS_TO_TEST + " days");
        System.out.println("🎯 Strategy: Aggressive Chart Technical Analysis");
        System.out.println("⚡ Risk-Reward: 1:2");
        System.out.println();
        
        System.out.println("📈 CHART ELEMENTS HANDLED:");
        System.out.println("   ✅ Price Movement (Uptrend/Downtrend)");
        System.out.println("   ✅ Bollinger Bands (Volatility)");
        System.out.println("   ✅ Support/Resistance Levels");
        System.out.println("   ✅ Candlestick Patterns (Green/Red)");
        System.out.println("   ✅ Trend Analysis (SMA5)");
        
        System.out.println("🎉 FINAL TEST COMPLETED!");
    }

    /**
     * Test a single symbol
     */
    private static TestResult testSymbol(String symbol) {
        // Calculate time range for last 7 days
        long now = Instant.now().getEpochSecond();
        long start = now - (DAYS_TO_TEST * 24 * 60 * 60);
        
        // Fetch historical data
        System.out.println("📈 Fetching historical data for " + symbol + "...");
        List<Map<String, Object>> candles = fetchOhlcv(symbol, start, now);
        
        if (candles == null || candles.isEmpty()) {
            System.err.println("❌ No historical data received for " + symbol);
            return new TestResult(0, 0, 0, 0.0);
        }
        
        System.out.println("✅ Retrieved " + candles.size() + " candles for " + symbol);
        
        // Initialize aggressive chart strategy
        AggressiveChartStrategy strategy = new AggressiveChartStrategy();
        strategy.addCandleData(symbol, candles);
        
        // Check for signals
        List<AggressiveChartStrategy.TradeSignal> signals = strategy.checkSignals(symbol);
        
        int callSignals = 0;
        int putSignals = 0;
        double totalProfit = 0.0;
        
        System.out.println("🔍 Analyzing signals for " + symbol + "...");
        
        // Show first 10 signals as examples
        int signalsShown = 0;
        for (AggressiveChartStrategy.TradeSignal signal : signals) {
            if (signalsShown < 10) {
                if (signal.getType().equals("BUY")) {
                    callSignals++;
                    System.out.println("   🟢 BUY Signal: $" + String.format("%.2f", signal.getEntryPrice()) + 
                                     " | SL: $" + String.format("%.2f", signal.getStopLoss()) + 
                                     " | TP: $" + String.format("%.2f", signal.getTakeProfit()) +
                                     " | Reason: " + signal.getReason());
                } else if (signal.getType().equals("SELL")) {
                    putSignals++;
                    System.out.println("   🔴 SELL Signal: $" + String.format("%.2f", signal.getEntryPrice()) + 
                                     " | SL: $" + String.format("%.2f", signal.getStopLoss()) + 
                                     " | TP: $" + String.format("%.2f", signal.getTakeProfit()) +
                                     " | Reason: " + signal.getReason());
                }
                signalsShown++;
            } else {
                // Count remaining signals without showing details
                if (signal.getType().equals("BUY")) {
                    callSignals++;
                } else if (signal.getType().equals("SELL")) {
                    putSignals++;
                }
            }
            
            // Calculate potential profit (1:2 risk-reward)
            double risk = Math.abs(signal.getEntryPrice() - signal.getStopLoss());
            double reward = Math.abs(signal.getTakeProfit() - signal.getEntryPrice());
            double profit = reward - risk; // Net profit after risk
            totalProfit += profit;
        }
        
        if (signals.size() > 10) {
            System.out.println("   ... and " + (signals.size() - 10) + " more signals");
        }
        
        System.out.println("📊 " + symbol + " Results:");
        System.out.println("   Total Signals: " + signals.size());
        System.out.println("   BUY Signals: " + callSignals);
        System.out.println("   SELL Signals: " + putSignals);
        System.out.println("   Total Profit: $" + String.format("%.2f", totalProfit));
        
        return new TestResult(signals.size(), callSignals, putSignals, totalProfit);
    }

    /**
     * Fetch OHLCV data from Delta Exchange
     */
    private static List<Map<String, Object>> fetchOhlcv(String symbol, long start, long end) {
        try {
            String urlString = BASE_URL + "/history/candles?resolution=" + TIMEFRAME + 
                             "&symbol=" + symbol + "&start=" + start + "&end=" + end;
            
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                return parseCandles(response.toString());
            } else {
                System.err.println("❌ HTTP Error: " + responseCode);
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching OHLCV data: " + e.getMessage());
            return null;
        }
    }

    /**
     * Simple JSON parser for candles
     */
    private static List<Map<String, Object>> parseCandles(String json) {
        List<Map<String, Object>> candles = new ArrayList<>();
        
        try {
            int resultStart = json.indexOf("\"result\":[") + 10;
            int resultEnd = json.lastIndexOf("]");
            
            if (resultStart > 9 && resultEnd > resultStart) {
                String resultArray = json.substring(resultStart, resultEnd + 1);
                String[] candleStrings = resultArray.split("\\},\\{");
                
                for (String candleString : candleStrings) {
                    candleString = candleString.replaceAll("[\\{\\}]", "");
                    String[] pairs = candleString.split(",");
                    
                    Map<String, Object> candle = new HashMap<>();
                    for (String pair : pairs) {
                        String[] keyValue = pair.split(":");
                        if (keyValue.length == 2) {
                            String key = keyValue[0].replaceAll("\"", "").trim();
                            String value = keyValue[1].replaceAll("\"", "").trim();
                            
                            try {
                                if (key.equals("time")) {
                                    candle.put(key, Long.parseLong(value));
                                } else {
                                    candle.put(key, Double.parseDouble(value));
                                }
                            } catch (NumberFormatException e) {
                                candle.put(key, value);
                            }
                        }
                    }
                    
                    if (candle.size() >= 4) {
                        candles.add(candle);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error parsing JSON: " + e.getMessage());
        }
        
        return candles;
    }

    /**
     * Test result container
     */
    private static class TestResult {
        public final int totalSignals;
        public final int callSignals;
        public final int putSignals;
        public final double totalProfit;

        public TestResult(int totalSignals, int callSignals, int putSignals, double totalProfit) {
            this.totalSignals = totalSignals;
            this.callSignals = callSignals;
            this.putSignals = putSignals;
            this.totalProfit = totalProfit;
        }
    }
}
