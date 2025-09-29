package com.tradingbot.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Trading Analysis for $20 Deposit
 */
public class TradingWith20Dollars {
    
    private static final Logger logger = LoggerFactory.getLogger(TradingWith20Dollars.class);
    
    public static void main(String[] args) {
        logger.info("💰 TRADING ANALYSIS WITH $20 DEPOSIT");
        logger.info("====================================");
        
        // Current market data (from previous calculation)
        double btcPrice = 110758.42;
        double ethPrice = 4054.98;
        double btcContractSize = 0.001;
        double ethContractSize = 0.01;
        int leverage = 10;
        
        double depositAmount = 20.00;
        
        logger.info("📊 Your Deposit: ${}", depositAmount);
        logger.info("📈 Current Market Prices:");
        logger.info("   BTC: ${}", btcPrice);
        logger.info("   ETH: ${}", ethPrice);
        logger.info("");
        
        // Calculate what you can trade
        calculateTradingOptions(depositAmount, btcPrice, ethPrice, btcContractSize, ethContractSize, leverage);
        
        // Trading strategy recommendations
        logger.info("🎯 TRADING STRATEGY RECOMMENDATIONS:");
        logger.info("=====================================");
        logger.info("");
        
        logger.info("✅ RECOMMENDED: ETH Trading");
        logger.info("   • ETH margin required: $4.05");
        logger.info("   • Your available: ${}", depositAmount);
        logger.info("   • Safety margin: {}x", String.format("%.1f", depositAmount / 4.05));
        logger.info("   • Can trade: {} ETH contracts", (int)(depositAmount / 4.05));
        logger.info("");
        
        logger.info("⚠️  POSSIBLE: BTC Trading");
        logger.info("   • BTC margin required: $11.08");
        logger.info("   • Your available: ${}", depositAmount);
        logger.info("   • Safety margin: {}x", String.format("%.1f", depositAmount / 11.08));
        logger.info("   • Can trade: {} BTC contracts", (int)(depositAmount / 11.08));
        logger.info("");
        
        logger.info("🚀 OPTIMAL STRATEGY:");
        logger.info("   • Start with ETH trading (safer)");
        logger.info("   • Use 1 ETH contract = $4.05 margin");
        logger.info("   • Keep $15.95 as buffer for losses/fees");
        logger.info("   • If profitable, consider BTC trading later");
        logger.info("");
        
        logger.info("📋 RISK MANAGEMENT:");
        logger.info("   • Position size: 1 contract maximum");
        logger.info("   • Stop loss: 0.5% (automatic)");
        logger.info("   • Take profit: 1.0% (automatic)");
        logger.info("   • Risk per trade: ~$2-4");
        logger.info("   • Buffer for losses: $15.95");
        logger.info("");
        
        logger.info("💡 SUCCESS TIPS:");
        logger.info("   • Start with ETH (lower risk)");
        logger.info("   • Don't risk more than 20% per trade");
        logger.info("   • Let the bot's SL/TP work automatically");
        logger.info("   • Monitor performance for 1-2 weeks");
        logger.info("   • Consider adding more funds if profitable");
        logger.info("");
        
        logger.info("🎉 CONCLUSION:");
        logger.info("   ✅ $20 is PERFECT for ETH trading!");
        logger.info("   ✅ You have 5x safety margin");
        logger.info("   ✅ Can handle 4-5 losing trades");
        logger.info("   ✅ Good starting point for live trading");
        logger.info("");
        logger.info("🚀 Ready to start live trading with ETH!");
    }
    
    private static void calculateTradingOptions(double deposit, double btcPrice, double ethPrice, 
                                              double btcContractSize, double ethContractSize, int leverage) {
        
        // Calculate minimum margins
        double btcMinMargin = (btcPrice * btcContractSize) / leverage;
        double ethMinMargin = (ethPrice * ethContractSize) / leverage;
        
        logger.info("⚡ MARGIN CALCULATIONS:");
        logger.info("   BTC 1 contract margin: ${}", String.format("%.2f", btcMinMargin));
        logger.info("   ETH 1 contract margin: ${}", String.format("%.2f", ethMinMargin));
        logger.info("");
        
        // Calculate how many contracts you can trade
        int maxBtcContracts = (int)(deposit / btcMinMargin);
        int maxEthContracts = (int)(deposit / ethMinMargin);
        
        logger.info("📊 MAXIMUM CONTRACTS YOU CAN TRADE:");
        logger.info("   BTC contracts: {} (uses ${})", maxBtcContracts, String.format("%.2f", maxBtcContracts * btcMinMargin));
        logger.info("   ETH contracts: {} (uses ${})", maxEthContracts, String.format("%.2f", maxEthContracts * ethMinMargin));
        logger.info("");
        
        // Safety analysis
        double btcSafetyMargin = deposit / btcMinMargin;
        double ethSafetyMargin = deposit / ethMinMargin;
        
        logger.info("🛡️ SAFETY MARGIN ANALYSIS:");
        logger.info("   BTC safety margin: {}x", String.format("%.1f", btcSafetyMargin));
        logger.info("   ETH safety margin: {}x", String.format("%.1f", ethSafetyMargin));
        logger.info("");
        
        if (ethSafetyMargin >= 3.0) {
            logger.info("✅ EXCELLENT: ETH has {}x safety margin - very safe!", String.format("%.1f", ethSafetyMargin));
        } else if (ethSafetyMargin >= 2.0) {
            logger.info("✅ GOOD: ETH has {}x safety margin - safe", String.format("%.1f", ethSafetyMargin));
        } else if (ethSafetyMargin >= 1.5) {
            logger.info("⚠️  MODERATE: ETH has {}x safety margin - acceptable", String.format("%.1f", ethSafetyMargin));
        } else {
            logger.info("❌ RISKY: ETH has {}x safety margin - not recommended", String.format("%.1f", ethSafetyMargin));
        }
        
        if (btcSafetyMargin >= 2.0) {
            logger.info("✅ GOOD: BTC has {}x safety margin - safe", String.format("%.1f", btcSafetyMargin));
        } else if (btcSafetyMargin >= 1.5) {
            logger.info("⚠️  MODERATE: BTC has {}x safety margin - acceptable", String.format("%.1f", btcSafetyMargin));
        } else {
            logger.info("❌ RISKY: BTC has {}x safety margin - not recommended", String.format("%.1f", btcSafetyMargin));
        }
        
        logger.info("");
    }
}
