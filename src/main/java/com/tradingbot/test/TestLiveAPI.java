package com.tradingbot.test;

import com.tradingbot.service.DeltaApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class TestLiveAPI {

    private static final Logger logger = LoggerFactory.getLogger(TestLiveAPI.class);

    public static void main(String[] args) {
        logger.info("🧪 Testing Live API Connection");
        logger.info("=============================");

        DeltaApiClient deltaApiClient = new DeltaApiClient();

        try {
            // Check if API is configured
            if (!deltaApiClient.isConfigured()) {
                logger.error("❌ API credentials not configured!");
                return;
            }

            logger.info("🔑 Using LIVE API credentials");
            logger.info("🌐 Base URL: https://api.delta.exchange");
            logger.info("");

            // Test 1: Get current BTC price
            logger.info("📊 Test 1: Getting current BTC price...");
            Double btcPrice = deltaApiClient.getCurrentMarkPrice("BTCUSD");
            if (btcPrice != null) {
                logger.info("✅ BTC Price: ${}", String.format("%.2f", btcPrice));
            } else {
                logger.error("❌ Failed to get BTC price");
            }

            // Test 2: Get current ETH price
            logger.info("📊 Test 2: Getting current ETH price...");
            Double ethPrice = deltaApiClient.getCurrentMarkPrice("ETHUSD");
            if (ethPrice != null) {
                logger.info("✅ ETH Price: ${}", String.format("%.2f", ethPrice));
            } else {
                logger.error("❌ Failed to get ETH price");
            }

            // Test 3: Try different balance endpoint
            logger.info("💰 Test 3: Trying balance endpoint...");
            Map<String, Object> balanceInfo = deltaApiClient.getAccountBalance();
            if (balanceInfo != null) {
                logger.info("✅ Balance retrieved successfully!");
                logger.info("📊 Balance: {}", balanceInfo);
            } else {
                logger.warn("⚠️ Balance endpoint not working, but market data is working");
            }

        } catch (Exception e) {
            logger.error("❌ Error testing live API: {}", e.getMessage(), e);
        }

        logger.info("");
        logger.info("✅ Live API Test Complete!");
    }
}
