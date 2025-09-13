package com.tradingbot.service;

import com.tradingbot.model.Trade;
import com.tradingbot.model.CryptoStrategy2;
import com.tradingbot.repository.TradeRepository;
import com.tradingbot.repository.CryptoStrategy2Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Startup Recovery Service
 * 
 * Handles recovery of existing open trades when application restarts
 * Ensures continuity of position monitoring and management
 */
@Service
public class StartupRecoveryService {

    @Autowired
    private TradeRepository tradeRepository;
    
    @Autowired
    private CryptoStrategy2Repository strategy2Repository;
    
    @Autowired
    private TelegramNotificationService telegramService;
    
    @Autowired
    private CryptoPriceService priceService;

    /**
     * Recover and resume all existing open trades on startup
     */
    public void recoverExistingTrades() {
        System.out.println("🔄 RECOVERING EXISTING TRADES ON STARTUP");
        System.out.println("=" + "=".repeat(50));
        
        recoverStrategy1Trades();
        recoverStrategy2Trades();
        
        System.out.println("✅ Trade recovery completed!");
    }
    
    /**
     * Recover Strategy 1 open trades
     */
    private void recoverStrategy1Trades() {
        List<Trade> openTrades = tradeRepository.findByStatus("OPEN");
        
        if (openTrades.isEmpty()) {
            System.out.println("📋 No open Strategy 1 trades found");
            return;
        }
        
        System.out.println("🎯 Found " + openTrades.size() + " open Strategy 1 trades:");
        
        for (Trade trade : openTrades) {
            System.out.println("• " + trade.getSymbol() + " " + trade.getType() + 
                             " @ $" + trade.getEntryPrice() + 
                             " (Entry: " + trade.getEntryTime() + ")");
        }
        
        // Send recovery notification
        String recoveryMessage = createStrategy1RecoveryMessage(openTrades);
        telegramService.sendTelegramMessage(recoveryMessage);
    }
    
    /**
     * Recover Strategy 2 open trades
     */
    private void recoverStrategy2Trades() {
        List<CryptoStrategy2> openTrades = strategy2Repository.findByStatus("OPEN");
        
        if (openTrades.isEmpty()) {
            System.out.println("📋 No open Strategy 2 trades found");
            return;
        }
        
        System.out.println("⚡ Found " + openTrades.size() + " open Strategy 2 trades:");
        
        for (CryptoStrategy2 trade : openTrades) {
            System.out.println("• " + trade.getSymbol() + " " + trade.getTradeType() + 
                             " @ $" + trade.getEntryPrice() + 
                             " (Entry: " + trade.getEntryTime() + ")");
        }
        
        // Send recovery notification
        String recoveryMessage = createStrategy2RecoveryMessage(openTrades);
        telegramService.sendTelegramMessage(recoveryMessage);
    }
    
    /**
     * Create Strategy 1 recovery message
     */
    private String createStrategy1RecoveryMessage(List<Trade> openTrades) {
        StringBuilder message = new StringBuilder();
        message.append("🔄 *Strategy 1 Trade Recovery*\n\n");
        message.append("📊 *Found ").append(openTrades.size()).append(" open trades:*\n");
        
        for (Trade trade : openTrades) {
            message.append("• ").append(trade.getSymbol()).append(" ")
                   .append(trade.getType()).append(" @ $")
                   .append(String.format("%.2f", trade.getEntryPrice())).append("\n");
            message.append("  🛡️ SL: $").append(String.format("%.2f", trade.getStopLoss())).append("\n");
            message.append("  🎯 TP: $").append(String.format("%.2f", trade.getTakeProfit())).append("\n");
            message.append("  📅 Entry: ").append(trade.getEntryTime()).append("\n\n");
        }
        
        message.append("✅ *All positions will be monitored for SL/TP hits*");
        return message.toString();
    }
    
    /**
     * Create Strategy 2 recovery message
     */
    private String createStrategy2RecoveryMessage(List<CryptoStrategy2> openTrades) {
        StringBuilder message = new StringBuilder();
        message.append("⚡ *Strategy 2 Trade Recovery*\n\n");
        message.append("📊 *Found ").append(openTrades.size()).append(" open trades:*\n");
        
        for (CryptoStrategy2 trade : openTrades) {
            message.append("• ").append(trade.getSymbol()).append(" ")
                   .append(trade.getTradeType()).append(" @ $")
                   .append(String.format("%.2f", trade.getEntryPrice())).append("\n");
            message.append("  🛡️ SL: $").append(String.format("%.2f", trade.getStopLoss())).append("\n");
            message.append("  🎯 TP: $").append(String.format("%.2f", trade.getTakeProfit())).append("\n");
            message.append("  🔥 Leverage: ").append(trade.getLeverage()).append("x\n");
            message.append("  📅 Entry: ").append(trade.getEntryTime()).append("\n\n");
        }
        
        message.append("✅ *All positions will be monitored for SL/TP hits*");
        return message.toString();
    }
    
    /**
     * Get summary of all open trades
     */
    public String getOpenTradesSummary() {
        List<Trade> strategy1Trades = tradeRepository.findByStatus("OPEN");
        List<CryptoStrategy2> strategy2Trades = strategy2Repository.findByStatus("OPEN");
        
        StringBuilder summary = new StringBuilder();
        summary.append("📊 *OPEN TRADES SUMMARY*\n\n");
        
        summary.append("🎯 *Strategy 1 Trades:* ").append(strategy1Trades.size()).append("\n");
        for (Trade trade : strategy1Trades) {
            summary.append("• ").append(trade.getSymbol()).append(" ")
                   .append(trade.getType()).append(" @ $")
                   .append(String.format("%.2f", trade.getEntryPrice())).append("\n");
        }
        
        summary.append("\n⚡ *Strategy 2 Trades:* ").append(strategy2Trades.size()).append("\n");
        for (CryptoStrategy2 trade : strategy2Trades) {
            summary.append("• ").append(trade.getSymbol()).append(" ")
                   .append(trade.getTradeType()).append(" @ $")
                   .append(String.format("%.2f", trade.getEntryPrice())).append("\n");
        }
        
        return summary.toString();
    }
    
    /**
     * Validate existing trades (check if they're still valid)
     */
    public void validateExistingTrades() {
        System.out.println("🔍 VALIDATING EXISTING TRADES");
        
        // Check Strategy 1 trades
        List<Trade> strategy1Trades = tradeRepository.findByStatus("OPEN");
        for (Trade trade : strategy1Trades) {
            // Check if trade is too old (e.g., more than 30 days)
            if (trade.getEntryTime().isBefore(LocalDateTime.now().minusDays(30))) {
                System.out.println("⚠️ Old trade found: " + trade.getSymbol() + " from " + trade.getEntryTime());
                // Could implement auto-close logic here
            }
        }
        
        // Check Strategy 2 trades
        List<CryptoStrategy2> strategy2Trades = strategy2Repository.findByStatus("OPEN");
        for (CryptoStrategy2 trade : strategy2Trades) {
            // Check if trade is too old (e.g., more than 7 days for high-frequency strategy)
            if (trade.getEntryTime().isBefore(LocalDateTime.now().minusDays(7))) {
                System.out.println("⚠️ Old Strategy 2 trade found: " + trade.getSymbol() + " from " + trade.getEntryTime());
                // Could implement auto-close logic here
            }
        }
        
        System.out.println("✅ Trade validation completed");
    }
}
