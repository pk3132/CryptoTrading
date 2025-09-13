package com.tradingbot.service;

import com.tradingbot.model.CryptoStrategy2;
import com.tradingbot.repository.CryptoStrategy2Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Strategy 2 SL/TP Monitoring Service
 * 
 * Monitors Strategy 2 positions for Stop Loss and Take Profit hits
 * Checks every 50 seconds for faster SL/TP detection
 */
@Service
public class Strategy2SLTPMonitoringService {

    @Autowired
    private CryptoStrategy2Repository strategy2Repository;
    
    @Autowired
    private Strategy2PositionService positionService;
    
    @Autowired
    private CryptoPriceService priceService;
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private boolean isMonitoring = false;
    private int monitoringCycle = 0;

    /**
     * Start Strategy 2 SL/TP monitoring
     */
    public void startMonitoring() {
        if (isMonitoring) {
            System.out.println("⚠️ Strategy 2 SL/TP monitoring is already running!");
            return;
        }
        
        System.out.println("🛡️ Starting Strategy 2 SL/TP Monitoring (every 30 seconds)");
        System.out.println("🎯 Monitoring Strategy 2 positions for Stop Loss and Take Profit hits");
        
        isMonitoring = true;
        monitoringCycle = 0;
        
        // Start monitoring every 30 seconds for ultra-fast SL/TP detection
        scheduler.scheduleAtFixedRate(this::monitorStrategy2Positions, 0, 30, TimeUnit.SECONDS);
    }

    /**
     * Stop Strategy 2 SL/TP monitoring
     */
    public void stopMonitoring() {
        if (!isMonitoring) {
            System.out.println("⚠️ Strategy 2 SL/TP monitoring is not running!");
            return;
        }
        
        System.out.println("🛑 Stopping Strategy 2 SL/TP monitoring...");
        isMonitoring = false;
        scheduler.shutdown();
    }

    /**
     * Monitor Strategy 2 positions for SL/TP hits
     */
    private void monitorStrategy2Positions() {
        monitoringCycle++;
        String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        System.out.println("🔍 Strategy 2 SL/TP Check #" + monitoringCycle + " - " + timestamp);
        
        try {
            // Get all open Strategy 2 positions
            List<CryptoStrategy2> openPositions = strategy2Repository.findByStatus("OPEN");
            
            if (openPositions.isEmpty()) {
                System.out.println("📋 No open Strategy 2 positions to monitor");
                return;
            }
            
            System.out.println("📊 Monitoring " + openPositions.size() + " open Strategy 2 positions");
            
            for (CryptoStrategy2 position : openPositions) {
                monitorPosition(position, timestamp);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error in Strategy 2 SL/TP monitoring: " + e.getMessage());
        }
        
        System.out.println("✅ Strategy 2 SL/TP monitoring cycle completed\n");
    }

    /**
     * Monitor individual position for SL/TP hits
     */
    private void monitorPosition(CryptoStrategy2 position, String timestamp) {
        try {
            // Get current price for the symbol
            Double currentPrice = getCurrentPrice(position.getSymbol());
            
            if (currentPrice == null) {
                System.out.println("⚠️ Could not get current price for " + position.getSymbol());
                return;
            }
            
            System.out.println("📊 Checking " + position.getSymbol() + " " + position.getTradeType() + 
                             " - Current: $" + String.format("%.2f", currentPrice) + 
                             ", SL: $" + String.format("%.2f", position.getStopLoss()) + 
                             ", TP: $" + String.format("%.2f", position.getTakeProfit()));
            
            // Check for Stop Loss hit
            if (position.isStopLossHit(currentPrice)) {
                System.out.println("🛑 STOP LOSS HIT: " + position.getSymbol() + " " + position.getTradeType());
                positionService.closeStrategy2Position(position.getId(), currentPrice, "Stop Loss Hit");
                return;
            }
            
            // Check for Take Profit hit
            if (position.isTakeProfitHit(currentPrice)) {
                System.out.println("🎯 TAKE PROFIT HIT: " + position.getSymbol() + " " + position.getTradeType());
                positionService.closeStrategy2Position(position.getId(), currentPrice, "Take Profit Hit");
                return;
            }
            
            // Calculate current P&L for monitoring
            double currentPnL = calculateCurrentPnL(position, currentPrice);
            System.out.println("📈 Current P&L: $" + String.format("%.2f", currentPnL));
            
        } catch (Exception e) {
            System.err.println("❌ Error monitoring position " + position.getId() + ": " + e.getMessage());
        }
    }

    /**
     * Get current price for a symbol
     */
    private Double getCurrentPrice(String symbol) {
        try {
            switch (symbol) {
                case "BTCUSD":
                    return priceService.getBitcoinPrice();
                case "ETHUSD":
                    return priceService.getEthereumPrice();
                case "SOLUSD":
                    return priceService.getSolanaPrice();
                default:
                    System.out.println("⚠️ Unknown symbol: " + symbol);
                    return null;
            }
        } catch (Exception e) {
            System.err.println("❌ Error getting current price for " + symbol + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Calculate current P&L for a position
     */
    private double calculateCurrentPnL(CryptoStrategy2 position, Double currentPrice) {
        if (position.getTradeType().equals("BUY")) {
            return (currentPrice - position.getEntryPrice()) * position.getQuantity() * position.getLeverage();
        } else { // SELL
            return (position.getEntryPrice() - currentPrice) * position.getQuantity() * position.getLeverage();
        }
    }

    /**
     * Get Strategy 2 SL/TP monitoring status
     */
    public String getStrategy2SLTPStatus() {
        if (!isMonitoring) {
            return "❌ Strategy 2 SL/TP monitoring is not active";
        }
        
        List<CryptoStrategy2> openPositions = strategy2Repository.findByStatus("OPEN");
        
        StringBuilder status = new StringBuilder();
        status.append("✅ Strategy 2 SL/TP Monitoring Active\n");
        status.append("⏰ Check Interval: 30 seconds\n");
        status.append("📊 Open Positions: ").append(openPositions.size()).append("\n");
        status.append("🔄 Monitoring Cycles: ").append(monitoringCycle).append("\n\n");
        
        if (!openPositions.isEmpty()) {
            status.append("📋 Open Positions:\n");
            for (CryptoStrategy2 position : openPositions) {
                status.append("• ").append(position.getSymbol()).append(" ")
                      .append(position.getTradeType()).append(" - Entry: $")
                      .append(String.format("%.2f", position.getEntryPrice()))
                      .append("\n");
            }
        } else {
            status.append("📋 No open positions to monitor");
        }
        
        return status.toString();
    }

    /**
     * Manual SL/TP check for a specific position
     */
    public void manualSLTPCheck(Long positionId) {
        try {
            CryptoStrategy2 position = strategy2Repository.findById(positionId).orElse(null);
            
            if (position == null) {
                System.out.println("❌ Strategy 2 position not found: " + positionId);
                return;
            }
            
            if (!position.getStatus().equals("OPEN")) {
                System.out.println("⚠️ Position is not open: " + positionId);
                return;
            }
            
            String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            System.out.println("🔍 Manual SL/TP check for position " + positionId + " - " + timestamp);
            
            monitorPosition(position, timestamp);
            
        } catch (Exception e) {
            System.err.println("❌ Error in manual SL/TP check: " + e.getMessage());
        }
    }

    /**
     * Get monitoring statistics
     */
    public String getMonitoringStatistics() {
        if (!isMonitoring) {
            return "❌ Strategy 2 SL/TP monitoring is not active";
        }
        
        List<CryptoStrategy2> openPositions = strategy2Repository.findByStatus("OPEN");
        List<CryptoStrategy2> recentTrades = strategy2Repository.findRecentTrades().stream()
                .limit(10)
                .toList();
        
        StringBuilder stats = new StringBuilder();
        stats.append("📊 Strategy 2 SL/TP Monitoring Statistics\n");
        stats.append("=").append("=".repeat(40)).append("\n");
        stats.append("🔄 Monitoring Cycles: ").append(monitoringCycle).append("\n");
        stats.append("📊 Open Positions: ").append(openPositions.size()).append("\n");
        stats.append("⏰ Check Interval: 30 seconds\n");
        stats.append("📅 Last Check: ").append(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
        
        if (!recentTrades.isEmpty()) {
            stats.append("📋 Recent Strategy 2 Trades:\n");
            for (CryptoStrategy2 trade : recentTrades) {
                stats.append("• ").append(trade.getSymbol()).append(" ")
                      .append(trade.getTradeType()).append(" - ")
                      .append(trade.getStatus()).append(" - ")
                      .append("P&L: $").append(trade.getPnl() != null ? String.format("%.2f", trade.getPnl()) : "0.00")
                      .append("\n");
            }
        }
        
        return stats.toString();
    }
}
