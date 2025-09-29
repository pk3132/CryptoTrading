package com.tradingbot.test;

import com.tradingbot.service.DeltaApiClient;
import com.tradingbot.strategy.EMA200TrendlineStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ETHTest500Candles {

    private static final Logger logger = LoggerFactory.getLogger(ETHTest500Candles.class);

    public static void main(String[] args) {
        logger.info("🧪 Testing ETH with 500 Candles (1-Minute Timeframe)");
        logger.info("==================================================");

        DeltaApiClient deltaApiClient = new DeltaApiClient();
        EMA200TrendlineStrategy strategy = new EMA200TrendlineStrategy();

        try {
            String symbol = "ETHUSD";
            int candles = 500; // Use 500 candles
            long now = System.currentTimeMillis() / 1000;
            long start = now - (candles * 60); // 1 minute per candle

            logger.info("📊 Fetching {} candles for ETH (1m timeframe)...", candles);
            List<Map<String, Object>> candleData = deltaApiClient.fetchOhlcv(symbol, "1m", start, now);

            if (candleData != null && !candleData.isEmpty()) {
                logger.info("✅ Fetched {} candles for 1m timeframe", candleData.size());
                strategy.addCandleData(symbol, candleData);

                // Get EMA 200
                Double ema200 = strategy.getLastEma200(symbol);

                if (ema200 != null) {
                    logger.info("📈 EMA 200 (1m, 500 candles): ${}", String.format("%.2f", ema200));

                    // Get current price
                    Double currentPrice = deltaApiClient.getCurrentMarkPrice(symbol);

                    if (currentPrice != null) {
                        logger.info("💰 Current ETH Price: ${}", String.format("%.2f", currentPrice));
                        logger.info("📊 EMA 200: ${}", String.format("%.2f", ema200));
                        double distance = ((currentPrice - ema200) / ema200) * 100;
                        logger.info("📏 Distance: {:.2f}%", distance);

                        if (currentPrice > ema200) {
                            logger.info("🎯 Trend: 📈 ABOVE");
                            logger.info("✅ BULLISH: Looking for BUY signals");
                            logger.info("🚀 READY FOR TRADING: Bot will look for BUY signals!");
                        } else {
                            logger.info("🎯 Trend: 📉 BELOW");
                            logger.info("❌ BEARISH: Looking for SELL signals");
                            logger.info("🛑 NOT READY FOR TRADING: Waiting for BULLISH trend.");
                        }
                    } else {
                        logger.warn("❌ Could not fetch current ETH price.");
                    }
                } else {
                    logger.warn("❌ Could not calculate EMA 200 for ETH.");
                }
            } else {
                logger.warn("❌ No candle data fetched for ETH.");
            }
        } catch (Exception e) {
            logger.error("An error occurred during ETH 500 candles test: {}", e.getMessage(), e);
        }
        logger.info("\n📊 ETH ANALYSIS:");
        logger.info("• Timeframe: 1-minute candles");
        logger.info("• Data Period: {} minutes ({} hours)", 500, 500 / 60);
        logger.info("• EMA 200 Calculation: Based on last 200 candles");
        logger.info("• Trend Detection: Price vs EMA 200");
        logger.info("\n✅ ETH 500 Candles Test Complete!\n");
    }
}
