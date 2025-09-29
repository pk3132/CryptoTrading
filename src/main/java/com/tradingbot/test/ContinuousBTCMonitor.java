package com.tradingbot.test;

import com.tradingbot.service.DeltaApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Continuous BTC EMA 200 monitoring until 12:00
 */
public class ContinuousBTCMonitor {
    
    private static final Logger logger = LoggerFactory.getLogger(ContinuousBTCMonitor.class);
    
    public static void main(String[] args) {
        logger.info("🕐 Starting Continuous BTC EMA 200 Monitor");
        logger.info("==========================================");
        logger.info("⏰ Will monitor until 12:00");
        logger.info("📊 Checking every 2 minutes");
        logger.info("");
        
        try {
            DeltaApiClient apiClient = new DeltaApiClient();
            String symbol = "BTCUSD";
            String timeframe = "1m";
            
            int checkCount = 0;
            
            while (true) {
                checkCount++;
                LocalDateTime now = LocalDateTime.now();
                String timeStr = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                
                logger.info("🔍 Check #{} - Time: {}", checkCount, timeStr);
                logger.info("=====================================");
                
                // Check if it's 12:00 or later
                if (now.getHour() >= 12) {
                    logger.info("⏰ 12:00 reached! Stopping monitor.");
                    break;
                }
                
                try {
                    // Fetch current data
                    long nowEpoch = System.currentTimeMillis() / 1000;
                    long start = nowEpoch - (500 * 60); // Use 500 candles like main bot
                    
                    List<Map<String, Object>> candles = apiClient.fetchOhlcv(symbol, timeframe, start, nowEpoch);
                    
                    if (candles != null && !candles.isEmpty()) {
                        // Extract close prices
                        List<Double> closePrices = new ArrayList<>();
                        for (Map<String, Object> candle : candles) {
                            closePrices.add(((Number) candle.get("close")).doubleValue());
                        }
                        
                        // Calculate EMA 200
                        double[] ema200 = EMA200Calculator.calculateEMA(closePrices, 200);
                        double lastEMA = ema200[ema200.length - 1];
                        
                        // Get current price
                        Double currentPrice = apiClient.getCurrentMarkPrice(symbol);
                        
                        if (currentPrice != null) {
                            // Calculate distance and trend
                            double distance = ((currentPrice - lastEMA) / lastEMA) * 100;
                            String trend = currentPrice > lastEMA ? "ABOVE" : "BELOW";
                            String trendEmoji = currentPrice > lastEMA ? "📈" : "📉";
                            
                            logger.info("💰 BTC Price: $" + String.format("%.2f", currentPrice));
                            logger.info("📊 EMA 200: $" + String.format("%.2f", lastEMA));
                            logger.info("📏 Distance: " + String.format("%.2f", distance) + "%");
                            logger.info("🎯 Trend: {} {}", trendEmoji, trend);
                            
                            if (currentPrice > lastEMA) {
                                logger.info("✅ BULLISH: Looking for BUY signals");
                            } else {
                                logger.info("❌ BEARISH: Looking for SELL signals");
                            }
                        }
                    }
                    
                } catch (Exception e) {
                    logger.error("❌ Error in check #{}: {}", checkCount, e.getMessage());
                }
                
                logger.info("");
                
                // Wait 2 minutes (120 seconds) before next check
                try {
                    Thread.sleep(120000); // 2 minutes
                } catch (InterruptedException e) {
                    logger.info("⏹️ Monitor stopped by user");
                    break;
                }
            }
            
            logger.info("🏁 Monitoring completed at {}", 
                       LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            
        } catch (Exception e) {
            logger.error("❌ Monitor error: {}", e.getMessage(), e);
        }
    }
}

/**
 * EMA Calculator
 */
class EMA200Calculator {
    
    public static double[] calculateEMA(List<Double> prices, int period) {
        double[] ema = new double[prices.size()];
        double multiplier = 2.0 / (period + 1);

        if (prices.size() < period) {
            throw new IllegalArgumentException("Not enough data points for EMA-" + period);
        }

        // Step 1: Seed value = SMA of first 'period' closes
        double sma = 0.0;
        for (int i = 0; i < period; i++) {
            sma += prices.get(i);
        }
        sma /= period;
        ema[period - 1] = sma;

        // Step 2: EMA formula for the rest
        for (int i = period; i < prices.size(); i++) {
            ema[i] = (prices.get(i) - ema[i - 1]) * multiplier + ema[i - 1];
        }

        return ema;
    }
}
