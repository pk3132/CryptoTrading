package com.tradingbot.service;

import com.tradingbot.model.Trade;
import com.tradingbot.repository.TradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Live Trade Executor Serviceno
 * Handles execution of BUY/SELL trades from database entries on Delta Exchange
 */
@Service
public class LiveTradeExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(LiveTradeExecutor.class);
    
    @Autowired
    private TradeRepository tradeRepository;
    
    @Autowired
    private DeltaOrderService deltaOrderService;
    
    @Autowired
    private BalanceCheck balanceCheck;
    
    @Autowired
    private PositionChecker positionChecker;
    
    @Autowired
    private TelegramNotificationService telegramService;
    
    /**
     * Execute all pending BUY/SELL trades from database
     */
    public void executePendingTrades() {
        logger.info("🚀 Starting Live Trade Execution Process...");
        
        try {
            // Get all open trades from database
            List<Trade> openTrades = tradeRepository.findByStatus("OPEN");
            
            if (openTrades.isEmpty()) {
                logger.info("📊 No open trades found in database");
                return;
            }
            
            logger.info("📋 Found {} open trades to execute", openTrades.size());
            
            // Check account balance before executing trades
            double availableBalance = balanceCheck.getAvailableBalanceAsDouble();
            logger.info("💰 Available Balance: ${}", availableBalance);
            
            if (availableBalance < 0.01) {
                logger.warn("⚠️ Insufficient balance (${}) for live trading. Minimum required: $0.01", availableBalance);
                telegramService.sendTelegramMessage("⚠️ INSUFFICIENT BALANCE WARNING\n" +
                    "Available: $" + availableBalance + "\n" +
                    "Minimum Required: $0.01\n" +
                    "Please deposit funds to continue live trading.");
                return;
            }
            
            // Execute each trade
            for (Trade trade : openTrades) {
                executeTrade(trade);
            }
            
            logger.info("✅ Live Trade Execution Process Complete!");
            
        } catch (Exception e) {
            logger.error("❌ Error in live trade execution: {}", e.getMessage(), e);
            telegramService.sendTelegramMessage("❌ LIVE TRADING ERROR\n" +
                "Error: " + e.getMessage() + "\n" +
                "Please check logs for details.");
        }
    }
    
    /**
     * Execute a specific trade
     */
    public boolean executeTrade(Trade trade) {
        logger.info("🎯 Executing Trade: {} {} {} at ${}", 
                   trade.getType(), trade.getSymbol(), trade.getQuantity(), trade.getEntryPrice());
        
        try {
            // Validate trade data
            if (!validateTrade(trade)) {
                logger.error("❌ Trade validation failed for trade ID: {}", trade.getId());
                return false;
            }
            
            // 🚨 CRITICAL: Check for existing open positions on Delta Exchange
            if (positionChecker.hasOpenPosition(trade.getSymbol())) {
                PositionChecker.PositionInfo existingPos = positionChecker.getPositionForSymbol(trade.getSymbol());
                logger.warn("⚠️ DUPLICATE ORDER PREVENTION: Open position already exists for {}", trade.getSymbol());
                logger.warn("   Existing Position: {} {} contracts at ${}", 
                           existingPos.side, existingPos.size, existingPos.entryPrice);
                logger.warn("   Skipping new order to avoid duplicate");
                return false;
            }
            
            // Check if we already have a position for this symbol in database
            List<Trade> existingTrades = tradeRepository.findBySymbolAndStatus(trade.getSymbol(), "OPEN");
            if (existingTrades.size() > 1) { // More than just this trade
                logger.warn("⚠️ Multiple open database positions found for {}. Skipping execution.", trade.getSymbol());
                return false;
            }
            
            // Execute the order on Delta Exchange
            boolean orderSuccess = executeOrderOnExchange(trade);
            
            if (orderSuccess) {
                // Update trade status to indicate it's been executed
                trade.setStatus("EXECUTED");
                tradeRepository.save(trade);
                
                // Send success notification
                String message = String.format("✅ LIVE TRADE EXECUTED\n" +
                    "Symbol: %s\n" +
                    "Type: %s\n" +
                    "Entry Price: $%.2f\n" +
                    "Quantity: %.0f\n" +
                    "Stop Loss: $%.2f\n" +
                    "Take Profit: $%.2f\n" +
                    "Reason: %s", 
                    trade.getSymbol(), trade.getType(), trade.getEntryPrice(), 
                    trade.getQuantity(), trade.getStopLoss(), trade.getTakeProfit(), 
                    trade.getReason());
                
                telegramService.sendTelegramMessage(message);
                
                logger.info("✅ Trade executed successfully: {}", trade.getId());
                return true;
                
            } else {
                logger.error("❌ Failed to execute order on exchange for trade: {}", trade.getId());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("❌ Error executing trade {}: {}", trade.getId(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Execute order on Delta Exchange
     */
    private boolean executeOrderOnExchange(Trade trade) {
        try {
            logger.info("🌐 Placing {} order on Delta Exchange...", trade.getType());
            
            // Convert quantity to integer (Delta Exchange requirement)
            int orderSize = trade.getQuantity().intValue();
            if (orderSize <= 0) {
                orderSize = 1; // Minimum size
            }
            
            // Set leverage for the product
            String productId = DeltaOrderService.getProductId(trade.getSymbol());
            if (productId != null) {
                int leverage = 10; // 10x leverage
                var leverageResult = deltaOrderService.setLeverage(productId, leverage);
                if (leverageResult != null) {
                    logger.info("✅ Leverage set to {}x for {}", leverage, trade.getSymbol());
                }
            }
            
            // Place the order
            if ("BUY".equals(trade.getType())) {
                return executeBuyOrder(trade, orderSize);
            } else if ("SELL".equals(trade.getType())) {
                return executeSellOrder(trade, orderSize);
            } else {
                logger.error("❌ Invalid trade type: {}", trade.getType());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("❌ Error placing order on exchange: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Execute BUY order
     */
    private boolean executeBuyOrder(Trade trade, int orderSize) {
        try {
            DeltaOrderService.BuyOrderRequest buyRequest = new DeltaOrderService.BuyOrderRequest();
            buyRequest.setProductSymbol(trade.getSymbol());
            buyRequest.setSize(orderSize);
            buyRequest.setOrderType("market_order"); // Use market order for immediate execution
            buyRequest.setLeverage("10"); // 10x leverage
            
            var result = deltaOrderService.placeBuyOrder(buyRequest);
            
            if (result != null && result.containsKey("result")) {
                logger.info("✅ BUY order placed successfully for {}", trade.getSymbol());
                logger.info("📊 Order details: {}", result);
                return true;
            } else {
                logger.error("❌ BUY order failed for {}", trade.getSymbol());
                logger.error("📄 Response: {}", result);
                return false;
            }
            
        } catch (Exception e) {
            logger.error("❌ Error placing BUY order: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Execute SELL order
     */
    private boolean executeSellOrder(Trade trade, int orderSize) {
        try {
            DeltaOrderService.SellOrderRequest sellRequest = new DeltaOrderService.SellOrderRequest();
            sellRequest.setProductSymbol(trade.getSymbol());
            sellRequest.setSize(orderSize);
            sellRequest.setOrderType("market_order"); // Use market order for immediate execution
            sellRequest.setLeverage("10"); // 10x leverage
            
            var result = deltaOrderService.placeSellOrder(sellRequest);
            
            if (result != null && result.containsKey("result")) {
                logger.info("✅ SELL order placed successfully for {}", trade.getSymbol());
                logger.info("📊 Order details: {}", result);
                return true;
            } else {
                logger.error("❌ SELL order failed for {}", trade.getSymbol());
                logger.error("📄 Response: {}", result);
                return false;
            }
            
        } catch (Exception e) {
            logger.error("❌ Error placing SELL order: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Validate trade data before execution
     */
    private boolean validateTrade(Trade trade) {
        // Check required fields
        if (trade.getSymbol() == null || trade.getSymbol().isEmpty()) {
            logger.error("❌ Trade symbol is null or empty");
            return false;
        }
        
        if (trade.getType() == null || (!"BUY".equals(trade.getType()) && !"SELL".equals(trade.getType()))) {
            logger.error("❌ Invalid trade type: {}", trade.getType());
            return false;
        }
        
        if (trade.getEntryPrice() == null || trade.getEntryPrice() <= 0) {
            logger.error("❌ Invalid entry price: {}", trade.getEntryPrice());
            return false;
        }
        
        if (trade.getQuantity() == null || trade.getQuantity() <= 0) {
            logger.error("❌ Invalid quantity: {}", trade.getQuantity());
            return false;
        }
        
        // Validate symbol support
        if (!DeltaOrderService.isSymbolSupported(trade.getSymbol())) {
            logger.error("❌ Unsupported symbol: {}", trade.getSymbol());
            return false;
        }
        
        // Validate price ranges
        if ("BTCUSD".equals(trade.getSymbol()) && (trade.getEntryPrice() < 1000 || trade.getEntryPrice() > 200000)) {
            logger.error("❌ Unrealistic BTC price: {}", trade.getEntryPrice());
            return false;
        }
        
        if ("ETHUSD".equals(trade.getSymbol()) && (trade.getEntryPrice() < 100 || trade.getEntryPrice() > 20000)) {
            logger.error("❌ Unrealistic ETH price: {}", trade.getEntryPrice());
            return false;
        }
        
        logger.info("✅ Trade validation passed for {}", trade.getSymbol());
        return true;
    }
    
    /**
     * Get summary of pending trades
     */
    public String getPendingTradesSummary() {
        List<Trade> openTrades = tradeRepository.findByStatus("OPEN");
        
        if (openTrades.isEmpty()) {
            return "📊 No pending trades in database";
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append("📋 Pending Trades Summary\n");
        summary.append("========================\n");
        summary.append("Total Pending: ").append(openTrades.size()).append("\n\n");
        
        for (Trade trade : openTrades) {
            summary.append("🎯 Trade ID: ").append(trade.getId()).append("\n");
            summary.append("   Symbol: ").append(trade.getSymbol()).append("\n");
            summary.append("   Type: ").append(trade.getType()).append("\n");
            summary.append("   Entry Price: $").append(trade.getEntryPrice()).append("\n");
            summary.append("   Quantity: ").append(trade.getQuantity()).append("\n");
            summary.append("   Stop Loss: $").append(trade.getStopLoss()).append("\n");
            summary.append("   Take Profit: $").append(trade.getTakeProfit()).append("\n");
            summary.append("   Reason: ").append(trade.getReason()).append("\n");
            summary.append("   Entry Time: ").append(trade.getEntryTime()).append("\n\n");
        }
        
        return summary.toString();
    }
    
    /**
     * Execute trades for specific symbol
     */
    public void executeTradesForSymbol(String symbol) {
        logger.info("🎯 Executing trades for symbol: {}", symbol);
        
        List<Trade> symbolTrades = tradeRepository.findBySymbolAndStatus(symbol, "OPEN");
        
        if (symbolTrades.isEmpty()) {
            logger.info("📊 No open trades found for {}", symbol);
            return;
        }
        
        logger.info("📋 Found {} open trades for {}", symbolTrades.size(), symbol);
        
        for (Trade trade : symbolTrades) {
            executeTrade(trade);
        }
    }
}
