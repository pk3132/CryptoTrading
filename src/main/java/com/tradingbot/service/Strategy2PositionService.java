package com.tradingbot.service;

import com.tradingbot.model.CryptoStrategy2;
import com.tradingbot.repository.CryptoStrategy2Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Strategy 2 Position Management Service
 * 
 * Manages Strategy 2 trades with 1-hour timeframe
 * Handles opening, closing, and tracking of Strategy 2 positions
 */
@Service
public class Strategy2PositionService {

    @Autowired
    private CryptoStrategy2Repository strategy2Repository;
    
    @Autowired
    private TelegramNotificationService telegramService;

    // Strategy 2 parameters
    private static final double LEVERAGE = 5.0;
    private static final double RISK_REWARD_RATIO = 2.0;
    private static final double RISK_PER_TRADE = 0.01; // 1% risk per trade
    private static final double INITIAL_CAPITAL = 10000.0; // For position sizing

    /**
     * Open a Strategy 2 position
     */
    public CryptoStrategy2 openStrategy2Position(String symbol, String tradeType, Double entryPrice, 
                                               Double stopLoss, Double takeProfit, String entryReason,
                                               Double ema9Value, Double ema20Value, String trendDirection,
                                               String marketCondition) {
        
        // Check if there's already an open position for this symbol
        if (hasOpenPosition(symbol)) {
            System.out.println("⚠️ Strategy 2: " + symbol + " already has an open position");
            return null;
        }

        // Calculate position size based on risk
        double riskAmount = INITIAL_CAPITAL * RISK_PER_TRADE;
        double riskPerUnit = Math.abs(entryPrice - stopLoss);
        double quantity = (riskAmount / riskPerUnit) * LEVERAGE;

        // Create new Strategy 2 trade
        CryptoStrategy2 trade = new CryptoStrategy2();
        trade.setSymbol(symbol);
        trade.setTradeType(tradeType);
        trade.setEntryPrice(entryPrice);
        trade.setStopLoss(stopLoss);
        trade.setTakeProfit(takeProfit);
        trade.setQuantity(quantity);
        trade.setLeverage(LEVERAGE);
        trade.setRiskRewardRatio(RISK_REWARD_RATIO);
        trade.setTimeframe("1h");
        trade.setStrategyName("Strategy 2 - 9/20 EMA Crossover");
        trade.setEntryReason(entryReason);
        trade.setStatus("OPEN");
        trade.setEntryTime(LocalDateTime.now());
        trade.setEma9Value(ema9Value);
        trade.setEma20Value(ema20Value);
        trade.setTrendDirection(trendDirection);
        trade.setMarketCondition(marketCondition);
        trade.setCreatedAt(LocalDateTime.now());

        // Save to database
        CryptoStrategy2 savedTrade = strategy2Repository.save(trade);

        // Send Telegram notification
        sendStrategy2EntryNotification(savedTrade);

        System.out.println("✅ Strategy 2 Position Opened: " + symbol + " " + tradeType + 
                         " at $" + String.format("%.2f", entryPrice));
        
        return savedTrade;
    }

    /**
     * Close a Strategy 2 position
     */
    public CryptoStrategy2 closeStrategy2Position(Long tradeId, Double exitPrice, String exitReason) {
        Optional<CryptoStrategy2> tradeOpt = strategy2Repository.findById(tradeId);
        
        if (tradeOpt.isEmpty()) {
            System.out.println("❌ Strategy 2 Trade not found: " + tradeId);
            return null;
        }

        CryptoStrategy2 trade = tradeOpt.get();
        
        if (!trade.getStatus().equals("OPEN")) {
            System.out.println("⚠️ Strategy 2 Trade is not open: " + tradeId);
            return null;
        }

        // Close the trade
        trade.closeTrade(exitPrice, exitReason);
        
        // Save to database
        CryptoStrategy2 savedTrade = strategy2Repository.save(trade);

        // Send Telegram notification
        sendStrategy2ExitNotification(savedTrade);

        System.out.println("✅ Strategy 2 Position Closed: " + trade.getSymbol() + " " + trade.getTradeType() + 
                         " at $" + String.format("%.2f", exitPrice) + 
                         " - P&L: $" + String.format("%.2f", trade.getPnl()));
        
        return savedTrade;
    }

    /**
     * Close position by symbol and trade type
     */
    public CryptoStrategy2 closeStrategy2Position(String symbol, String tradeType, Double exitPrice, String exitReason) {
        Optional<CryptoStrategy2> tradeOpt = strategy2Repository.findBySymbolAndTradeTypeAndStatus(symbol, tradeType, "OPEN");
        
        if (tradeOpt.isEmpty()) {
            System.out.println("❌ No open Strategy 2 position found for " + symbol + " " + tradeType);
            return null;
        }

        return closeStrategy2Position(tradeOpt.get().getId(), exitPrice, exitReason);
    }

    /**
     * Check if there's an open position for a symbol
     */
    public boolean hasOpenPosition(String symbol) {
        return strategy2Repository.existsBySymbolAndStatus(symbol, "OPEN");
    }

    /**
     * Get open positions
     */
    public List<CryptoStrategy2> getOpenPositions() {
        return strategy2Repository.findByStatus("OPEN");
    }

    /**
     * Get open positions for a specific symbol
     */
    public List<CryptoStrategy2> getOpenPositions(String symbol) {
        return strategy2Repository.findBySymbolAndStatus(symbol, "OPEN");
    }

    /**
     * Close all open positions
     */
    public void closeAllOpenPositions(String exitReason) {
        List<CryptoStrategy2> openTrades = getOpenPositions();
        
        for (CryptoStrategy2 trade : openTrades) {
            // Get current price (you would fetch this from your price service)
            Double currentPrice = getCurrentPrice(trade.getSymbol());
            if (currentPrice != null) {
                closeStrategy2Position(trade.getId(), currentPrice, exitReason);
            }
        }
    }

    /**
     * Get Strategy 2 performance summary
     */
    public String getStrategy2PerformanceSummary() {
        Object[] summary = strategy2Repository.getStrategyPerformanceSummary();
        
        if (summary == null || summary.length < 5) {
            return "No Strategy 2 trades found";
        }

        long totalTrades = ((Number) summary[0]).longValue();
        long winningTrades = ((Number) summary[1]).longValue();
        long losingTrades = ((Number) summary[2]).longValue();
        double totalPnL = ((Number) summary[3]).doubleValue();
        double averagePnL = ((Number) summary[4]).doubleValue();

        double winRate = totalTrades > 0 ? (double) winningTrades / totalTrades * 100 : 0;

        return String.format("""
            📊 Strategy 2 Performance Summary:
            
            🔄 Total Trades: %d
            ✅ Winning Trades: %d (%.1f%%)
            ❌ Losing Trades: %d (%.1f%%)
            💰 Total P&L: $%.2f
            📈 Average P&L: $%.2f
            🎯 Win Rate: %.1f%%
            """, totalTrades, winningTrades, winRate, losingTrades, 100-winRate, totalPnL, averagePnL);
    }

    /**
     * Get recent trades
     */
    public List<CryptoStrategy2> getRecentTrades(int limit) {
        return strategy2Repository.findRecentTrades().stream()
                .limit(limit)
                .toList();
    }

    /**
     * Send Strategy 2 entry notification
     */
    private void sendStrategy2EntryNotification(CryptoStrategy2 trade) {
        String message = String.format("""
            🚀 *Strategy 2 Entry Signal - 1H Timeframe*
            
            📊 *Symbol:* %s
            🎯 *Action:* %s
            💰 *Entry Price:* $%.2f
            🛡️ *Stop Loss:* $%.2f
            🎯 *Take Profit:* $%.2f
            ⚡ *Leverage:* %.0fx
            📈 *Risk-Reward:* 1:%.0f
            📊 *Quantity:* %.4f
            
            📈 *Market Analysis:*
            • EMA 9: $%.2f
            • EMA 20: $%.2f
            • Trend: %s
            • Market: %s
            
            💡 *Reason:* %s
            ⏰ *Time:* %s
            
            🎯 *Strategy 2: 9/20 EMA Crossover (1H)*
            """,
            trade.getSymbol(),
            trade.getTradeType(),
            trade.getEntryPrice(),
            trade.getStopLoss(),
            trade.getTakeProfit(),
            trade.getLeverage(),
            trade.getRiskRewardRatio(),
            trade.getQuantity(),
            trade.getEma9Value(),
            trade.getEma20Value(),
            trade.getTrendDirection(),
            trade.getMarketCondition(),
            trade.getEntryReason(),
            trade.getEntryTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        telegramService.sendTelegramMessage(message);
    }

    /**
     * Send Strategy 2 exit notification
     */
    private void sendStrategy2ExitNotification(CryptoStrategy2 trade) {
        String pnlEmoji = trade.getPnl() > 0 ? "✅" : "❌";
        
        String message = String.format("""
            %s *Strategy 2 Exit - 1H Timeframe*
            
            📊 *Symbol:* %s
            🎯 *Action:* %s
            💰 *Entry Price:* $%.2f
            💰 *Exit Price:* $%.2f
            %s *P&L:* $%.2f (%.2f%%)
            
            🛡️ *Exit Reason:* %s
            ⏰ *Duration:* %d minutes
            📅 *Entry Time:* %s
            📅 *Exit Time:* %s
            
            📈 *Trade Summary:*
            • Risk-Reward: 1:%.0f
            • Leverage: %.0fx
            • Timeframe: 1H
            
            %s Strategy 2 Trade Completed
            """,
            pnlEmoji,
            trade.getSymbol(),
            trade.getTradeType(),
            trade.getEntryPrice(),
            trade.getExitPrice(),
            pnlEmoji,
            trade.getPnl(),
            trade.getPnlPercentage(),
            trade.getExitReason(),
            trade.getDurationMinutes(),
            trade.getEntryTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            trade.getExitTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            trade.getRiskRewardRatio(),
            trade.getLeverage(),
            pnlEmoji
        );

        telegramService.sendTelegramMessage(message);
    }

    /**
     * Get current price for a symbol (placeholder - implement with your price service)
     */
    private Double getCurrentPrice(String symbol) {
        // This should be implemented with your actual price service
        // For now, return a placeholder
        return 50000.0; // Placeholder price
    }
}
