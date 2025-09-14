package com.tradingbot.service;

import com.tradingbot.strategy.MovingAverageStrategy;
import com.tradingbot.strategy.EMAStrategy;
import com.tradingbot.model.CryptoStrategy2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Dual Strategy Scheduler Service
 * 
 * Runs both strategies with different intervals:
 * - Strategy 1 (200-Day MA + Trendline): Every 1 minute
 * - Strategy 2 (9/20 EMA Crossover): Every 2 minutes
 */
@Service
@EnableScheduling
public class DualStrategySchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(DualStrategySchedulerService.class);

    @Autowired
    private MovingAverageStrategy strategy1;
    
    @Autowired
    private EMAStrategy strategy2;
    
    @Autowired
    private CryptoPriceService priceService;
    
    
    @Autowired
    private PositionManagementService positionService;
    
    @Autowired
    private Strategy2PositionService strategy2PositionService;
    
    @Autowired
    private DeltaCandlestickService candlestickService;
    
    @Autowired
    private AlertVerificationService alertVerificationService;
    
    // Strategy monitoring flags
    private boolean strategy1Enabled = true;
    private boolean strategy2Enabled = true;
    
    // Monitoring counters
    private int strategy1Cycles = 0;
    private int strategy2Cycles = 0;
    
    // Symbols to monitor
    private static final String[] SYMBOLS_TO_MONITOR = {"BTCUSD", "ETHUSD", "SOLUSD"};

    /**
     * Strategy 1: 200-Day MA + Trendline Breakout
     * Runs every 1 minute
     */
    @Scheduled(fixedRate = 60000) // 1 minute = 60,000 milliseconds
    public void runStrategy1() {
        if (!strategy1Enabled) {
            logger.debug("Strategy 1 is disabled - skipping execution");
            return;
        }
        
        strategy1Cycles++;
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        logger.info("🎯 STRATEGY 1 EXECUTION #{} - {}", strategy1Cycles, timestamp);
        logger.info("📊 Strategy: 200-Day MA + Trendline Breakout");
        logger.info("📈 Risk-Reward: 6:1 (Conservative)");
        logger.debug("Monitoring symbols: {}", String.join(", ", SYMBOLS_TO_MONITOR));
        
        try {
            for (String symbol : SYMBOLS_TO_MONITOR) {
                logger.debug("Processing Strategy 1 for symbol: {}", symbol);
                monitorStrategy1(symbol, timestamp);
            }
        } catch (Exception e) {
            logger.error("❌ Error in Strategy 1 execution", e);
            logger.error("Error details: {}", e.getMessage(), e);
        }
        
        logger.info("✅ Strategy 1 cycle #{} completed", strategy1Cycles);
    }

    /**
     * Strategy 2: 9/20 EMA Crossover
     * Runs every 2 minutes
     */
    @Scheduled(fixedRate = 120000) // 2 minutes = 120,000 milliseconds
    public void runStrategy2() {
        if (!strategy2Enabled) {
            logger.debug("Strategy 2 is disabled - skipping execution");
            return;
        }
        
        strategy2Cycles++;
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        logger.info("⚡ STRATEGY 2 EXECUTION #{} - {}", strategy2Cycles, timestamp);
        logger.info("📊 Strategy: 9/20 EMA Crossover");
        logger.info("📈 Risk-Reward: 1:3 (Aggressive)");
        logger.info("⚡ Leverage: 25x");
        logger.debug("Monitoring symbols: {}", String.join(", ", SYMBOLS_TO_MONITOR));
        
        try {
            for (String symbol : SYMBOLS_TO_MONITOR) {
                logger.debug("Processing Strategy 2 for symbol: {}", symbol);
                monitorStrategy2(symbol, timestamp);
            }
        } catch (Exception e) {
            logger.error("❌ Error in Strategy 2 execution", e);
            logger.error("Error details: {}", e.getMessage(), e);
        }
        
        logger.info("✅ Strategy 2 cycle #{} completed", strategy2Cycles);
    }

    /**
     * Monitor Strategy 1 for specific symbol
     */
    private void monitorStrategy1(String symbol, String timestamp) {
        try {
            logger.debug("🔍 Strategy 1 checking {}...", symbol);
            
            // Get current price
            Double currentPrice = getCurrentPrice(symbol);
            if (currentPrice == null) {
                logger.warn("❌ Could not fetch price for {}", symbol);
                return;
            }
            
            logger.debug("💰 {} current price: ${}", symbol, String.format("%.2f", currentPrice));
            
            // Check for existing position
            if (positionService.hasOpenPosition(symbol, "BUY") || positionService.hasOpenPosition(symbol, "SELL")) {
                logger.debug("📋 {} already has open position - skipping new signals", symbol);
                return;
            }
            
            // Get historical data for Strategy 1 (200-Day MA needs daily data)
            logger.debug("📊 Fetching historical data for {} (Strategy 1 needs 250 days)", symbol);
            Map<String, Object> response = candlestickService.getCandlestickData(symbol, 250, "1d");
            if (response == null || !response.containsKey("result")) {
                logger.warn("❌ Insufficient historical data for {}", symbol);
                return;
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> candles = (List<Map<String, Object>>) response.get("result");
            if (candles == null || candles.size() < 200) {
                logger.warn("❌ Insufficient historical data for {} (need at least 200 days, got {})", symbol, candles != null ? candles.size() : 0);
                return;
            }
            
            logger.debug("✅ Retrieved {} days of historical data for {}", candles.size(), symbol);
            
            // Add data to Strategy 1
            logger.debug("📈 Adding {} candles to Strategy 1 for {}", candles.size(), symbol);
            for (Map<String, Object> candle : candles) {
                MovingAverageStrategy.CandleData data = new MovingAverageStrategy.CandleData(
                    Long.parseLong(candle.get("timestamp").toString()),
                    Double.parseDouble(candle.get("open").toString()),
                    Double.parseDouble(candle.get("high").toString()),
                    Double.parseDouble(candle.get("low").toString()),
                    Double.parseDouble(candle.get("close").toString()),
                    Double.parseDouble(candle.get("volume").toString())
                );
                strategy1.addCandleData(data);
            }
            
            // Identify trendlines
            logger.debug("📊 Identifying trendlines for {}", symbol);
            strategy1.identifyTrendlines();
            
            // Check for signals
            logger.debug("🔍 Checking for Strategy 1 breakout signals for {}", symbol);
            List<MovingAverageStrategy.TradeSignal> signals = strategy1.checkBreakoutSignals();
            
            if (!signals.isEmpty()) {
                logger.info("🚨 STRATEGY 1 SIGNAL DETECTED: {} signals for {}", signals.size(), symbol);
                for (MovingAverageStrategy.TradeSignal signal : signals) {
                    logger.info("🚨 STRATEGY 1 SIGNAL: {} {} at ${}", signal.getType(), symbol, signal.getEntryPrice());
                    processStrategy1Signal(symbol, signal, timestamp);
                }
            } else {
                logger.debug("ℹ️ No Strategy 1 signals for {}", symbol);
            }
            
        } catch (Exception e) {
            logger.error("❌ Error monitoring Strategy 1 for {}", symbol, e);
            logger.error("Error details: {}", e.getMessage(), e);
        }
    }

    /**
     * Monitor Strategy 2 for specific symbol
     */
    private void monitorStrategy2(String symbol, String timestamp) {
        try {
            logger.debug("🔍 Strategy 2 checking {}...", symbol);
            
            // Get current price
            Double currentPrice = getCurrentPrice(symbol);
            if (currentPrice == null) {
                logger.warn("❌ Could not fetch price for {}", symbol);
                return;
            }
            
            logger.debug("💰 {} current price: ${}", symbol, String.format("%.2f", currentPrice));
            
            // Check for existing Strategy 2 position
            if (strategy2PositionService.hasOpenPosition(symbol)) {
                logger.debug("📋 {} already has open Strategy 2 position - skipping new signals", symbol);
                return;
            }
            
            // Get historical data for Strategy 2 using the existing service
            logger.debug("📊 Fetching historical data for {} (Strategy 2 needs 50 candles)", symbol);
            Map<String, Object> response = candlestickService.getCandlestickData(symbol, 50, "15m");
            
            if (response == null || !response.containsKey("result")) {
                logger.warn("❌ Could not fetch historical data for {}", symbol);
                return;
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> candles = (List<Map<String, Object>>) response.get("result");
            if (candles == null || candles.size() < 20) {
                logger.warn("❌ Insufficient historical data for {} (need at least 20 candles, got {})", 
                    symbol, candles != null ? candles.size() : 0);
                return;
            }
            
            logger.debug("✅ Retrieved {} candles for {} Strategy 2 analysis", candles.size(), symbol);
            
            // Add data to Strategy 2
            strategy2.addCandleData(symbol, candles);
            
            // Get current trend and market condition
            String trend = strategy2.getCurrentTrend(symbol);
            boolean isTrending = strategy2.isTrendingMarket(symbol);
            Map<String, Double> emaValues = strategy2.getCurrentEMAValues(symbol);
            
            logger.debug("📈 {} - Trend: {}, Trending Market: {}", symbol, trend, isTrending ? "✅" : "❌");
            if (emaValues != null && emaValues.containsKey("EMA9") && emaValues.containsKey("EMA20")) {
                logger.debug("📊 EMA9: ${}, EMA20: ${}", 
                    String.format("%.2f", emaValues.get("EMA9")), 
                    String.format("%.2f", emaValues.get("EMA20")));
            }
            
            // Only trade in trending markets (avoid ranging markets)
            if (!isTrending) {
                logger.debug("⚠️ {} is in ranging market - Strategy 2 avoids these conditions", symbol);
                return;
            }
            
            // Check for EMA crossover signals
            logger.debug("🔍 Checking for EMA crossover signals for {}", symbol);
            List<EMAStrategy.TradeSignal> crossoverSignals = strategy2.checkEMASignalsForSymbol(symbol);
            if (!crossoverSignals.isEmpty()) {
                logger.info("🚨 STRATEGY 2 CROSSOVER SIGNAL DETECTED: {} signals for {}", crossoverSignals.size(), symbol);
                for (EMAStrategy.TradeSignal signal : crossoverSignals) {
                    logger.info("🚨 STRATEGY 2 CROSSOVER: {} {} at ${}", signal.getType(), symbol, signal.getEntryPrice());
                    processStrategy2Signal(signal, timestamp);
                }
            }
            
            // Check for pullback entry signals
            logger.debug("🔍 Checking for pullback entry signals for {}", symbol);
            List<EMAStrategy.TradeSignal> pullbackSignals = strategy2.checkPullbackEntriesForSymbol(symbol);
            if (!pullbackSignals.isEmpty()) {
                logger.info("🚨 STRATEGY 2 PULLBACK SIGNAL DETECTED: {} signals for {}", pullbackSignals.size(), symbol);
                for (EMAStrategy.TradeSignal signal : pullbackSignals) {
                    logger.info("🚨 STRATEGY 2 PULLBACK: {} {} at ${}", signal.getType(), symbol, signal.getEntryPrice());
                    processStrategy2Signal(signal, timestamp);
                }
            }
            
            if (crossoverSignals.isEmpty() && pullbackSignals.isEmpty()) {
                logger.debug("ℹ️ No Strategy 2 signals for {}", symbol);
            }
            
        } catch (Exception e) {
            logger.error("❌ Error monitoring Strategy 2 for {}", symbol, e);
            logger.error("Error details: {}", e.getMessage(), e);
        }
    }

    /**
     * Process Strategy 1 signal
     */
    private void processStrategy1Signal(String symbol, MovingAverageStrategy.TradeSignal signal, String timestamp) {
        try {
            logger.info("🎯 Processing Strategy 1 signal for {}: {} at ${}", symbol, signal.getType(), signal.getEntryPrice());
            
            // Open position
            logger.debug("Opening Strategy 1 position for {}", symbol);
            positionService.openPosition(
                symbol,
                signal.getType().toString(),
                signal.getEntryPrice(),
                signal.getStopLoss(),
                signal.getTakeProfit(),
                signal.getReason()
            );
            logger.debug("✅ Strategy 1 position opened for {}", symbol);
            
            // Send enhanced notification
            logger.debug("Sending Strategy 1 breakout alert for {}", symbol);
            alertVerificationService.sendStrategy1BreakoutAlert(
                symbol,
                signal.getType().toString(),
                signal.getEntryPrice(),
                signal.getStopLoss(),
                signal.getTakeProfit(),
                signal.getReason()
            );
            logger.info("✅ Strategy 1 position opened and notification sent for {}", symbol);
            
        } catch (Exception e) {
            logger.error("❌ Error processing Strategy 1 signal for {}", symbol, e);
            logger.error("Error details: {}", e.getMessage(), e);
        }
    }

    /**
     * Process Strategy 2 signal
     */
    private void processStrategy2Signal(EMAStrategy.TradeSignal signal, String timestamp) {
        try {
            logger.info("⚡ Processing Strategy 2 signal for {}: {} at ${}", signal.getSymbol(), signal.getType(), signal.getEntryPrice());
            
            // Get current EMA values for storage
            logger.debug("Getting EMA values and market conditions for {}", signal.getSymbol());
            Map<String, Double> emaValues = strategy2.getCurrentEMAValues(signal.getSymbol());
            String trendDirection = strategy2.getCurrentTrend(signal.getSymbol());
            boolean isTrending = strategy2.isTrendingMarket(signal.getSymbol());
            String marketCondition = isTrending ? "TRENDING" : "SIDEWAYS";
            
            logger.debug("EMA9: {}, EMA20: {}, Trend: {}, Market: {}", 
                emaValues.getOrDefault("EMA9", 0.0), emaValues.getOrDefault("EMA20", 0.0), trendDirection, marketCondition);
            
            // Open Strategy 2 position (stores in crypto_strategy2 table)
            logger.debug("Opening Strategy 2 position for {}", signal.getSymbol());
            CryptoStrategy2 strategy2Trade = strategy2PositionService.openStrategy2Position(
                signal.getSymbol(),
                signal.getType(),
                signal.getEntryPrice(),
                signal.getStopLoss(),
                signal.getTakeProfit(),
                signal.getReason(),
                emaValues.getOrDefault("EMA9", 0.0),
                emaValues.getOrDefault("EMA20", 0.0),
                trendDirection,
                marketCondition
            );
            
            if (strategy2Trade != null) {
                logger.debug("✅ Strategy 2 position opened with ID: {}", strategy2Trade.getId());
                
                // Send enhanced notification
                logger.debug("Sending Strategy 2 EMA alert for {}", signal.getSymbol());
                alertVerificationService.sendStrategy2EMAAlert(
                    signal.getSymbol(),
                    signal.getType(),
                    signal.getEntryPrice(),
                    signal.getStopLoss(),
                    signal.getTakeProfit(),
                    emaValues.getOrDefault("EMA9", 0.0),
                    emaValues.getOrDefault("EMA20", 0.0),
                    trendDirection,
                    marketCondition,
                    signal.getReason()
                );
                logger.info("✅ Strategy 2 position opened in crypto_strategy2 table and notification sent for {}", signal.getSymbol());
            } else {
                logger.warn("⚠️ Strategy 2 position not opened - likely duplicate position exists for {}", signal.getSymbol());
            }
            
        } catch (Exception e) {
            logger.error("❌ Error processing Strategy 2 signal for {}", signal.getSymbol(), e);
            logger.error("Error details: {}", e.getMessage(), e);
        }
    }

    /**
     * Get current price for symbol
     */
    private Double getCurrentPrice(String symbol) {
        try {
            logger.debug("Fetching current price for {}", symbol);
            Double price = null;
            
            switch (symbol) {
                case "BTCUSD":
                    price = priceService.getBitcoinPrice();
                    break;
                case "ETHUSD":
                    price = priceService.getEthereumPrice();
                    break;
                case "SOLUSD":
                    price = priceService.getSolanaPrice();
                    break;
                default:
                    logger.warn("Unknown symbol: {}", symbol);
                    return null;
            }
            
            if (price != null) {
                logger.debug("✅ Price fetched for {}: ${}", symbol, String.format("%.2f", price));
            } else {
                logger.warn("❌ Price service returned null for {}", symbol);
            }
            
            return price;
        } catch (Exception e) {
            logger.error("❌ Error fetching price for {}", symbol, e);
            logger.error("Error details: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Enable/disable Strategy 1
     */
    public void setStrategy1Enabled(boolean enabled) {
        this.strategy1Enabled = enabled;
        logger.info("🎯 Strategy 1 {}", enabled ? "ENABLED" : "DISABLED");
    }

    /**
     * Enable/disable Strategy 2
     */
    public void setStrategy2Enabled(boolean enabled) {
        this.strategy2Enabled = enabled;
        logger.info("⚡ Strategy 2 {}", enabled ? "ENABLED" : "DISABLED");
    }

    /**
     * Get scheduler status
     */
    public String getSchedulerStatus() {
        return String.format("""
            📊 *Dual Strategy Scheduler Status*
            
            🎯 *Strategy 1:* %s (Cycles: %d)
            ⚡ *Strategy 2:* %s (Cycles: %d)
            
            📈 *Symbols:* %s
            
            ✅ *System:* Active
            """,
            strategy1Enabled ? "ENABLED" : "DISABLED", strategy1Cycles,
            strategy2Enabled ? "ENABLED" : "DISABLED", strategy2Cycles,
            String.join(", ", SYMBOLS_TO_MONITOR)
        );
    }
}
