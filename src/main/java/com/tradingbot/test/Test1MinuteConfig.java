package com.tradingbot.test;

import com.tradingbot.service.DeltaApiClient;
import com.tradingbot.strategy.EMA200TrendlineStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Test 1 minute configuration
 */
public class Test1MinuteConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(Test1MinuteConfig.class);
    
    public static void main(String[] args) {
        logger.info("🧪 Testing 1 Minute Configuration");
        logger.info("==================================");
        
        // Initialize services
        DeltaApiClient deltaApiClient = new DeltaApiClient();
        EMA200TrendlineStrategy strategy = new EMA200TrendlineStrategy();
        
        try {
            String symbol = "BTCUSD";
            int candles = 500; // Use 500 candles
            long now = System.currentTimeMillis() / 1000;
            long start = now - (candles * 60); // 1 minute per candle
            
            logger.info("📊 Fetching {} candles for BTC (1m timeframe)...", candles);
            List<Map<String, Object>> candleData = deltaApiClient.fetchOhlcv(symbol, "1m", start, now);
            
            if (candleData != null && !candleData.isEmpty()) {
                logger.info("✅ Fetched {} candles for 1m timeframe", candleData.size());
                
                // Add to strategy
                strategy.addCandleData(symbol, candleData);
                
                // Get EMA 200
                Double ema200 = strategy.getLastEma200(symbol);
                
                if (ema200 != null) {
                    logger.info("📈 EMA 200 (1m, 500 candles): ${}", String.format("%.2f", ema200));
                    
                    // Get current price
                    Double currentPrice = deltaApiClient.getCurrentMarkPrice(symbol);
                    
                    if (currentPrice != null) {
                        double distance = ((currentPrice - ema200) / ema200) * 100;
                        String trend = currentPrice > ema200 ? "📈 ABOVE" : "📉 BELOW";
                        
                        logger.info("💰 Current BTC Price: ${}", String.format("%.2f", currentPrice));
                        logger.info("📊 EMA 200: ${}", String.format("%.2f", ema200));
                        logger.info("📏 Distance: {:.2f}%", distance);
                        logger.info("🎯 Trend: {}", trend);
                        
                        if (currentPrice > ema200) {
                            logger.info("✅ BULLISH: Looking for BUY signals");
                        } else {
                            logger.info("❌ BEARISH: Looking for SELL signals");
                        }
                    }
                } else {
                    logger.warn("⚠️ EMA 200 calculation failed");
                }
                
            } else {
                logger.error("❌ Failed to fetch candles for 1m timeframe");
            }
            
        } catch (Exception e) {
            logger.error("❌ Error testing 1m configuration: {}", e.getMessage());
        }
        
        logger.info("\n✅ 1 Minute Configuration Test Complete!");
    }
}
