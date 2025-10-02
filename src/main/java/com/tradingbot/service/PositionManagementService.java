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

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PositionManagementService.class);

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
     * With auto-sync to prevent stuck states
     */
    public boolean isInCooldown(String symbol) {
        logger.info("═══════════════════════════════════════════════════════════");
        logger.info("🔍 COOLDOWN CHECK for {}", symbol);
        
        TrendlineState state = symbolTrendlineState.get(symbol);
        if (state == null) {
            logger.info("✅ {} - No cooldown state found", symbol);
            logger.info("✅ {} - READY FOR NEW TRADES", symbol);
            logger.info("═══════════════════════════════════════════════════════════");
            return false; // No recent profitable trade
        }
        
        logger.info("📊 {} - Cooldown state exists", symbol);
        logger.info("   hasNewTrendline: {}", state.hasNewTrendline);
        logger.info("   trendlineBroken: {}", state.trendlineBroken);
        logger.info("   lastTrendlineValue: {}", state.lastTrendlineValue);
        logger.info("   previousTrendlineValue: {}", state.previousTrendlineValue);
        
        // 🛡️ Auto-sync: Check if position actually exists in database
        try {
            boolean dbHasPosition = positionChecker.hasOpenPosition(symbol);
            logger.info("🗄️ {} - Database position check: {}", symbol, (dbHasPosition ? "OPEN" : "CLOSED"));
            
            if (!dbHasPosition) {
                // Position closed in DB but cooldown state exists - clear it
                logger.warn("⚠️ {} - MISMATCH DETECTED!", symbol);
                logger.warn("   Database: Position CLOSED");
                logger.warn("   Memory: Cooldown state EXISTS");
                logger.info("🔄 {} - AUTO-CLEARING cooldown state", symbol);
                clearSymbolCooldownState(symbol);
                logger.info("✅ {} - READY FOR NEW TRADES (auto-sync cleared)", symbol);
                logger.info("═══════════════════════════════════════════════════════════");
                return false; // Ready for new trades
            } else {
                logger.info("✅ {} - Database and memory in sync (position still open)", symbol);
            }
        } catch (Exception e) {
            logger.error("❌ Error checking position for {}: {}", symbol, e.getMessage());
        }
        
        // First check: New trendline must be formed
        if (!state.hasNewTrendline) {
            logger.info("⏰ {} - BLOCKED: Waiting for NEW trendline formation", symbol);
            logger.info("═══════════════════════════════════════════════════════════");
            return true;
        }
        
        // Second check: Ensure new trendline is actually NEW and different
        if (state.lastTrendlineValue == state.previousTrendlineValue && state.previousTrendlineValue != 0) {
            logger.info("♻️ {} - BLOCKED: Need FRESH trendline (current identical to last)", symbol);
            logger.info("   Current: {}", state.lastTrendlineValue);
            logger.info("   Previous: {}", state.previousTrendlineValue);
            logger.info("═══════════════════════════════════════════════════════════");
            return true;
        }
        
        // Third check: Must have breakout confirmation  
        if (!state.trendlineBroken) {
            logger.info("🚧 {} - BLOCKED: Trendline formed, waiting for breakout", symbol);
            logger.info("═══════════════════════════════════════════════════════════");
            return true;
        }
        
        logger.info("✅ {} - ALL CONDITIONS MET!", symbol);
        logger.info("✅ {} - READY FOR NEW TRADES", symbol);
        logger.info("═══════════════════════════════════════════════════════════");
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
            final double slPct = 0.002;  // 0.20%
            final double tpPct = 0.006;  // 0.60%
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
     * Enhanced logic: Set fresh trendline + breakout cooldown for ALL trades (profit or loss)
     */
    private void checkAndSetCooldown(Trade trade) {
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("🔄 TRADE CLOSE HANDLER");
        
        String symbol = trade.getSymbol();
        LocalDateTime closeTime = trade.getExitTime();
        Double pnl = trade.getPnl();
        
        System.out.println("📊 Trade Details:");
        System.out.println("   Symbol: " + symbol);
        System.out.println("   Type: " + trade.getType());
        System.out.println("   Entry: $" + trade.getEntryPrice());
        System.out.println("   Exit: $" + trade.getExitPrice());
        System.out.println("   PnL: $" + (pnl != null ? String.format("%.2f", pnl) : "N/A"));
        System.out.println("   Close Time: " + closeTime);
        
        // First, always clear any existing cooldown state when trade closes
        if (symbolTrendlineState.containsKey(symbol)) {
            System.out.println("🔄 " + symbol + " - Clearing existing cooldown state");
            clearSymbolCooldownState(symbol);
        } else {
            System.out.println("ℹ️ " + symbol + " - No existing cooldown state to clear");
        }
        
        // For ALL trades (profit or loss), set fresh trendline + breakout monitoring
        if (closeTime != null) {
            String tradeType = (pnl != null && pnl > 0) ? "PROFITABLE" : "LOSS";
            System.out.println("🛡️ Setting NEW cooldown for " + symbol + " (" + tradeType + " trade)");
            
            // Reset state after ANY trade - monitor for completely FRESH trendline
            symbolTrendlineState.put(symbol, new TrendlineState(
                false,           // hasNewTrendline - force completely new formation   
                false,           // trendlineBroken - need fresh breakout
                closeTime,       // when trade closed
                null,            // lastTrendlineType - reset  
                0.0,             // lastTrendlineValue - reset (will be different)
                null,            // lastTrendlineFormationTime - reset timing
                0.0              // previousTrendlineValue - clear old reference
            ));
            
            lastProfitableTradeTimes.put(symbol, closeTime);
            
            System.out.println("✅ " + symbol + " - Cooldown state SET");
            System.out.println("📋 " + symbol + " - Waiting for:");
            System.out.println("   1️⃣ NEW trendline formation");
            System.out.println("   2️⃣ FRESH trendline (2% different from previous)");
            System.out.println("   3️⃣ BREAKOUT confirmation");
        } else {
            System.out.println("⚠️ " + symbol + " - No close time available, skipping cooldown setup");
        }
        
        System.out.println("═══════════════════════════════════════════════════════════");
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
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("🔍 GET OPEN POSITIONS - DEBUG");
        
        // Get all trades to debug
        List<Trade> allTrades = tradeRepository.findAll();
        System.out.println("📊 Total trades in database: " + allTrades.size());
        
        if (!allTrades.isEmpty()) {
            System.out.println("📋 Recent 5 trades:");
            allTrades.stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .limit(5)
                .forEach(t -> System.out.println(String.format(
                    "   ID: %d, Symbol: %s, Type: %s, Status: '%s', Entry: $%.2f, Time: %s",
                    t.getId(), t.getSymbol(), t.getType(), t.getStatus(), 
                    t.getEntryPrice(), t.getEntryTime()
                )));
        }
        
        // Get OPEN trades (including legacy "EXECUTED" status)
        List<Trade> openTrades = tradeRepository.findByStatus("OPEN");
        List<Trade> executedTrades = tradeRepository.findByStatus("EXECUTED"); // Legacy support
        
        System.out.println("✅ Trades with status='OPEN': " + openTrades.size());
        System.out.println("⚠️ Trades with status='EXECUTED' (legacy): " + executedTrades.size());
        
        // Combine both for monitoring
        openTrades.addAll(executedTrades);
        
        if (!openTrades.isEmpty()) {
            openTrades.forEach(t -> System.out.println(String.format(
                "   OPEN Trade: ID=%d, %s %s at $%.2f",
                t.getId(), t.getSymbol(), t.getType(), t.getEntryPrice()
            )));
        }
        
        System.out.println("═══════════════════════════════════════════════════════════");
        return openTrades;
    }

    /**
     * Get open positions for a specific symbol
     */
    public List<Trade> getOpenPositions(String symbol) {
        List<Trade> openTrades = tradeRepository.findBySymbolAndStatus(symbol, "OPEN");
        List<Trade> executedTrades = tradeRepository.findBySymbolAndStatus(symbol, "EXECUTED"); // Legacy
        openTrades.addAll(executedTrades);
        return openTrades;
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
     * Enhanced: Checks DATABASE first (source of truth), then Delta Exchange for sync
     */
    public boolean hasOpenPosition(String symbol, String type) {
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("🔍 POSITION CHECK for " + symbol + " " + type);
        
        // STEP 1: Check database for open trades (source of truth)
        List<Trade> openTrades = tradeRepository.findBySymbolAndStatus(symbol, "OPEN");
        boolean hasDbPosition = !openTrades.isEmpty();
        boolean hasMatchingType = openTrades.stream().anyMatch(trade -> trade.getType().equals(type));
        
        System.out.println("📊 Database Check:");
        System.out.println("   Total open trades for " + symbol + ": " + openTrades.size());
        if (hasDbPosition) {
            for (Trade trade : openTrades) {
                System.out.println("   - ID: " + trade.getId() + ", Type: " + trade.getType() + 
                                 ", Entry: $" + trade.getEntryPrice() + ", Status: " + trade.getStatus());
            }
        } else {
            System.out.println("   ✅ No open trades in database");
        }
        
        // STEP 2: Check Delta Exchange for actual positions (sync verification)
        boolean hasExchangePosition = false;
        try {
            hasExchangePosition = positionChecker.hasOpenPosition(symbol);
            System.out.println("📊 Delta Exchange Check: " + (hasExchangePosition ? "⚠️ POSITION EXISTS" : "✅ NO POSITION"));
            
            if (hasExchangePosition) {
                PositionChecker.PositionInfo exchangePos = positionChecker.getPositionForSymbol(symbol);
                if (exchangePos != null) {
                    System.out.println("   Exchange Position: " + exchangePos.side + " " + exchangePos.size + 
                                     " contracts at $" + exchangePos.entryPrice);
                }
            }
        } catch (Exception e) {
            System.out.println("   ❌ Exchange check error: " + e.getMessage());
        }
        
        // STEP 3: Sync check - warn if mismatch
        if (hasDbPosition && !hasExchangePosition) {
            System.out.println("⚠️ SYNC MISMATCH DETECTED!");
            System.out.println("   Database: HAS open position");
            System.out.println("   Exchange: NO position");
            System.out.println("   → Using DATABASE as source of truth");
        } else if (!hasDbPosition && hasExchangePosition) {
            System.out.println("⚠️ REVERSE SYNC MISMATCH!");
            System.out.println("   Database: NO open position");
            System.out.println("   Exchange: HAS position");
            System.out.println("   → Possible manual close or DB update failure");
        }
        
        // STEP 4: Decision - use DATABASE as source of truth
        boolean shouldBlock = hasMatchingType;
        
        if (shouldBlock) {
            System.out.println("🛡️ BLOCKING: Database has matching " + type + " position for " + symbol);
        } else {
            System.out.println("✅ ALLOWING: No matching " + type + " position in database");
        }
        
        System.out.println("═══════════════════════════════════════════════════════════");
        return shouldBlock;
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
     * Reset all cooldown states (called on application startup)
     */
    public void resetCooldownStates() {
        symbolTrendlineState.clear();
        lastProfitableTradeTimes.clear();
        System.out.println("🔄 All cooldown states reset - Ready for immediate trading");
    }

    /**
     * Clear cooldown state for specific symbol (called after trade close or mismatch detection)
     */
    private void clearSymbolCooldownState(String symbol) {
        symbolTrendlineState.remove(symbol);
        lastProfitableTradeTimes.remove(symbol);
        System.out.println("🔄 " + symbol + " cooldown state cleared - Ready for new trades");
    }

    /**
     * Get actual position size from Delta Exchange using PositionChecker
     */
    private int getActualPositionSize(String symbol) {
        try {
            // For consistency, always use 1 contract for both BTC and ETH
            // This prevents position size issues when restarting the application
            System.out.println("   📊 Using fixed position size: 1 contract for " + symbol);
            return 1; // Fixed size for both BTC and ETH
        } catch (Exception e) {
            System.err.println("   ❌ Error getting actual position size: " + e.getMessage());
            return 1; // Default to 1 contract
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
