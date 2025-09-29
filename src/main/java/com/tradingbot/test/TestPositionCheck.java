package com.tradingbot.test;

import com.tradingbot.service.PositionChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test position checking functionality
 */
public class TestPositionCheck {
    
    private static final Logger logger = LoggerFactory.getLogger(TestPositionCheck.class);
    
    public static void main(String[] args) {
        logger.info("🔍 TESTING POSITION CHECK FUNCTIONALITY");
        logger.info("=======================================");
        
        PositionChecker positionChecker = new PositionChecker();
        
        try {
            // Test checking positions for BTC and ETH
            logger.info("📊 Checking positions for BTCUSD...");
            boolean hasBtcPosition = positionChecker.hasOpenPosition("BTCUSD");
            logger.info("BTCUSD Position Status: {}", hasBtcPosition ? "OPEN" : "NONE");
            logger.info("");
            
            logger.info("📊 Checking positions for ETHUSD...");
            boolean hasEthPosition = positionChecker.hasOpenPosition("ETHUSD");
            logger.info("ETHUSD Position Status: {}", hasEthPosition ? "OPEN" : "NONE");
            logger.info("");
            
            logger.info("📋 SUMMARY:");
            logger.info("===========");
            logger.info("BTCUSD: {}", hasBtcPosition ? "⚠️ OPEN POSITION" : "✅ NO POSITION");
            logger.info("ETHUSD: {}", hasEthPosition ? "⚠️ OPEN POSITION" : "✅ NO POSITION");
            logger.info("");
            
            if (!hasBtcPosition && !hasEthPosition) {
                logger.info("🎉 PERFECT! No open positions found.");
                logger.info("✅ Ready to place new trades without duplicates.");
            } else {
                logger.info("⚠️ Open positions detected - new trades will be blocked to prevent duplicates.");
            }
            
        } catch (Exception e) {
            logger.error("❌ Error testing position check: {}", e.getMessage(), e);
        }
    }
}
