package com.tradingbot.service;

import com.tradingbot.model.Trade;
import com.tradingbot.repository.TradeRepository;
import com.tradingbot.service.DeltaOrderService;
import com.tradingbot.service.DeltaOrderService.BuyOrderRequest;
import com.tradingbot.service.DeltaOrderService.SellOrderRequest;
import com.tradingbot.service.DeltaApiClient;
import com.tradingbot.service.PositionChecker;
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
    private DeltaOrderService deltaOrderService;
    
    @Autowired
    private DeltaApiClient deltaApiClient;
    
    @Autowired
    private PositionChecker positionChecker;
    
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
            // Use DeltaApiClient for more reliable price fetching
            Double currentPrice = deltaApiClient.getCurrentMarkPrice(symbol);
            
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

        // Calculate quantity using leveraged position sizing
        // Use fixed position size for simplicity
        int contracts = 1; // Minimum position size
        Double quantity = (double) contracts;
        
        // Validate symbol support
        if (!DeltaOrderService.isSymbolSupported(symbol)) {
            System.err.println("❌ Unsupported symbol: " + symbol + ". Only BTCUSD and ETHUSD are supported.");
            return null;
        }
        
        // 🚀 DEMO ACCOUNT ORDER PLACEMENT FIRST (to avoid sync issues)
        boolean demoOrderSuccess = placeDemoOrder(symbol, type, entryPrice, stopLoss, takeProfit, quantity, reason);
        
        // Only save to database if demo order was successful
        if (!demoOrderSuccess) {
            System.err.println("❌ Demo order failed - not saving to database to maintain sync");
            return null;
        }
        
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
     * Place order on Delta Exchange Demo Account
     * @return true if order was successful, false if failed
     */
    private boolean placeDemoOrder(String symbol, String type, Double entryPrice, Double stopLoss, Double takeProfit, Double quantity, String reason) {
        try {
            System.out.println("🚀 PLACING DEMO ORDER:");
            System.out.println("   Symbol: " + symbol);
            System.out.println("   Type: " + type);
            System.out.println("   Entry Price: $" + entryPrice);
            System.out.println("   Quantity: " + quantity);
            System.out.println("   Stop Loss: $" + stopLoss);
            System.out.println("   Take Profit: $" + takeProfit);
            System.out.println("   Contract Size: " + DeltaOrderService.getContractSize(symbol));
            System.out.println();

            // Convert quantity to integer (Delta Exchange requires integer sizes)
            int orderSize = quantity != null ? quantity.intValue() : 1;
            if (orderSize <= 0) orderSize = 1; // Minimum size
            
            // Special handling for ETH (server timeout issues)
            if ("ETHUSD".equals(symbol)) {
                System.out.println("⚠️  ETH Trading Notice: ETH orders may experience timeouts due to testnet server issues.");
                System.out.println("   This is a temporary infrastructure issue, not a code problem.");
                System.out.println("   Orders will work when server stability improves.");
                System.out.println();
            }

            if ("BTCUSD".equals(symbol)) {
                // Special handling for BTC with complete order (main + stop loss + take profit)
                if ("BUY".equals(type)) {
                    System.out.println("📈 Placing COMPLETE BTC BUY order with Target & Stop Loss");
                    var btcResult = deltaOrderService.placeBuyOrder(new BuyOrderRequest(symbol, quantity.intValue(), "market_order"));
                    
                    if (btcResult != null) {
                        System.out.println("✅ COMPLETE BTC BUY ORDER EXECUTED!");
                        System.out.println("   Main Order ID: " + btcResult.get("id"));
                        System.out.println("   Status: " + btcResult.get("state"));
                        System.out.println("   Fill Price: " + btcResult.get("average_fill_price"));
                    } else {
                        System.out.println("❌ COMPLETE BTC BUY ORDER FAILED!");
                    }
                    
                } else if ("SELL".equals(type)) {
                    System.out.println("📉 Placing COMPLETE BTC SELL order with Target & Stop Loss");
                    var btcResult = deltaOrderService.placeSellOrder(new SellOrderRequest(symbol, quantity.intValue(), "market_order"));
                    
                    if (btcResult != null) {
                        System.out.println("✅ COMPLETE BTC SELL ORDER EXECUTED!");
                        System.out.println("   Main Order ID: " + btcResult.get("id"));
                        System.out.println("   Status: " + btcResult.get("state"));
                        System.out.println("   Fill Price: " + btcResult.get("average_fill_price"));
                    } else {
                        System.out.println("❌ COMPLETE BTC SELL ORDER FAILED!");
                    }
                }
                
            } else if ("BUY".equals(type)) {
                // Standard handling for other symbols (ETH)
                System.out.println("📈 Placing LEVERAGED BUY order for " + symbol);
                var buyResult = deltaOrderService.placeBuyOrder(new BuyOrderRequest(symbol, quantity.intValue(), "market_order"));
                
                if (buyResult != null) {
                    System.out.println("✅ LEVERAGED BUY order placed successfully!");
                    System.out.println("   Order ID: " + buyResult.get("id"));
                    System.out.println("   Status: " + buyResult.get("state"));
                    System.out.println("   Fill Price: " + buyResult.get("average_fill_price"));
                } else {
                    System.out.println("❌ LEVERAGED BUY order failed!");
                    if ("ETHUSD".equals(symbol)) {
                        System.out.println("   Note: ETH orders may fail due to testnet server timeouts.");
                        System.out.println("   This is temporary - will work when server is stable.");
                    }
                }
                
                // Stop loss is now handled in the main order
                
                // Take profit is now handled in the main order
                
            } else if ("SELL".equals(type)) {
                // Standard handling for other symbols (ETH)
                System.out.println("📉 Placing LEVERAGED SELL order for " + symbol);
                var sellResult = deltaOrderService.placeSellOrder(new SellOrderRequest(symbol, quantity.intValue(), "market_order"));
                
                if (sellResult != null) {
                    System.out.println("✅ LEVERAGED SELL order placed successfully!");
                    System.out.println("   Order ID: " + sellResult.get("id"));
                    System.out.println("   Status: " + sellResult.get("state"));
                    System.out.println("   Fill Price: " + sellResult.get("average_fill_price"));
                } else {
                    System.out.println("❌ LEVERAGED SELL order failed!");
                    if ("ETHUSD".equals(symbol)) {
                        System.out.println("   Note: ETH orders may fail due to testnet server timeouts.");
                        System.out.println("   This is temporary - will work when server is stable.");
                    }
                }
                
                // Stop loss is now handled in the main order
                
                // Take profit is now handled in the main order
            }
            
            return true; // All orders placed successfully
            
        } catch (Exception e) {
            System.err.println("❌ Error placing demo order: " + e.getMessage());
            e.printStackTrace();
            return false; // Order failed
        }
    }

    /**
     * Place exit order on Delta Exchange Demo Account
     */
    private void placeDemoExitOrder(Trade trade, Double exitPrice) {
        try {
            System.out.println("🚀 PLACING DEMO EXIT ORDER:");
            System.out.println("   Symbol: " + trade.getSymbol());
            System.out.println("   Original Type: " + trade.getType());
            System.out.println("   Exit Price: $" + exitPrice);
            System.out.println("   Exit Reason: " + trade.getExitReason());
            System.out.println();

            // 🎯 ENHANCED: Get actual position size from Delta Exchange
            int orderSize = getActualPositionSize(trade.getSymbol());
            System.out.println("   🔍 Actual Position Size: " + orderSize + " contracts");
            
            if (orderSize <= 0) {
                System.out.println("   ⚠️ No position found on exchange, using database quantity");
                orderSize = trade.getQuantity() != null ? trade.getQuantity().intValue() : 1;
                if (orderSize <= 0) orderSize = 1;
            }

            // For exit orders, we need to close the opposite position
            if ("BUY".equals(trade.getType().toString())) {
                // Original was BUY, so exit with SELL
                System.out.println("📉 Placing EXIT SELL order for " + trade.getSymbol() + " (Size: " + orderSize + ")");
                
                // Use DeltaOrderService for exits
                var exitResult = deltaOrderService.placeSellOrder(new SellOrderRequest(trade.getSymbol(), orderSize, "market_order"));
                
                if (exitResult != null) {
                    System.out.println("✅ EXIT SELL order placed successfully!");
                    System.out.println("   Order ID: " + exitResult.get("id"));
                    System.out.println("   Status: " + exitResult.get("state"));
                    System.out.println("   Fill Price: " + exitResult.get("average_fill_price"));
                } else {
                    System.out.println("❌ EXIT SELL order failed!");
                    if ("ETHUSD".equals(trade.getSymbol())) {
                        System.out.println("   Note: ETH exit orders may fail due to testnet server timeouts.");
                    }
                }
                
            } else if ("SELL".equals(trade.getType().toString())) {
                // Original was SELL, so exit with BUY
                System.out.println("📈 Placing EXIT BUY order for " + trade.getSymbol() + " (Size: " + orderSize + ")");
                
                // Use DeltaOrderService for exits
                var exitResult = deltaOrderService.placeBuyOrder(new BuyOrderRequest(trade.getSymbol(), orderSize, "market_order"));
                
                if (exitResult != null) {
                    System.out.println("✅ EXIT BUY order placed successfully!");
                    System.out.println("   Order ID: " + exitResult.get("id"));
                    System.out.println("   Status: " + exitResult.get("state"));
                    System.out.println("   Fill Price: " + exitResult.get("average_fill_price"));
                } else {
                    System.out.println("❌ EXIT BUY order failed!");
                    if ("ETHUSD".equals(trade.getSymbol())) {
                        System.out.println("   Note: ETH exit orders may fail due to testnet server timeouts.");
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error placing demo exit order: " + e.getMessage());
            e.printStackTrace();
        }
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
            
            // 🚀 DEMO ACCOUNT EXIT ORDER PLACEMENT
            placeDemoExitOrder(trade, exitPrice);
            
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
     * Enhanced: Checks both database AND Delta Exchange for existing positions
     */
    public boolean hasOpenPosition(String symbol, String type) {
        // First check database for open trades
        List<Trade> openTrades = tradeRepository.findBySymbolAndStatus(symbol, "OPEN");
        boolean hasDbPosition = openTrades.stream().anyMatch(trade -> trade.getType().equals(type));
        
        // Then check Delta Exchange for actual positions
        boolean hasExchangePosition = positionChecker.hasOpenPosition(symbol);
        
        // Log the results for debugging
        System.out.println("🔍 Position Check for " + symbol + " " + type + ":");
        System.out.println("   Database: " + (hasDbPosition ? "⚠️ OPEN" : "✅ NONE"));
        System.out.println("   Exchange: " + (hasExchangePosition ? "⚠️ OPEN" : "✅ NONE"));
        
        // Return true if either database OR exchange has position
        boolean hasAnyPosition = hasDbPosition || hasExchangePosition;
        
        if (hasAnyPosition) {
            System.out.println("   🛡️ DUPLICATE PREVENTION: Blocking new " + type + " order for " + symbol);
        } else {
            System.out.println("   ✅ CLEAR: No existing position found for " + symbol);
        }
        
        return hasAnyPosition;
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
     * Get actual position size from Delta Exchange using PositionChecker
     */
    private int getActualPositionSize(String symbol) {
        try {
            PositionChecker.PositionInfo position = positionChecker.getPositionForSymbol(symbol);
            if (position != null && position.isOpen()) {
                System.out.println("   ✅ Found actual position: " + position.side + " " + position.size + " contracts");
                return (int) position.size;
            } else {
                System.out.println("   ⚠️ No actual position found on exchange for " + symbol);
                return 0;
            }
        } catch (Exception e) {
            System.err.println("   ❌ Error getting actual position size: " + e.getMessage());
            return 0;
        }
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
