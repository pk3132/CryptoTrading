package com.tradingbot.test;

import com.tradingbot.service.BalanceCheck;
import com.tradingbot.service.DeltaOrderService;
import com.tradingbot.service.DeltaApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calculate minimum balance required for trading BTC and ETH
 */
public class MinimumBalanceCalculator {
    
    private static final Logger logger = LoggerFactory.getLogger(MinimumBalanceCalculator.class);
    
    public static void main(String[] args) {
        logger.info("💰 MINIMUM BALANCE CALCULATOR");
        logger.info("============================");
        
        try {
            // Get current balance
            BalanceCheck balanceCheck = new BalanceCheck();
            double currentBalance = balanceCheck.getAvailableBalanceAsDouble();
            
            // Get current market prices
            DeltaApiClient deltaApiClient = new DeltaApiClient();
            Double btcPrice = deltaApiClient.getCurrentMarkPrice("BTCUSD");
            Double ethPrice = deltaApiClient.getCurrentMarkPrice("ETHUSD");
            
            logger.info("📊 Current Account Status:");
            logger.info("   Available Balance: ${}", currentBalance);
            logger.info("   BTC Current Price: ${}", btcPrice);
            logger.info("   ETH Current Price: ${}", ethPrice);
            logger.info("");
            
            // Calculate minimum requirements
            calculateMinimumRequirements(btcPrice, ethPrice, currentBalance);
            
        } catch (Exception e) {
            logger.error("❌ Error calculating minimum balance: {}", e.getMessage(), e);
        }
    }
    
    private static void calculateMinimumRequirements(Double btcPrice, Double ethPrice, double currentBalance) {
        logger.info("🎯 MINIMUM BALANCE REQUIREMENTS");
        logger.info("===============================");
        logger.info("");
        
        // Contract sizes
        double btcContractSize = DeltaOrderService.getContractSize("BTCUSD"); // 0.001 BTC
        double ethContractSize = DeltaOrderService.getContractSize("ETHUSD"); // 0.01 ETH
        
        logger.info("📋 Contract Sizes:");
        logger.info("   BTC Contract Size: {} BTC", btcContractSize);
        logger.info("   ETH Contract Size: {} ETH", ethContractSize);
        logger.info("");
        
        if (btcPrice != null && ethPrice != null) {
            // Calculate minimum order values
            double btcMinOrderValue = btcPrice * btcContractSize;
            double ethMinOrderValue = ethPrice * ethContractSize;
            
            logger.info("💵 MINIMUM ORDER VALUES (1 Contract Each):");
            logger.info("   BTC Minimum Order: ${} ({} BTC × ${})", 
                       String.format("%.2f", btcMinOrderValue), btcContractSize, btcPrice);
            logger.info("   ETH Minimum Order: ${} ({} ETH × ${})", 
                       String.format("%.2f", ethMinOrderValue), ethContractSize, ethPrice);
            logger.info("");
            
            // Calculate with leverage
            int leverage = 10; // 10x leverage used in the system
            double btcMarginRequired = btcMinOrderValue / leverage;
            double ethMarginRequired = ethMinOrderValue / leverage;
            
            logger.info("⚡ WITH 10X LEVERAGE (Margin Required):");
            logger.info("   BTC Margin Required: ${} (${} ÷ {})", 
                       String.format("%.2f", btcMarginRequired), String.format("%.2f", btcMinOrderValue), leverage);
            logger.info("   ETH Margin Required: ${} (${} ÷ {})", 
                       String.format("%.2f", ethMarginRequired), String.format("%.2f", ethMinOrderValue), leverage);
            logger.info("");
            
            // Safety recommendations
            double safetyMultiplier = 2.0; // 2x safety margin
            double btcRecommendedBalance = btcMarginRequired * safetyMultiplier;
            double ethRecommendedBalance = ethMarginRequired * safetyMultiplier;
            
            logger.info("🛡️ RECOMMENDED BALANCES (2x Safety Margin):");
            logger.info("   BTC Recommended: ${}", String.format("%.2f", btcRecommendedBalance));
            logger.info("   ETH Recommended: ${}", String.format("%.2f", ethRecommendedBalance));
            logger.info("   Combined Recommended: ${}", String.format("%.2f", btcRecommendedBalance + ethRecommendedBalance));
            logger.info("");
            
            // Current balance analysis
            logger.info("📊 CURRENT BALANCE ANALYSIS:");
            logger.info("   Current Balance: ${}", String.format("%.2f", currentBalance));
            logger.info("");
            
            // BTC Analysis
            if (currentBalance >= btcMarginRequired) {
                logger.info("✅ BTC Trading: SUFFICIENT BALANCE");
                logger.info("   Can trade BTC with current balance");
            } else {
                double btcNeeded = btcMarginRequired - currentBalance;
                logger.info("❌ BTC Trading: INSUFFICIENT BALANCE");
                logger.info("   Need additional: ${}", String.format("%.2f", btcNeeded));
                logger.info("   Minimum to deposit: ${}", String.format("%.2f", btcMarginRequired));
            }
            logger.info("");
            
            // ETH Analysis
            if (currentBalance >= ethMarginRequired) {
                logger.info("✅ ETH Trading: SUFFICIENT BALANCE");
                logger.info("   Can trade ETH with current balance");
            } else {
                double ethNeeded = ethMarginRequired - currentBalance;
                logger.info("❌ ETH Trading: INSUFFICIENT BALANCE");
                logger.info("   Need additional: ${}", String.format("%.2f", ethNeeded));
                logger.info("   Minimum to deposit: ${}", String.format("%.2f", ethMarginRequired));
            }
            logger.info("");
            
            // Summary
            logger.info("🎯 SUMMARY & RECOMMENDATIONS:");
            logger.info("==============================");
            
            double minForEither = Math.min(btcMarginRequired, ethMarginRequired);
            double minForBoth = Math.max(btcMarginRequired, ethMarginRequired);
            
            logger.info("💰 Minimum Balance Requirements:");
            logger.info("   • Trade BTC only: ${}", String.format("%.2f", btcMarginRequired));
            logger.info("   • Trade ETH only: ${}", String.format("%.2f", ethMarginRequired));
            logger.info("   • Trade both: ${}", String.format("%.2f", minForBoth));
            logger.info("");
            
            logger.info("💡 Recommended Deposits:");
            logger.info("   • Conservative: ${} (for BTC only)", String.format("%.2f", btcRecommendedBalance));
            logger.info("   • Moderate: ${} (for ETH only)", String.format("%.2f", ethRecommendedBalance));
            logger.info("   • Aggressive: ${} (for both BTC & ETH)", String.format("%.2f", btcRecommendedBalance + ethRecommendedBalance));
            logger.info("");
            
            logger.info("⚠️  Important Notes:");
            logger.info("   • These are MINIMUM requirements with 10x leverage");
            logger.info("   • Higher balance = better risk management");
            logger.info("   • Consider stop-loss requirements (0.5% of position)");
            logger.info("   • Account for trading fees and slippage");
            
        } else {
            logger.error("❌ Could not fetch current market prices");
            logger.info("💡 Use approximate values:");
            logger.info("   • BTC: ~$95,000 × 0.001 = ~$95 minimum");
            logger.info("   • ETH: ~$3,200 × 0.01 = ~$32 minimum");
        }
    }
}
