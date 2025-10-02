package com.tradingbot.test;

import com.tradingbot.service.BalanceCheck;
import com.tradingbot.service.LiveTradeExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.ComponentScan;

/**
 * Execute pending trades with minimum quantity
 */
/**
 * DISABLED: This is a testing tool, should not run with main app
 * To use: Uncomment @SpringBootApplication and @ComponentScan, then run separately
 */
// @SpringBootApplication
// @ComponentScan(basePackages = "com.tradingbot")
public class ExecutePendingTrades { // implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(ExecutePendingTrades.class);
    
    @Autowired
    private LiveTradeExecutor liveTradeExecutor;
    
    @Autowired
    private BalanceCheck balanceCheck;
    
    /**
     * DISABLED: This method won't run because class doesn't implement CommandLineRunner
     * To enable: Uncomment @SpringBootApplication and implements CommandLineRunner
     */
    public void runManually(String... args) throws Exception {
        logger.info("🚀 EXECUTING PENDING TRADES WITH MINIMUM QUANTITY");
        logger.info("================================================");
        
        try {
            // Check current balance first
            logger.info("💰 Checking account balance...");
            balanceCheck.printBalance();
            logger.info("");
            
            // Show pending trades summary
            String summary = liveTradeExecutor.getPendingTradesSummary();
            logger.info("📋 Pending Trades Status:");
            logger.info(summary);
            
            // Execute all pending trades
            logger.info("🎯 Executing all pending trades...");
            liveTradeExecutor.executePendingTrades();
            
            logger.info("✅ Trade execution process completed!");
            
        } catch (Exception e) {
            logger.error("❌ Error executing trades: {}", e.getMessage(), e);
        }
    }
    
    public static void main(String[] args) {
        SpringApplication.run(ExecutePendingTrades.class, args);
    }
}
