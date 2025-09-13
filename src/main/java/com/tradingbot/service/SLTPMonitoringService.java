package com.tradingbot.service;

import com.tradingbot.model.Trade;
import com.tradingbot.repository.TradeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * SL/TP Monitoring Service
 * Continuously monitors open positions for stop loss and take profit hits
 */
@Service
public class SLTPMonitoringService {

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private PositionManagementService positionService;

    @Autowired
    private CryptoPriceService priceService;

    @Autowired
    private TelegramNotificationService telegramService;

    private final ScheduledExecutorService scheduler;
    private boolean isMonitoring = false;
    private int monitoringCycle = 0;

    public SLTPMonitoringService() {
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    /**
     * Start SL/TP monitoring
     */
    public void startMonitoring() {
        if (isMonitoring) {
            System.out.println("⚠️ SL/TP monitoring is already running!");
            return;
        }

        System.out.println("🛡️ Starting SL/TP Monitoring (every 50 seconds)");
        
        // No redundant startup message - main bot already sends comprehensive startup message

        isMonitoring = true;
        monitoringCycle = 0;

        // Start monitoring every 50 seconds for faster SL/TP detection
        scheduler.scheduleAtFixedRate(this::monitorPositions, 0, 50, TimeUnit.SECONDS);
    }

    /**
     * Stop SL/TP monitoring
     */
    public void stopMonitoring() {
        if (!isMonitoring) {
            System.out.println("⚠️ SL/TP monitoring is not running!");
            return;
        }

        System.out.println("🛑 Stopping SL/TP Monitoring...");
        scheduler.shutdown();
        isMonitoring = false;

        String stopMessage = """
            🛑 *SL/TP Monitoring Stopped*
            
            📊 *Session Summary:*
            • Total cycles: %d
            • Monitoring duration: %d minutes
            • Status: Stopped by user
            
            🎯 *Position monitoring stopped*
            """.formatted(monitoringCycle, monitoringCycle * 2);

        telegramService.sendTelegramMessage(stopMessage);
    }

    /**
     * Main monitoring loop
     */
    private void monitorPositions() {
        if (!isMonitoring) return;

        monitoringCycle++;
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        System.out.println("🛡️ SL/TP Monitoring Cycle #" + monitoringCycle + " - " + timestamp);
        System.out.println("-".repeat(60));

        try {
            // Get all open positions
            List<Trade> openTrades = positionService.getOpenPositions();

            if (openTrades.isEmpty()) {
                System.out.println("ℹ️ No open positions to monitor");
                System.out.println();
                return;
            }

            System.out.println("📊 Monitoring " + openTrades.size() + " open positions...");

            // Check each position
            for (Trade trade : openTrades) {
                monitorTrade(trade, timestamp);
            }

            // Send periodic status update every 30 cycles (1 hour)
            if (monitoringCycle % 30 == 0) {
                sendMonitoringStatusUpdate(openTrades);
            }

        } catch (Exception e) {
            System.err.println("❌ Error in SL/TP monitoring cycle: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("✅ SL/TP monitoring cycle completed");
        System.out.println();
    }

    /**
     * Monitor a specific trade for SL/TP hits
     */
    private void monitorTrade(Trade trade, String timestamp) {
        try {
            System.out.println("🔍 Monitoring " + trade.getSymbol() + " " + trade.getType() + 
                             " (ID: " + trade.getId() + ")");

            // Get current price
            Double currentPrice = getCurrentPrice(trade.getSymbol());
            if (currentPrice == null) {
                System.out.println("❌ Could not fetch price for " + trade.getSymbol());
                return;
            }

            System.out.println("💰 Current: $" + String.format("%.2f", currentPrice) + 
                             " | Entry: $" + String.format("%.2f", trade.getEntryPrice()) + 
                             " | SL: $" + String.format("%.2f", trade.getStopLoss()) + 
                             " | TP: $" + String.format("%.2f", trade.getTakeProfit()));

            // Check for stop loss hit
            if (trade.isStopLossHit(currentPrice)) {
                System.out.println("🔴 STOP LOSS HIT for " + trade.getSymbol() + "!");
                
                String exitReason = "Stop Loss Hit - Price: $" + String.format("%.2f", currentPrice);
                positionService.closePosition(trade.getId(), currentPrice, exitReason);
                
                // Send immediate alert
                sendStopLossAlert(trade, currentPrice, timestamp);
                return;
            }

            // Check for take profit hit
            if (trade.isTakeProfitHit(currentPrice)) {
                System.out.println("🟢 TAKE PROFIT HIT for " + trade.getSymbol() + "!");
                
                String exitReason = "Take Profit Hit - Price: $" + String.format("%.2f", currentPrice);
                positionService.closePosition(trade.getId(), currentPrice, exitReason);
                
                // Send immediate alert
                sendTakeProfitAlert(trade, currentPrice, timestamp);
                return;
            }

            // Check if close to SL/TP levels (warnings)
            checkProximityWarnings(trade, currentPrice);

        } catch (Exception e) {
            System.err.println("❌ Error monitoring trade " + trade.getId() + ": " + e.getMessage());
        }
    }

    /**
     * Get current price for a symbol
     */
    private Double getCurrentPrice(String symbol) {
        try {
            if (symbol.equals("BTCUSD")) {
                return priceService.getBitcoinPrice();
            } else if (symbol.equals("ETHUSD")) {
                return priceService.getEthereumPrice();
            } else if (symbol.equals("SOLUSD")) {
                return priceService.getSolanaPrice();
            }
        } catch (Exception e) {
            System.err.println("❌ Error fetching price for " + symbol + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Check proximity warnings for SL/TP levels
     */
    private void checkProximityWarnings(Trade trade, Double currentPrice) {
        double slDistance = Math.abs(currentPrice - trade.getStopLoss());
        double tpDistance = Math.abs(currentPrice - trade.getTakeProfit());
        
        // Warning if within 1% of SL/TP levels
        double warningThreshold = currentPrice * 0.01; // 1%
        
        if (slDistance <= warningThreshold) {
            System.out.println("⚠️ WARNING: " + trade.getSymbol() + " close to Stop Loss!");
        }
        
        if (tpDistance <= warningThreshold) {
            System.out.println("⚠️ WARNING: " + trade.getSymbol() + " close to Take Profit!");
        }
    }

    /**
     * Send stop loss alert
     */
    private void sendStopLossAlert(Trade trade, Double exitPrice, String timestamp) {
        String alertMessage = """
            🔴 *STOP LOSS HIT - IMMEDIATE ALERT*
            
            📊 *Symbol:* %s
            📈 *Type:* %s
            💰 *Entry Price:* $%.2f
            🔴 *Stop Loss:* $%.2f
            💸 *Exit Price:* $%.2f
            
            📉 *P&L:* $%.2f (LOSS)
            
            ⏰ *Time:* %s
            
            🚨 *Position automatically closed!*
            """.formatted(
                trade.getSymbol(),
                trade.getType(),
                trade.getEntryPrice(),
                trade.getStopLoss(),
                exitPrice,
                trade.calculatePnL(exitPrice),
                timestamp
            );

        telegramService.sendTelegramMessage(alertMessage);
    }

    /**
     * Send take profit alert
     */
    private void sendTakeProfitAlert(Trade trade, Double exitPrice, String timestamp) {
        String alertMessage = """
            🟢 *TAKE PROFIT HIT - PROFIT ALERT*
            
            📊 *Symbol:* %s
            📈 *Type:* %s
            💰 *Entry Price:* $%.2f
            🎯 *Take Profit:* $%.2f
            💸 *Exit Price:* $%.2f
            
            💰 *P&L:* $%.2f (PROFIT)
            
            ⏰ *Time:* %s
            
            🎉 *Position automatically closed with profit!*
            """.formatted(
                trade.getSymbol(),
                trade.getType(),
                trade.getEntryPrice(),
                trade.getTakeProfit(),
                exitPrice,
                trade.calculatePnL(exitPrice),
                timestamp
            );

        telegramService.sendTelegramMessage(alertMessage);
    }

    /**
     * Send monitoring status update
     */
    private void sendMonitoringStatusUpdate(List<Trade> openTrades) {
        try {
            String statusMessage = """
                📊 *SL/TP Monitoring Status Update*
                
                ⏰ *Time:* %s
                🔄 *Cycles Completed:* %d
                ⏱️ *Monitoring Duration:* %d minutes
                
                📈 *Open Positions:* %d
                
                🛡️ *Monitoring:*
                • Stop Loss levels
                • Take Profit levels
                • Real-time price checks
                
                ✅ *System Status:* Active
                """.formatted(
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    monitoringCycle,
                    monitoringCycle * 2,
                    openTrades.size()
                );

            telegramService.sendTelegramMessage(statusMessage);

        } catch (Exception e) {
            System.err.println("❌ Error sending monitoring status update: " + e.getMessage());
        }
    }

    /**
     * Check if monitoring is active
     */
    public boolean isMonitoring() {
        return isMonitoring;
    }

    /**
     * Get monitoring statistics
     */
    public String getMonitoringStats() {
        return String.format("SL/TP Monitoring Cycle: %d, Duration: %d minutes", 
                           monitoringCycle, monitoringCycle * 2);
    }
}
