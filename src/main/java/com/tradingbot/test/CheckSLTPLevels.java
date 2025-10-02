package com.tradingbot.test;

import com.tradingbot.service.PositionChecker;
import com.tradingbot.service.DeltaApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Check current Stop Loss and Take Profit levels
 */
public class CheckSLTPLevels {
    
    private static final Logger logger = LoggerFactory.getLogger(CheckSLTPLevels.class);
    
    public static void main(String[] args) {
        logger.info("🎯 STOP LOSS & TAKE PROFIT LEVELS CHECK");
        logger.info("======================================");
        logger.info("");
        
        try {
            // Check current positions
            logger.info("1️⃣ CURRENT POSITIONS:");
            logger.info("====================");
            PositionChecker positionChecker = new PositionChecker();
            
            boolean hasBtcPosition = positionChecker.hasOpenPosition("BTCUSD");
            boolean hasEthPosition = positionChecker.hasOpenPosition("ETHUSD");
            
            logger.info("BTCUSD Position: {}", hasBtcPosition ? "⚠️ OPEN" : "✅ NONE");
            logger.info("ETHUSD Position: {}", hasEthPosition ? "⚠️ OPEN" : "✅ NONE");
            logger.info("");
            
            // Get current prices
            logger.info("2️⃣ CURRENT MARKET PRICES:");
            logger.info("=========================");
            DeltaApiClient deltaApiClient = new DeltaApiClient();
            
            Double btcPrice = deltaApiClient.getCurrentMarkPrice("BTCUSD");
            Double ethPrice = deltaApiClient.getCurrentMarkPrice("ETHUSD");
            
            logger.info("BTCUSD Current Price: ${}", btcPrice != null ? String.format("%.2f", btcPrice) : "N/A");
            logger.info("ETHUSD Current Price: ${}", ethPrice != null ? String.format("%.2f", ethPrice) : "N/A");
            logger.info("");
            
            // Check ETH position details
            if (hasEthPosition) {
                logger.info("3️⃣ ETH POSITION DETAILS:");
                logger.info("========================");
                PositionChecker.PositionInfo ethPosition = positionChecker.getPositionForSymbol("ETHUSD");
                
                if (ethPosition != null) {
                    logger.info("📊 ETH Position Information:");
                    logger.info("   Entry Price: ${}", String.format("%.2f", ethPosition.entryPrice));
                    logger.info("   Current Mark Price: ${}", String.format("%.2f", ethPrice));
                    logger.info("   Position Size: {} contracts", ethPosition.size);
                    logger.info("   Side: {}", ethPosition.side);
                    logger.info("   Product ID: {}", ethPosition.productId);
                    logger.info("");
                    
                    // Calculate potential SL/TP levels (based on standard 0.2% SL, 0.6% TP)
                    double entryPrice = ethPosition.entryPrice;
                    double slPct = 0.002; // 0.2%
                    double tpPct = 0.006; // 0.6%
                    
                    double stopLoss = entryPrice * (1 - slPct);
                    double takeProfit = entryPrice * (1 + tpPct);
                    
                    logger.info("4️⃣ ESTIMATED SL/TP LEVELS:");
                    logger.info("==========================");
                    logger.info("📊 Based on standard strategy settings:");
                    logger.info("   Entry Price: ${}", String.format("%.2f", entryPrice));
                    logger.info("   Stop Loss: ${} (-0.5%)", String.format("%.2f", stopLoss));
                    logger.info("   Take Profit: ${} (+1.0%)", String.format("%.2f", takeProfit));
                    logger.info("");
                    
                    // Current P&L calculation
                    if (ethPrice != null) {
                        double currentPnL = (ethPrice - entryPrice) * ethPosition.size;
                        double currentPnLPct = ((ethPrice - entryPrice) / entryPrice) * 100;
                        
                        logger.info("5️⃣ CURRENT P&L STATUS:");
                        logger.info("======================");
                        logger.info("📊 Current Position Analysis:");
                        logger.info("   Entry Price: ${}", String.format("%.2f", entryPrice));
                        logger.info("   Current Price: ${}", String.format("%.2f", ethPrice));
                        logger.info("   Current P&L: ${} ({})", 
                                   String.format("%.2f", currentPnL),
                                   String.format("%.2f%%", currentPnLPct));
                        logger.info("");
                        
                        // Distance to SL/TP
                        double slDistance = ethPrice - stopLoss;
                        double tpDistance = takeProfit - ethPrice;
                        double slDistancePct = (slDistance / ethPrice) * 100;
                        double tpDistancePct = (tpDistance / ethPrice) * 100;
                        
                        logger.info("6️⃣ DISTANCE TO EXIT LEVELS:");
                        logger.info("===========================");
                        logger.info("📊 Current Position vs Exit Levels:");
                        logger.info("   Distance to Stop Loss: ${} ({}%)", 
                                   String.format("%.2f", slDistance), 
                                   String.format("%.2f", slDistancePct));
                        logger.info("   Distance to Take Profit: ${} ({}%)", 
                                   String.format("%.2f", tpDistance), 
                                   String.format("%.2f", tpDistancePct));
                        logger.info("");
                        
                        // Risk assessment
                        logger.info("7️⃣ RISK ASSESSMENT:");
                        logger.info("===================");
                        if (currentPnL > 0) {
                            logger.info("✅ Position is PROFITABLE: +${}", String.format("%.2f", currentPnL));
                            logger.info("📈 Moving towards Take Profit level");
                        } else if (currentPnL < 0) {
                            logger.info("⚠️ Position is at LOSS: ${}", String.format("%.2f", currentPnL));
                            logger.info("📉 Moving towards Stop Loss level");
                        } else {
                            logger.info("➡️ Position is at BREAKEVEN: $0.00");
                        }
                        logger.info("");
                        
                        logger.info("8️⃣ EXIT SCENARIOS:");
                        logger.info("==================");
                        logger.info("🔴 Stop Loss Hit: Price drops to ${}", String.format("%.2f", stopLoss));
                        logger.info("   → Exits {} contracts at market price", (int)ethPosition.size);
                        logger.info("   → Estimated Loss: ${}", String.format("%.2f", (stopLoss - entryPrice) * ethPosition.size));
                        logger.info("");
                        logger.info("🟢 Take Profit Hit: Price rises to ${}", String.format("%.2f", takeProfit));
                        logger.info("   → Exits {} contracts at market price", (int)ethPosition.size);
                        logger.info("   → Estimated Profit: ${}", String.format("%.2f", (takeProfit - entryPrice) * ethPosition.size));
                        logger.info("");
                    }
                }
            } else {
                logger.info("3️⃣ NO OPEN POSITIONS:");
                logger.info("=====================");
                logger.info("✅ No ETH position found");
                logger.info("📊 No stop loss or take profit levels to display");
                logger.info("🚀 Ready for new trades when signals come");
            }
            
            // BTC status
            if (!hasBtcPosition) {
                logger.info("📊 BTC Status: Ready for new trades");
                logger.info("   No position → No SL/TP levels");
            }
            
            logger.info("");
            logger.info("🎯 SUMMARY:");
            logger.info("===========");
            logger.info("• Current ETH Price: ${}", ethPrice != null ? String.format("%.2f", ethPrice) : "N/A");
            if (hasEthPosition && ethPrice != null) {
                PositionChecker.PositionInfo ethPosition = positionChecker.getPositionForSymbol("ETHUSD");
                if (ethPosition != null) {
                    double entryPrice = ethPosition.entryPrice;
                    double stopLoss = entryPrice * 0.998; // -0.2%
                    double takeProfit = entryPrice * 1.006; // +0.6%
                    double currentPnL = (ethPrice - entryPrice) * ethPosition.size;
                    
                    logger.info("• ETH Entry: ${}", String.format("%.2f", entryPrice));
                    logger.info("• ETH Stop Loss: ${}", String.format("%.2f", stopLoss));
                    logger.info("• ETH Take Profit: ${}", String.format("%.2f", takeProfit));
                    logger.info("• ETH Current P&L: ${}", String.format("%.2f", currentPnL));
                    logger.info("• ETH Position Size: {} contracts", (int)ethPosition.size);
                }
            }
            
        } catch (Exception e) {
            logger.error("❌ Error checking SL/TP levels: {}", e.getMessage(), e);
        }
    }
}
