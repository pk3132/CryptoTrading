package com.tradingbot.example;

import com.tradingbot.service.PositionChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example demonstrating how to use the PositionChecker service
 * 
 * This class shows how to:
 * 1. Check for existing positions before placing orders
 * 2. Prevent duplicate orders
 * 3. Get position details
 * 4. Print position summaries
 */
public class PositionCheckerExample {
    
    private static final Logger logger = LoggerFactory.getLogger(PositionCheckerExample.class);
    
    public static void main(String[] args) {
        logger.info("📋 POSITION CHECKER USAGE EXAMPLE");
        logger.info("==================================");
        
        // Create PositionChecker instance
        PositionChecker positionChecker = new PositionChecker();
        
        // Example 1: Check if specific symbols have open positions
        logger.info("1️⃣ CHECKING SPECIFIC SYMBOLS:");
        logger.info("==============================");
        
        checkSymbolPosition(positionChecker, "BTCUSD");
        checkSymbolPosition(positionChecker, "ETHUSD");
        
        logger.info("");
        
        // Example 2: Print all open positions
        logger.info("2️⃣ ALL OPEN POSITIONS:");
        logger.info("======================");
        positionChecker.printOpenPositions();
        
        logger.info("");
        
        // Example 3: Get detailed position info
        logger.info("3️⃣ DETAILED POSITION INFO:");
        logger.info("===========================");
        getDetailedPositionInfo(positionChecker, "ETHUSD");
        
        logger.info("");
        
        // Example 4: Simulate trade decision logic
        logger.info("4️⃣ TRADE DECISION LOGIC:");
        logger.info("=========================");
        simulateTradeDecision(positionChecker, "BTCUSD", "BUY");
        simulateTradeDecision(positionChecker, "ETHUSD", "BUY");
        simulateTradeDecision(positionChecker, "ETHUSD", "SELL");
    }
    
    /**
     * Check if a symbol has an open position
     */
    private static void checkSymbolPosition(PositionChecker checker, String symbol) {
        boolean hasPosition = checker.hasOpenPosition(symbol);
        logger.info("{} Position: {}", symbol, hasPosition ? "⚠️ OPEN" : "✅ NONE");
    }
    
    /**
     * Get detailed position information
     */
    private static void getDetailedPositionInfo(PositionChecker checker, String symbol) {
        PositionChecker.PositionInfo position = checker.getPositionForSymbol(symbol);
        if (position != null) {
            logger.info("📊 {} Position Details:", symbol);
            logger.info("   Side: {}", position.side);
            logger.info("   Size: {} contracts", position.size);
            logger.info("   Entry Price: ${}", position.entryPrice);
            logger.info("   Mark Price: ${}", position.markPrice);
            logger.info("   Unrealized PnL: ${}", position.unrealizedPnl);
            logger.info("   Product ID: {}", position.productId);
        } else {
            logger.info("📊 {} Position: No position found", symbol);
        }
    }
    
    /**
     * Simulate trade decision based on position check
     */
    private static void simulateTradeDecision(PositionChecker checker, String symbol, String side) {
        logger.info("🤔 Trade Decision: {} {} {}", side, symbol, "Signal");
        logger.info("   Checking for existing positions...");
        
        boolean hasPosition = checker.hasOpenPosition(symbol);
        
        if (hasPosition) {
            if (side.equals("BUY")) {
                logger.info("   ❌ DECISION: BLOCK - Duplicate BUY order prevented");
                logger.info("   📝 Reason: Existing position found, avoiding duplicate");
            } else {
                logger.info("   ✅ DECISION: ALLOW - SELL order to close position");
                logger.info("   📝 Reason: SELL order can close existing position");
            }
        } else {
            logger.info("   ✅ DECISION: ALLOW - New {} order", side);
            logger.info("   📝 Reason: No existing position found");
        }
        logger.info("");
    }
}
