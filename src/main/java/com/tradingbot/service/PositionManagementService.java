package com.tradingbot.service;

import com.tradingbot.model.Trade;
import com.tradingbot.repository.TradeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Position Management Service
 * Manages trading positions and trade lifecycle
 */
@Service
public class PositionManagementService {

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private TelegramNotificationService telegramService;

    /**
     * Open a new trade position
     */
    public Trade openPosition(String symbol, String type, Double entryPrice, 
                             Double stopLoss, Double takeProfit, String reason) {
        
        // Calculate quantity based on risk management (example: 1% risk)
        Double quantity = calculatePositionSize(entryPrice, stopLoss);
        
        Trade trade = new Trade(symbol, type, entryPrice, stopLoss, takeProfit, quantity, reason);
        Trade savedTrade = tradeRepository.save(trade);
        
        System.out.println("✅ New position opened: " + savedTrade);
        
        // Send Telegram notification for new position
        if (type.equals("BUY")) {
            telegramService.sendBuySignal(symbol, entryPrice, stopLoss, takeProfit, reason);
        } else {
            telegramService.sendSellSignal(symbol, entryPrice, stopLoss, takeProfit, reason);
        }
        
        return savedTrade;
    }

    /**
     * Close a trade position
     */
    public Trade closePosition(Long tradeId, Double exitPrice, String exitReason) {
        Optional<Trade> tradeOpt = tradeRepository.findById(tradeId);
        
        if (tradeOpt.isPresent()) {
            Trade trade = tradeOpt.get();
            trade.closeTrade(exitPrice, exitReason);
            Trade savedTrade = tradeRepository.save(trade);
            
            System.out.println("✅ Position closed: " + savedTrade);
            
            // Send Telegram notification for trade closure
            String signalType = trade.getType().toString();
            Double pnl = trade.getPnl();
            telegramService.sendExitNotification(
                trade.getSymbol(), 
                signalType, 
                trade.getEntryPrice(), 
                exitPrice, 
                exitReason, 
                pnl
            );
            
            return savedTrade;
        }
        
        throw new RuntimeException("Trade not found with ID: " + tradeId);
    }

    /**
     * Get all open positions
     */
    public List<Trade> getOpenPositions() {
        return tradeRepository.findByStatus("OPEN");
    }

    /**
     * Get open positions for a specific symbol
     */
    public List<Trade> getOpenPositions(String symbol) {
        return tradeRepository.findBySymbolAndStatus(symbol, "OPEN");
    }

    /**
     * Get all trades for monitoring
     */
    public List<Trade> getTradesForMonitoring() {
        return tradeRepository.findTradesForMonitoring();
    }

    /**
     * Get trade statistics
     */
    public TradeStatistics getTradeStatistics() {
        long totalTrades = tradeRepository.count();
        long openTrades = tradeRepository.countByStatus("OPEN");
        long closedTrades = tradeRepository.count() - openTrades;
        
        List<Trade> profitableTrades = tradeRepository.findProfitableTrades();
        List<Trade> losingTrades = tradeRepository.findLosingTrades();
        
        long winningTrades = profitableTrades.size();
        long losingTradesCount = losingTrades.size();
        
        double winRate = closedTrades > 0 ? (double) winningTrades / closedTrades * 100 : 0;
        
        Optional<Double> totalPnLOpt = tradeRepository.calculateTotalPnL();
        double totalPnL = totalPnLOpt.orElse(0.0);
        
        return new TradeStatistics(
            totalTrades, openTrades, closedTrades, 
            winningTrades, losingTradesCount, winRate, totalPnL
        );
    }

    /**
     * Get trade statistics for a specific symbol
     */
    public TradeStatistics getTradeStatistics(String symbol) {
        List<Trade> symbolTrades = tradeRepository.findBySymbol(symbol);
        
        long totalTrades = symbolTrades.size();
        long openTrades = symbolTrades.stream().mapToLong(t -> "OPEN".equals(t.getStatus()) ? 1 : 0).sum();
        long closedTrades = totalTrades - openTrades;
        
        long winningTrades = symbolTrades.stream()
            .filter(t -> "CLOSED".equals(t.getStatus()) && t.getPnl() != null && t.getPnl() > 0)
            .count();
        
        long losingTrades = symbolTrades.stream()
            .filter(t -> "CLOSED".equals(t.getStatus()) && t.getPnl() != null && t.getPnl() < 0)
            .count();
        
        double winRate = closedTrades > 0 ? (double) winningTrades / closedTrades * 100 : 0;
        
        double totalPnL = symbolTrades.stream()
            .filter(t -> t.getPnl() != null)
            .mapToDouble(Trade::getPnl)
            .sum();
        
        return new TradeStatistics(
            totalTrades, openTrades, closedTrades, 
            winningTrades, losingTrades, winRate, totalPnL
        );
    }

    /**
     * Calculate position size based on risk management
     */
    private Double calculatePositionSize(Double entryPrice, Double stopLoss) {
        // Simple position sizing: 1% risk per trade
        // This is a placeholder - in real trading, this would be more sophisticated
        double riskAmount = 1000.0; // $1000 risk per trade
        double riskPerUnit = Math.abs(entryPrice - stopLoss);
        
        if (riskPerUnit > 0) {
            return riskAmount / riskPerUnit;
        }
        
        return 1.0; // Default quantity
    }

    /**
     * Check if we already have an open position for this symbol and type
     */
    public boolean hasOpenPosition(String symbol, String type) {
        List<Trade> openTrades = tradeRepository.findBySymbolAndStatus(symbol, "OPEN");
        return openTrades.stream().anyMatch(trade -> trade.getType().equals(type));
    }

    /**
     * Close all open positions (emergency close)
     */
    public void closeAllPositions(String reason) {
        List<Trade> openTrades = getOpenPositions();
        
        for (Trade trade : openTrades) {
            // Get current price for exit
            Double currentPrice = getCurrentPrice(trade.getSymbol());
            if (currentPrice != null) {
                closePosition(trade.getId(), currentPrice, reason);
            }
        }
    }

    /**
     * Get current price for a symbol (placeholder)
     */
    private Double getCurrentPrice(String symbol) {
        // This would integrate with your price service
        // For now, return null to indicate price unavailable
        return null;
    }

    /**
     * Trade Statistics Inner Class
     */
    public static class TradeStatistics {
        private final long totalTrades;
        private final long openTrades;
        private final long closedTrades;
        private final long winningTrades;
        private final long losingTrades;
        private final double winRate;
        private final double totalPnL;

        public TradeStatistics(long totalTrades, long openTrades, long closedTrades, 
                              long winningTrades, long losingTrades, double winRate, double totalPnL) {
            this.totalTrades = totalTrades;
            this.openTrades = openTrades;
            this.closedTrades = closedTrades;
            this.winningTrades = winningTrades;
            this.losingTrades = losingTrades;
            this.winRate = winRate;
            this.totalPnL = totalPnL;
        }

        // Getters
        public long getTotalTrades() { return totalTrades; }
        public long getOpenTrades() { return openTrades; }
        public long getClosedTrades() { return closedTrades; }
        public long getWinningTrades() { return winningTrades; }
        public long getLosingTrades() { return losingTrades; }
        public double getWinRate() { return winRate; }
        public double getTotalPnL() { return totalPnL; }

        @Override
        public String toString() {
            return String.format("TradeStats{total=%d, open=%d, closed=%d, wins=%d, losses=%d, winRate=%.1f%%, pnl=$%.2f}",
                    totalTrades, openTrades, closedTrades, winningTrades, losingTrades, winRate, totalPnL);
        }
    }
}
