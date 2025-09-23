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
 * 30-Day Backtest for EMA 200 + Trendline Breakout Strategy
 * Comprehensive analysis of the last 30 days
 */
public class ThirtyDayBacktest {
    
    private static final String DELTA_API_URL = "https://api.delta.exchange/v2";
    private static final ObjectMapper mapper = new ObjectMapper();
    
    public static void main(String[] args) {
        System.out.println("🎯 30-DAY BACKTEST - EMA 200 + TRENDLINE BREAKOUT STRATEGY");
        System.out.println("=" + "=".repeat(70));
        System.out.println("📊 Strategy: EMA 200 + Trendline Breakout");
        System.out.println("🔍 Features: EMA 200 Filter, Swing Points, Trendline Analysis");
        System.out.println("⏰ Period: Last 30 days");
        System.out.println("📈 Timeframe: 15 minutes");
        System.out.println("⚡ Risk-Reward: 1:2 (2% SL, 4% TP)");
        System.out.println("🎯 Symbols: BTCUSD, ETHUSD, SOLUSD");
        System.out.println();
        
        try {
            // Create strategy
            EMA200TrendlineStrategy strategy = new EMA200TrendlineStrategy();
            
            // Test symbols
            String[] symbols = {"BTCUSD", "ETHUSD", "SOLUSD"};
            
            int totalSignals = 0;
            int totalTrades = 0;
            double totalProfit = 0.0;
            double totalLoss = 0.0;
            
            for (String symbol : symbols) {
                System.out.println("🔍 TESTING: " + symbol);
                System.out.println("=" + "=".repeat(50));
                
                // Get 30 days of historical data
                long now = System.currentTimeMillis() / 1000;
                long start = now - (30 * 24 * 60 * 60); // 30 days in seconds
                
                System.out.println("📊 Fetching 30 days of historical data for " + symbol + "...");
                System.out.println("📅 From: " + LocalDateTime.ofEpochSecond(start, 0, java.time.ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                System.out.println("📅 To: " + LocalDateTime.ofEpochSecond(now, 0, java.time.ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                
                List<Map<String, Object>> candles = fetchData(symbol, start, now);
                
                if (candles == null || candles.isEmpty()) {
                    System.out.println("❌ No data received for " + symbol);
                    continue;
                }
                
                System.out.println("✅ Retrieved " + candles.size() + " candles over 30 days");
                
                // Add data to strategy
                strategy.addCandleData(symbol, candles);
                
                // Check for signals
                System.out.println("🔍 Analyzing signals for " + symbol + "...");
                List<EMA200TrendlineStrategy.TradeSignal> signals = strategy.checkSignals(symbol);
                
                if (!signals.isEmpty()) {
                    System.out.println("🚨 SIGNALS DETECTED: " + signals.size() + " signals for " + symbol);
                    totalSignals += signals.size();
                    
                    for (int i = 0; i < signals.size(); i++) {
                        EMA200TrendlineStrategy.TradeSignal signal = signals.get(i);
                        System.out.println("   📈 Trade #" + (i + 1) + ":");
                        System.out.println("      🎯 Type: " + signal.getType());
                        System.out.println("      💰 Entry: $" + String.format("%.2f", signal.getEntryPrice()));
                        System.out.println("      🛡️ Stop Loss: $" + String.format("%.2f", signal.getStopLoss()));
                        System.out.println("      🎯 Take Profit: $" + String.format("%.2f", signal.getTakeProfit()));
                        System.out.println("      📝 Reason: " + signal.getReason());
                        
                        // Calculate potential profit/loss
                        double entryPrice = signal.getEntryPrice();
                        double stopLoss = signal.getStopLoss();
                        double takeProfit = signal.getTakeProfit();
                        
                        if (signal.getType().equals("BUY")) {
                            double risk = entryPrice - stopLoss;
                            double reward = takeProfit - entryPrice;
                            double riskRewardRatio = reward / risk;
                            System.out.println("      ⚡ Risk: $" + String.format("%.2f", risk) + " | Reward: $" + String.format("%.2f", reward) + " | Ratio: " + String.format("%.2f", riskRewardRatio));
                        } else {
                            double risk = stopLoss - entryPrice;
                            double reward = entryPrice - takeProfit;
                            double riskRewardRatio = reward / risk;
                            System.out.println("      ⚡ Risk: $" + String.format("%.2f", risk) + " | Reward: $" + String.format("%.2f", reward) + " | Ratio: " + String.format("%.2f", riskRewardRatio));
                        }
                        
                        totalTrades++;
                    }
                } else {
                    System.out.println("ℹ️ No signals for " + symbol + " in the last 30 days");
                }
                
                System.out.println("✅ " + symbol + " completed: " + signals.size() + " signals");
                System.out.println();
            }
            
            // Summary
            System.out.println("📊 30-DAY BACKTEST SUMMARY");
            System.out.println("=" + "=".repeat(50));
            System.out.println("🎯 Total Signals: " + totalSignals);
            System.out.println("📈 Total Trades: " + totalTrades);
            System.out.println("📅 Period: 30 days");
            System.out.println("⏰ Test Time: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            System.out.println();
            
            if (totalSignals > 0) {
                System.out.println("🎉 STRATEGY PERFORMANCE:");
                System.out.println("✅ Strategy is generating signals");
                System.out.println("📊 Average signals per day: " + String.format("%.2f", (double) totalSignals / 30));
                System.out.println("📈 Signal frequency: " + String.format("%.1f", (double) totalSignals / 30 * 100) + "% of days");
            } else {
                System.out.println("⚠️ STRATEGY ANALYSIS:");
                System.out.println("❌ No signals generated in 30 days");
                System.out.println("🔍 This could mean:");
                System.out.println("   • Market conditions don't meet strict criteria");
                System.out.println("   • EMA 200 + Trendline conditions not met");
                System.out.println("   • Strategy is too conservative");
                System.out.println("   • Need to adjust parameters for more sensitivity");
            }
            
            System.out.println();
            System.out.println("🎯 STRATEGY DETAILS:");
            System.out.println("• BUY: Price > EMA200 + breaks above descending resistance");
            System.out.println("• SELL: Price < EMA200 + breaks below ascending support");
            System.out.println("• Risk-Reward: 1:2 (2% SL, 4% TP)");
            System.out.println("• Timeframe: 15 minutes");
            System.out.println("• Swing Point Detection: Last 5 candles");
            System.out.println("• Trendline Fitting: Linear Regression");
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
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
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            
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
