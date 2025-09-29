package com.tradingbot.command;

import com.tradingbot.service.LiveTradeExecutor;
import com.tradingbot.service.BalanceCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Command-line interface for executing live trades from database
 */
@Component
public class LiveTradeCommand implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(LiveTradeCommand.class);
    
    @Autowired
    private LiveTradeExecutor liveTradeExecutor;
    
    @Autowired
    private BalanceCheck balanceCheck;
    
    @Override
    public void run(String... args) throws Exception {
        // Check if this is a live trade execution request
        if (args.length > 0 && "live-trade".equals(args[0])) {
            executeLiveTrades(args);
        }
    }
    
    private void executeLiveTrades(String[] args) {
        logger.info("🚀 LIVE TRADE EXECUTION COMMAND");
        logger.info("===============================");
        
        try {
            // First, check current balance
            logger.info("💰 Checking account balance...");
            balanceCheck.printBalance();
            logger.info("");
            
            // Show pending trades summary
            String summary = liveTradeExecutor.getPendingTradesSummary();
            logger.info(summary);
            
            // Check if user wants to execute all trades or specific symbol
            if (args.length > 1) {
                String symbol = args[1].toUpperCase();
                logger.info("🎯 Executing trades for symbol: {}", symbol);
                liveTradeExecutor.executeTradesForSymbol(symbol);
            } else {
                logger.info("🎯 Executing all pending trades...");
                liveTradeExecutor.executePendingTrades();
            }
            
            logger.info("✅ Live trade execution command completed!");
            
        } catch (Exception e) {
            logger.error("❌ Error in live trade execution command: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Main method for standalone execution
     */
    public static void main(String[] args) {
        // This would be called when running the application with live-trade argument
        logger.info("🔧 Live Trade Command - Use 'live-trade' argument to execute trades");
        logger.info("📋 Usage:");
        logger.info("   java -jar tradingbot.jar live-trade                    # Execute all pending trades");
        logger.info("   java -jar tradingbot.jar live-trade BTCUSD            # Execute trades for BTCUSD only");
        logger.info("   java -jar tradingbot.jar live-trade ETHUSD            # Execute trades for ETHUSD only");
    }
}
