package com.tradingbot.example;

import com.tradingbot.service.BalanceCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example usage of BalanceCheck class
 * Shows different ways to use the balance checking functionality
 */
public class BalanceCheckExample {
    
    private static final Logger logger = LoggerFactory.getLogger(BalanceCheckExample.class);
    
    public static void main(String[] args) {
        BalanceCheck balanceCheck = new BalanceCheck();
        
        logger.info("🔍 BalanceCheck Class Usage Examples");
        logger.info("====================================");
        
        // Example 1: Simple balance check
        logger.info("\n📊 Example 1: Simple Balance Check");
        balanceCheck.printBalance();
        
        // Example 2: Get balance as structured data
        logger.info("\n📊 Example 2: Get Balance as Data");
        var balanceData = balanceCheck.getCurrentBalance();
        if (balanceData != null) {
            logger.info("✅ Balance data retrieved successfully");
            logger.info("📋 Balance data: {}", balanceData);
        }
        
        // Example 3: Get available balance for calculations
        logger.info("\n📊 Example 3: Get Available Balance for Calculations");
        double availableBalance = balanceCheck.getAvailableBalanceAsDouble();
        logger.info("💰 Available Balance: ${}", availableBalance);
        
        // Example 4: Check if sufficient balance for trading
        logger.info("\n📊 Example 4: Check Sufficient Balance");
        double requiredAmount = 0.01; // $0.01 required
        boolean hasEnough = balanceCheck.hasSufficientBalance(requiredAmount);
        logger.info("🔍 Required Amount: ${}", requiredAmount);
        logger.info("✅ Has Sufficient Balance: {}", hasEnough);
        
        // Example 5: Get formatted summary
        logger.info("\n📊 Example 5: Get Formatted Summary");
        String summary = balanceCheck.getBalanceSummary();
        logger.info("📄 Formatted Summary:\n{}", summary);
        
        logger.info("\n✅ BalanceCheck Examples Complete!");
    }
    
    /**
     * Example method showing how to use BalanceCheck in trading logic
     */
    public static void exampleTradingLogic() {
        BalanceCheck balanceCheck = new BalanceCheck();
        
        // Check balance before placing a trade
        double tradeAmount = 0.005; // $0.005 trade
        
        if (balanceCheck.hasSufficientBalance(tradeAmount)) {
            logger.info("✅ Sufficient balance available for trade of ${}", tradeAmount);
            // Proceed with trade logic here
        } else {
            logger.warn("⚠️ Insufficient balance for trade of ${}", tradeAmount);
            logger.warn("💡 Consider depositing more funds");
        }
    }
}
