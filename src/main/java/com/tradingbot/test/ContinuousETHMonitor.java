package com.tradingbot.test;

import com.tradingbot.service.DeltaApiClient;
import com.tradingbot.strategy.EMA200Calculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ContinuousETHMonitor {

    private static final Logger logger = LoggerFactory.getLogger(ContinuousETHMonitor.class);

    public static void main(String[] args) {
        logger.info("🔍 Continuous ETH Monitor - 500 Candles (1m timeframe)");
        logger.info("=====================================================");
        logger.info("");

        try {
            DeltaApiClient apiClient = new DeltaApiClient();
            String symbol = "ETHUSD";
            String timeframe = "1m";
            
            int checkCount = 0;
            
            while (true) {
                checkCount++;
                logger.info("🔍 Check #{} - Time: {}", checkCount, 
                    java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
                logger.info("=====================================");
                
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
                            
                            logger.info("💰 ETH Price: ${}", String.format("%.2f", currentPrice));
                            logger.info("📊 EMA 200: ${}", String.format("%.2f", lastEMA));
                            logger.info("📏 Distance: {:.2f}%", distance);
                            logger.info("🎯 Trend: {} {}", trendEmoji, trend);
                            
                            if (currentPrice > lastEMA) {
                                logger.info("✅ BULLISH: Looking for BUY signals");
                                logger.info("🚀 READY FOR TRADING: Bot will look for BUY signals!");
                            } else {
                                logger.info("❌ BEARISH: Looking for SELL signals");
                                logger.info("🛑 NOT READY FOR TRADING: Waiting for BULLISH trend.");
                            }
                        } else {
                            logger.warn("❌ Could not fetch current ETH price");
                        }
                    } else {
                        logger.warn("❌ No candle data available");
                    }
                } catch (Exception e) {
                    logger.error("❌ Error during ETH check: {}", e.getMessage());
                }
                
                logger.info("");
                
                // Wait 2 minutes before next check
                try {
                    Thread.sleep(120000); // 2 minutes
                } catch (InterruptedException e) {
                    logger.info("🛑 Monitor stopped by user");
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("❌ Fatal error in ETH monitor: {}", e.getMessage(), e);
        }
    }
}
