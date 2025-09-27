package com.tradingbot.service;

import com.tradingbot.model.Trade;
import com.tradingbot.repository.TradeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

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

    @Autowired
    private CryptoPriceService priceService;
    
    // Enhanced trendline formation tracking
    public static class TrendlineState {
        public final boolean hasNewTrendline;
        public final boolean trendlineBroken;
        public final LocalDateTime lastProfitableTradeTime;
        public final String lastTrendlineType;
        public final double lastTrendlineValue;
        public final LocalDateTime lastTrendlineFormationTime;
        public final double previousTrendlineValue; // Track if trendline changed significantly
        
        public TrendlineState(boolean hasNewTrendline, boolean trendlineBroken, 
                           LocalDateTime lastProfitableTradeTime, String lastTrendlineType, 
                           double lastTrendlineValue, LocalDateTime lastTrendlineFormationTime, 
                           double previousTrendlineValue) {
            this.hasNewTrendline = hasNewTrendline;
            this.trendlineBroken = trendlineBroken;
            this.lastProfitableTradeTime = lastProfitableTradeTime;
            this.lastTrendlineType = lastTrendlineType;
            this.lastTrendlineValue = lastTrendlineValue;
            this.lastTrendlineFormationTime = lastTrendlineFormationTime;
            this.previousTrendlineValue = previousTrendlineValue;
        }
    }
    
    // Track trendline formation state per symbol 
    private final Map<String, TrendlineState> symbolTrendlineState = new ConcurrentHashMap<>();
    
    // Track when profitable trades closed to reset trendline monitoring
    private final Map<String, LocalDateTime> lastProfitableTradeTimes = new ConcurrentHashMap<>();

    /**
     * Enhanced logic: Must wait for FRESH trendline (different from last one used) + breakout
     */
    public boolean isInCooldown(String symbol) {
        TrendlineState state = symbolTrendlineState.get(symbol);
        if (state == null) {
            return false; // No recent profitable trade
        }
        
        // First check: New trendline must be formed
        if (!state.hasNewTrendline) {
            System.out.println("⏰ " + symbol + " - Waiting for NEW trendline formation after profitable trade");
            return true;
        }
        
        // Second check: Ensure new trendline is actually NEW and different
        if (state.lastTrendlineValue == state.previousTrendlineValue && state.previousTrendlineValue != 0) {
            System.out.println("♻️ " + symbol + " - Need FRESH trendline (current is identical to last one)");
            return true;
        }
        
        // Third check: Must have breakout confirmation  
        if (!state.trendlineBroken) {
            System.out.println("🚧 " + symbol + " - New trendline formed, waiting for breakout confirmation");
            return true;
        }
        
        return false; // All conditions met - genuinely new trendline + breakout = ready
    }
    
    /**
     * Enhanced status message for timing trendline development  
     */
    public String getTrendlineStatusMessage(String symbol) {
        TrendlineState state = symbolTrendlineState.get(symbol);
        if (state == null) {
            return symbol + " - No recent profitable trades"; 
        }
        
        if (!state.hasNewTrendline) {
            return symbol + " - ⏰ Waiting for NEW trendline formation after profitable trade";
        }
        
        if (state.lastTrendlineValue == state.previousTrendlineValue && state.previousTrendlineValue != 0) {
            return symbol + " - ♻️ Need FRESH trendline (current identical to last - market evolving)";
        }
        
        if (!state.trendlineBroken) {
            return symbol + " - 🚧 NEW trendline detected, waiting for breakout confirmation";
        }
        
        return symbol + " - ✅ Ready: FRESH trendline formed and broken for trading";
    }

    /**
     * Open a new trade position
     */
    public Trade openPosition(String symbol, String type, Double entryPrice, 
                             Double stopLoss, Double takeProfit, String reason) {
        // Enhanced check: Must have both new trendline AND breakout 
        if (isInCooldown(symbol)) {
            String statusMsg = getTrendlineStatusMessage(symbol);
            System.out.println("🛡️ " + statusMsg);
            return null; // Skip opening - waiting for trendline fulfillment
        }
        
        // Sanity check: correct obviously wrong entry prices by using current price
        try {
            // Use CryptoPriceService for more reliable price fetching
            Double currentPrice = null;
            if ("BTCUSD".equals(symbol)) {
                currentPrice = priceService.getBitcoinPrice();
            } else if ("ETHUSD".equals(symbol)) {
                currentPrice = priceService.getEthereumPrice();
            }
            
            if (currentPrice != null && currentPrice > 0) {
                // Use current price from candles endpoint (more reliable than tickers)
                entryPrice = currentPrice;
            }
            // Hard validation: prevent saving unrealistic entry prices
            boolean badBtcEth = ("BTCUSD".equals(symbol) || "ETHUSD".equals(symbol)) && entryPrice != null && entryPrice < 1000;
            if (badBtcEth) {
                throw new IllegalStateException("Refusing to open trade with unrealistic entryPrice=" + entryPrice + " for " + symbol +
                        ". Current price fetched=" + currentPrice + ".");
            }
            // If SL/TP are provided by the strategy (e.g., swing-based), preserve them; otherwise compute defaults
            final double slPct = 0.005;  // 0.50%
            final double tpPct = 0.01;   // 1.00%
            if ("BUY".equals(type)) {
                if (entryPrice != null) {
                    stopLoss = (stopLoss != null && stopLoss < entryPrice) ? roundToTick(symbol, stopLoss)
                            : roundToTick(symbol, entryPrice * (1 - slPct));
                    takeProfit = (takeProfit != null && takeProfit > entryPrice) ? roundToTick(symbol, takeProfit)
                            : roundToTick(symbol, entryPrice * (1 + tpPct));
                }
            } else { // SELL
                if (entryPrice != null) {
                    stopLoss = (stopLoss != null && stopLoss > entryPrice) ? roundToTick(symbol, stopLoss)
                            : roundToTick(symbol, entryPrice * (1 + slPct));
                    takeProfit = (takeProfit != null && takeProfit < entryPrice) ? roundToTick(symbol, takeProfit)
                            : roundToTick(symbol, entryPrice * (1 - tpPct));
                }
            }
        } catch (Exception ignore) {}

        // Calculate quantity based on risk management (example: 1% risk)
        Double quantity = calculatePositionSize(entryPrice, stopLoss);
        
        Trade trade = new Trade(symbol, type, entryPrice, stopLoss, takeProfit, quantity, reason);
        Trade savedTrade = tradeRepository.save(trade);
        
        System.out.println("✅ New position opened: " + savedTrade);
        
        // Send Telegram notification for new position
        if (entryPrice != null && stopLoss != null && takeProfit != null) {
            if (type.equals("BUY")) {
                telegramService.sendBuySignal(symbol, entryPrice, stopLoss, takeProfit, reason);
            } else {
                telegramService.sendSellSignal(symbol, entryPrice, stopLoss, takeProfit, reason);
            }
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
            
            // Track profitable trades to enforce cooldown period
            checkAndSetCooldown(trade);
            
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
     * Enhanced logic: Start monitoring for NEW trendline after profitable trade
     */
    private void checkAndSetCooldown(Trade trade) {
        if (trade.getPnl() != null && trade.getPnl() > 0) {
            String symbol = trade.getSymbol();
            LocalDateTime closeTime = trade.getExitTime();
            if (closeTime != null) {
                // Reset state after profitable trade - monitor for completely FRESH trendline
                symbolTrendlineState.put(symbol, new TrendlineState(
                    false,           // hasNewTrendline - force completely new formation   
                    false,           // trendlineBroken - need fresh breakout
                    closeTime,       // when profitable trade closed
                    null,            // lastTrendlineType - reset  
                    0.0,             // lastTrendlineValue - reset (will be different)
                    null,            // lastTrendlineFormationTime - reset timing
                    0.0              // previousTrendlineValue - clear old reference
                ));
                
                lastProfitableTradeTimes.put(symbol, closeTime);
                System.out.println("🛡️ Profitable trade closed for " + symbol + " - Starting NEW trendline formation monitoring");
            }
        }
    }
    
    /**
     * Enhanced update with fresh trendline validation
     */
    public void updateTrendlineFormation(String symbol, String trendlineType, double trendlineValue, boolean isBreaking) {
        if (symbolTrendlineState.containsKey(symbol)) {
            TrendlineState currentState = symbolTrendlineState.get(symbol);
            
            // Check if this is truly a NEW/FRESH trendline different from previous
            boolean isFreshTrendline = false;
            if (currentState.previousTrendlineValue == 0 || 
                Math.abs(trendlineValue - currentState.lastTrendlineValue) > trendlineValue * 0.02) { // 2% difference threshold for "new"
                isFreshTrendline = true;
                System.out.println("🔍 Detected fresh " + trendlineType + " trendline vs previous: " + 
                                 currentState.lastTrendlineValue + " → " + trendlineValue);
            }
            
            LocalDateTime formationTime = LocalDateTime.now(); // Record when trendline detected 
            if (isFreshTrendline) {
                System.out.println("📊 NEW " + trendlineType + " trendline detected for " + symbol + 
                                 " at " + trendlineValue);
            }
            
            // Only update if we're monitoring AND (it's fresh OR we detect structural change)
            symbolTrendlineState.put(symbol, new TrendlineState(
                true,                                 // hasNewTrendline - accept if different enough 
                isBreaking,                           // trendlineBroken
                currentState.lastProfitableTradeTime,
                trendlineType,
                trendlineValue,
                formationTime,                        // Record timing of new formation
                currentState.lastTrendlineValue       // Save the previous for comparison
            ));
            
            String message = isBreaking ? "BREAKOUT CONFIRMED" : 
                            (isFreshTrendline ? "Fresh trendline - waiting for breakout" : "Formed");
            System.out.println("📈 " + symbol + " " + trendlineType + " status: " + message);
        }
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
        
        if (entryPrice != null && stopLoss != null) {
            double riskPerUnit = Math.abs(entryPrice - stopLoss);
            
            if (riskPerUnit > 0) {
                return riskAmount / riskPerUnit;
            }
        }
        
        return 1.0; // Default quantity
    }

    private double roundToTick(String symbol, double price) {
        double tick;
        if ("BTCUSD".equals(symbol)) tick = 0.5;
        else if ("ETHUSD".equals(symbol)) tick = 0.05;
        else tick = 0.01;
        return Math.round(price / tick) * tick;
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
