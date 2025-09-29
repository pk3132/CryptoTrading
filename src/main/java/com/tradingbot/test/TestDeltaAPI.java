package com.tradingbot.test;

import com.tradingbot.service.DeltaApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class TestDeltaAPI {

    private static final Logger logger = LoggerFactory.getLogger(TestDeltaAPI.class);

    public static void main(String[] args) {
        logger.info("🔍 Testing Delta Exchange API Connection");
        logger.info("=====================================");

        DeltaApiClient deltaApiClient = new DeltaApiClient();

        try {
            // Check if API is configured
            if (!deltaApiClient.isConfigured()) {
                logger.error("❌ API credentials not configured!");
                return;
            }

            logger.info("🔑 API credentials configured");
            logger.info("🌐 Base URL: https://api.india.delta.exchange");
            logger.info("");

            // Test 1: Get current positions
            logger.info("📊 Testing positions endpoint...");
            List<Map<String, Object>> positions = deltaApiClient.getPositions();
            
            if (positions != null) {
                logger.info("✅ Positions endpoint working!");
                logger.info("📋 Current positions: {}", positions.size());
                for (Map<String, Object> position : positions) {
                    logger.info("  Position: {}", position);
                }
            } else {
                logger.warn("⚠️ No positions or positions endpoint failed");
            }
            
            logger.info("");

            // Test 2: Try wallet balances with different endpoint
            logger.info("💰 Testing wallet balances...");
            Map<String, Object> balanceInfo = deltaApiClient.getAccountBalance();
            
            if (balanceInfo != null) {
                logger.info("✅ Balance endpoint working!");
                logger.info("📊 Balance info: {}", balanceInfo);
            } else {
                logger.warn("⚠️ Balance endpoint failed - trying alternative approach");
                
                // Try alternative balance endpoint
                logger.info("🔄 Trying alternative balance check...");
                testAlternativeBalanceCheck(deltaApiClient);
            }

        } catch (Exception e) {
            logger.error("❌ Error testing Delta API: {}", e.getMessage(), e);
        }

        logger.info("");
        logger.info("✅ Delta API Test Complete!");
    }
    
    private static void testAlternativeBalanceCheck(DeltaApiClient client) {
        try {
            // This would be where we implement alternative balance checking
            logger.info("🔍 Alternative balance check not implemented yet");
        } catch (Exception e) {
            logger.error("❌ Alternative balance check failed: {}", e.getMessage());
        }
    }
}
