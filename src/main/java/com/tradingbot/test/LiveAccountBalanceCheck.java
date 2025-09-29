package com.tradingbot.test;

import com.tradingbot.service.DeltaApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class LiveAccountBalanceCheck {

    private static final Logger logger = LoggerFactory.getLogger(LiveAccountBalanceCheck.class);

    public static void main(String[] args) {
        logger.info("💰 Live Account Balance Check");
        logger.info("=============================");

        DeltaApiClient deltaApiClient = new DeltaApiClient();

        try {
            // Check if API is configured
            if (!deltaApiClient.isConfigured()) {
                logger.error("❌ API credentials not configured!");
                return;
            }

            logger.info("🔑 Using LIVE API credentials");
            logger.info("🌐 Base URL: https://api.india.delta.exchange");
            logger.info("");

            // Get account balance
            Map<String, Object> balanceInfo = deltaApiClient.getAccountBalance();

            if (balanceInfo != null) {
                logger.info("✅ Live Account Balance Retrieved Successfully!");
                logger.info("📊 Balance Details:");
                
                if (balanceInfo.containsKey("available_balance")) {
                    logger.info("💰 Available Balance: ${}", balanceInfo.get("available_balance"));
                }
                if (balanceInfo.containsKey("total_balance")) {
                    logger.info("💎 Total Balance: ${}", balanceInfo.get("total_balance"));
                }
                if (balanceInfo.containsKey("currency")) {
                    logger.info("💱 Currency: {}", balanceInfo.get("currency"));
                }
                
                logger.info("");
                logger.info("🎯 Live Account Status: ✅ ACTIVE");
                logger.info("🚀 Ready for LIVE TRADING!");
                
            } else {
                logger.error("❌ Failed to retrieve live account balance");
            }

        } catch (Exception e) {
            logger.error("❌ Error checking live account balance: {}", e.getMessage(), e);
        }

        logger.info("");
        logger.info("✅ Live Account Balance Check Complete!");
    }
}
