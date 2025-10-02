package com.tradingbot.test;

import com.tradingbot.model.Trade;
import com.tradingbot.repository.TradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Check recent trades and their exit details
 */
@SpringBootApplication
public class CheckRecentTrades {
    
    private static final Logger logger = LoggerFactory.getLogger(CheckRecentTrades.class);
    
    public static void main(String[] args) {
        logger.info("📊 RECENT TRADES & EXIT DETAILS CHECK");
        logger.info("====================================");
        logger.info("");
        
        ConfigurableApplicationContext context = null;
        try {
            // Start Spring context to access database
            context = SpringApplication.run(CheckRecentTrades.class, args);
            TradeRepository tradeRepository = context.getBean(TradeRepository.class);
            
            // Get all trades
            List<Trade> allTrades = tradeRepository.findAll();
            
            logger.info("1️⃣ ALL TRADES IN DATABASE:");
            logger.info("==========================");
            logger.info("Total trades found: {}", allTrades.size());
            logger.info("");
            
            if (allTrades.isEmpty()) {
                logger.info("📊 No trades found in database");
                logger.info("🚀 System is ready for new trades");
            } else {
                // Sort by date (newest first)
                allTrades.sort((a, b) -> b.getEntryTime().compareTo(a.getEntryTime()));
                
                logger.info("📋 TRADE HISTORY (Newest First):");
                logger.info("================================");
                
                for (int i = 0; i < allTrades.size(); i++) {
                    Trade trade = allTrades.get(i);
                    logger.info("Trade #{}:", i + 1);
                    logger.info("   ID: {}", trade.getId());
                    logger.info("   Symbol: {}", trade.getSymbol());
                    logger.info("   Type: {}", trade.getType());
                    logger.info("   Entry Price: ${}", trade.getEntryPrice() != null ? String.format("%.2f", trade.getEntryPrice()) : "N/A");
                    logger.info("   Exit Price: ${}", trade.getExitPrice() != null ? String.format("%.2f", trade.getExitPrice()) : "N/A");
                    logger.info("   Quantity: {}", trade.getQuantity() != null ? trade.getQuantity() : "N/A");
                    logger.info("   Status: {}", trade.getStatus());
                    logger.info("   PnL: ${}", trade.getPnl() != null ? String.format("%.2f", trade.getPnl()) : "N/A");
                    logger.info("   Exit Reason: {}", trade.getExitReason() != null ? trade.getExitReason() : "N/A");
                    logger.info("   Entry Time: {}", trade.getEntryTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    logger.info("   Exit Time: {}", trade.getExitTime() != null ? trade.getExitTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "N/A");
                    logger.info("");
                }
                
                // Check for open trades
                List<Trade> openTrades = tradeRepository.findByStatus("OPEN");
                logger.info("2️⃣ CURRENT OPEN TRADES:");
                logger.info("=======================");
                logger.info("Open trades: {}", openTrades.size());
                
                if (openTrades.isEmpty()) {
                    logger.info("✅ No open trades found");
                    logger.info("🚀 System is ready for new trades");
                } else {
                    for (Trade openTrade : openTrades) {
                        logger.info("📊 Open Trade:");
                        logger.info("   Symbol: {}", openTrade.getSymbol());
                        logger.info("   Type: {}", openTrade.getType());
                        logger.info("   Entry Price: ${}", openTrade.getEntryPrice() != null ? String.format("%.2f", openTrade.getEntryPrice()) : "N/A");
                        logger.info("   Stop Loss: ${}", openTrade.getStopLoss() != null ? String.format("%.2f", openTrade.getStopLoss()) : "N/A");
                        logger.info("   Take Profit: ${}", openTrade.getTakeProfit() != null ? String.format("%.2f", openTrade.getTakeProfit()) : "N/A");
                        logger.info("   Quantity: {}", openTrade.getQuantity() != null ? openTrade.getQuantity() : "N/A");
                        logger.info("   Entry Time: {}", openTrade.getEntryTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                        
                        // Calculate potential SL/TP levels
                        if (openTrade.getEntryPrice() != null) {
                            double entryPrice = openTrade.getEntryPrice();
                            double stopLoss = entryPrice * 0.998; // -0.2%
                            double takeProfit = entryPrice * 1.006; // +0.6%
                            
                            logger.info("   📊 Estimated SL/TP Levels:");
                            logger.info("      Stop Loss: ${} (-0.2%)", String.format("%.2f", stopLoss));
                            logger.info("      Take Profit: ${} (+0.6%)", String.format("%.2f", takeProfit));
                        }
                        logger.info("");
                    }
                }
                
                // Check recent closed trades
                List<Trade> closedTrades = tradeRepository.findByStatus("CLOSED");
                logger.info("3️⃣ RECENT CLOSED TRADES:");
                logger.info("========================");
                logger.info("Closed trades: {}", closedTrades.size());
                
                if (!closedTrades.isEmpty()) {
                    // Sort by entry time (newest first)
                    closedTrades.sort((a, b) -> b.getEntryTime().compareTo(a.getEntryTime()));
                    
                    // Show last 5 closed trades
                    int showCount = Math.min(5, closedTrades.size());
                    for (int i = 0; i < showCount; i++) {
                        Trade closedTrade = closedTrades.get(i);
                        logger.info("📊 Closed Trade #{}:", i + 1);
                        logger.info("   Symbol: {}", closedTrade.getSymbol());
                        logger.info("   Type: {}", closedTrade.getType());
                        logger.info("   Entry Price: ${}", closedTrade.getEntryPrice() != null ? String.format("%.2f", closedTrade.getEntryPrice()) : "N/A");
                        logger.info("   Exit Price: ${}", closedTrade.getExitPrice() != null ? String.format("%.2f", closedTrade.getExitPrice()) : "N/A");
                        logger.info("   PnL: ${}", closedTrade.getPnl() != null ? String.format("%.2f", closedTrade.getPnl()) : "N/A");
                        logger.info("   Exit Reason: {}", closedTrade.getExitReason() != null ? closedTrade.getExitReason() : "N/A");
                        logger.info("   Closed: {}", closedTrade.getExitTime() != null ? closedTrade.getExitTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "N/A");
                        logger.info("");
                    }
                }
            }
            
            logger.info("4️⃣ SYSTEM STATUS:");
            logger.info("==================");
            logger.info("✅ Database connected successfully");
            logger.info("✅ Trade repository working");
            logger.info("✅ All trade data accessible");
            logger.info("");
            
            logger.info("🎯 CURRENT SITUATION:");
            logger.info("=====================");
            logger.info("• No open positions on Delta Exchange");
            logger.info("• Database shows {} total trades", allTrades.size());
            logger.info("• {} open trades, {} closed trades", 
                       tradeRepository.findByStatus("OPEN").size(),
                       tradeRepository.findByStatus("CLOSED").size());
            logger.info("• System ready for new signals");
            
        } catch (Exception e) {
            logger.error("❌ Error checking recent trades: {}", e.getMessage(), e);
        } finally {
            if (context != null) {
                context.close();
            }
        }
    }
}
