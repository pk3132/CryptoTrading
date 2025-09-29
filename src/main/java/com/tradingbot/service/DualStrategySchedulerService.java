package com.tradingbot.service;

import com.tradingbot.strategy.EMA200TrendlineStrategy;
import com.tradingbot.service.DeltaApiClient;
import com.tradingbot.model.Trade;
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
 * EMA 200 + Trendline Breakout Strategy Scheduler Service
 * 
 * Runs EMA 200 + Trendline Breakout Strategy: Every 5 minutes
 * BUY: Price > EMA200 + breaks above descending resistance line
 * SELL: Price < EMA200 + breaks below ascending support line
 */
@Service
@EnableScheduling
public class DualStrategySchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(DualStrategySchedulerService.class);

    @Autowired
    private EMA200TrendlineStrategy ema200TrendlineStrategy;
    
    @Autowired
    private DeltaApiClient deltaApiClient;
    
    @Autowired
    private PositionManagementService positionService;
    
    @Autowired
    private AlertVerificationService alertVerificationService;
    
    // Strategy monitoring flags
    private boolean strategyEnabled = true;
    
    // Monitoring counters
    private int strategyCycles = 0;
    
    // Symbols to monitor
    private static final String[] SYMBOLS_TO_MONITOR = {"BTCUSD", "ETHUSD"};
    private static final String TIMEFRAME = "1m";
    // EMA 200 calculation candles (500 candles for both)
    private static final int BTC_EMA_CANDLES = 500;
    private static final int ETH_EMA_CANDLES = 500;
    
    // Trendline calculation candles (500 candles for better analysis)
    private static final int TRENDLINE_CANDLES = 500;

    /**
     * EMA 200 + Trendline Breakout Strategy
     * Runs every 5 minutes - BUY/SELL based on EMA200 and trendline breakouts
     */
    @Scheduled(fixedRate = 300000) // 5 minutes = 300,000 milliseconds
    public void runEMA200TrendlineStrategy() {
        if (!strategyEnabled) {
            logger.debug("EMA 200 + Trendline Strategy is disabled - skipping execution");
            return;
        }
        
        strategyCycles++;
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        logger.info("🎯 EMA 200 + TRENDLINE STRATEGY EXECUTION #{} - {}", strategyCycles, timestamp);
        logger.info("📊 Strategy: EMA 200 + Trendline Breakout");
        logger.info("📈 Timeframe: 15 minutes");
        logger.info("⚡ Risk-Reward: 1:2 (0.5% SL, 1.0% TP)");
        logger.info("🔍 Features: EMA 200 Filter, Swing Points, Trendline Breakouts");
        logger.debug("Monitoring symbols: {}", String.join(", ", SYMBOLS_TO_MONITOR));
        
        try {
            for (String symbol : SYMBOLS_TO_MONITOR) {
                logger.debug("Processing EMA 200 + Trendline Strategy for symbol: {}", symbol);
                monitorEMA200TrendlineStrategy(symbol, timestamp);
            }
        } catch (Exception e) {
            logger.error("❌ Error in EMA 200 + Trendline Strategy execution", e);
            logger.error("Error details: {}", e.getMessage(), e);
        }
        
        logger.info("✅ EMA 200 + Trendline Strategy cycle #{} completed", strategyCycles);
    }


    /**
     * Monitor EMA 200 + Trendline Strategy for specific symbol
     */
    private void monitorEMA200TrendlineStrategy(String symbol, String timestamp) {
        try {
            logger.debug("🔍 EMA 200 + Trendline Strategy checking {}...", symbol);
            
            // Check for existing position
            if (positionService.hasOpenPosition(symbol, "BUY") || positionService.hasOpenPosition(symbol, "SELL")) {
                logger.debug("📋 {} already has open position - skipping new signals", symbol);
                return;
            }
            
            // Check if we need to wait for new trendline formation after profitable trade
            if (positionService.isInCooldown(symbol)) {
                String statusMsg = positionService.getTrendlineStatusMessage(symbol);
                logger.info("🛡️ {} : {}", symbol, statusMsg);
                return;
            }
            
            // Get EMA candles for each symbol (optimized for chart matching)
            int emaCandles = symbol.equals("BTCUSD") ? BTC_EMA_CANDLES : ETH_EMA_CANDLES;
            
            // Get historical data for EMA 200 calculation (1m candles)
            logger.debug("📊 Fetching historical data for {} (EMA 200 needs {} candles)", symbol, emaCandles);
            long now = System.currentTimeMillis() / 1000;
            long start = now - (emaCandles * 60); // 1 minute per candle
            
            List<Map<String, Object>> candles = deltaApiClient.fetchOhlcv(symbol, TIMEFRAME, start, now);
            if (candles == null || candles.isEmpty()) {
                logger.warn("❌ No historical data received for {}", symbol);
                return;
            }
            
            logger.debug("✅ Retrieved {} candles for {} EMA 200 analysis", candles.size(), symbol);
            
            // Add EMA data to strategy
            ema200TrendlineStrategy.addCandleData(symbol, candles);
            
            // Since both EMA and trendline use 500 candles, no additional fetch needed
            logger.debug("✅ Using same 500 candles for both EMA 200 and trendline analysis for {}", symbol);

            // Pre-trade gate: compare current MARK price to EMA-200
            Double ema200 = ema200TrendlineStrategy.getLastEma200(symbol);
            Double currentMark = deltaApiClient.getCurrentMarkPrice(symbol);
            if (ema200 == null || currentMark == null) {
                logger.warn("⚠️ Skipping {}: missing EMA or current mark (ema200={}, mark={})", symbol, ema200, currentMark);
                return;
            }
            logger.debug("📏 Pre-trade check for {} -> mark: {} vs EMA200: {}", symbol, currentMark, ema200);
            
            // Check for signals
            logger.debug("🔍 Checking for EMA 200 + Trendline signals for {}", symbol);
            List<EMA200TrendlineStrategy.TradeSignal> signals = ema200TrendlineStrategy.checkSignals(symbol);
            
            // Check if we need to monitor trendlines for breakout tracking
            checkTrendlineFormationStatus(symbol);
            
            if (!signals.isEmpty()) {
                logger.info("🚨 EMA 200 + TRENDLINE SIGNAL DETECTED: {} signals for {}", signals.size(), symbol);
                for (EMA200TrendlineStrategy.TradeSignal signal : signals) {
                    // Enforce EMA side: BUY only if current >= EMA, SELL only if current <= EMA
                    boolean sideOkay = ("BUY".equals(signal.getType()) && currentMark >= ema200) ||
                                       ("SELL".equals(signal.getType()) && currentMark <= ema200);
                    logger.info("🚨 SIGNAL: {} {} at ${} - {} | gate: {} (mark {} EMA)",
                            signal.getType(), symbol, signal.getEntryPrice(), signal.getReason(),
                            sideOkay ? "PASS" : "BLOCK",
                            currentMark >= ema200 ? ">=" : "<");
                    if (sideOkay) {
                        processEMA200TrendlineSignal(symbol, signal, timestamp);
                    } else {
                        logger.info("⛔ Blocked {} signal for {} due to EMA side mismatch (mark={}, ema200={})",
                                signal.getType(), symbol, currentMark, ema200);
                    }
                }
            } else {
                logger.debug("ℹ️ No EMA 200 + Trendline signals for {}", symbol);
            }
            
        } catch (Exception e) {
            logger.error("❌ Error monitoring EMA 200 + Trendline Strategy for {}", symbol, e);
            logger.error("Error details: {}", e.getMessage(), e);
        }
    }


    /**
     * Process EMA 200 + Trendline signal
     */
    private void processEMA200TrendlineSignal(String symbol, EMA200TrendlineStrategy.TradeSignal signal, String timestamp) {
        try {
            logger.info("🎯 Processing EMA 200 + Trendline signal for {}: {} at ${} - {}", symbol, signal.getType(), signal.getEntryPrice(), signal.getReason());
            
            // Open position
            logger.debug("Opening EMA 200 + Trendline position for {}", symbol);
            var newTrade = positionService.openPosition(
                symbol,
                signal.getType(),
                signal.getEntryPrice(),
                signal.getStopLoss(),
                signal.getTakeProfit(),
                signal.getReason()
            );
            
            if (newTrade == null) {
                logger.info("🛡️ Position opening blocked due to cooldown - waiting for better trendline setup");
                return;
            }
            logger.debug("✅ EMA 200 + Trendline position opened for {}", symbol);
            
            // Send notification
            logger.debug("Sending EMA 200 + Trendline alert for {}", symbol);
            alertVerificationService.sendEMA200TrendlineAlert(
                symbol,
                signal.getType(),
                signal.getEntryPrice(),
                signal.getStopLoss(),
                signal.getTakeProfit(),
                signal.getReason()
            );
            logger.info("✅ EMA 200 + Trendline position opened and notification sent for {}", symbol);
            
        } catch (Exception e) {
            logger.error("❌ Error processing EMA 200 + Trendline signal for {}", symbol, e);
            logger.error("Error details: {}", e.getMessage(), e);
        }
    }


    /**
     * Enable/disable Dynamic Chart Strategy
     */
    public void setStrategyEnabled(boolean enabled) {
        this.strategyEnabled = enabled;
        logger.info("🎯 Dynamic Chart Strategy {}", enabled ? "ENABLED" : "DISABLED");
    }

    /**
     * Check trendline formation state and update position management 
     */
    private void checkTrendlineFormationStatus(String symbol) {
        try {
            // Get recent signals that show trendline formation/breakout
            List<EMA200TrendlineStrategy.TradeSignal> recentSignals = ema200TrendlineStrategy.checkSignals(symbol);
            
            for (EMA200TrendlineStrategy.TradeSignal signal : recentSignals) {
                String trendlineType = "BUY".equals(signal.getType()) ? "Resistance" : "Support";
                
                // Check if current price is near breakout level - indicates trendline formation  
                Double currentPrice = deltaApiClient.getCurrentMarkPrice(symbol);
                Double entryPrice = signal.getEntryPrice();
                
                if (currentPrice != null && entryPrice != null) {
                    double priceDiff = Math.abs(currentPrice - entryPrice) / entryPrice;
                    
                    // If very close to trendline (within 0.5%), consider it formed and check if fresh vs last
                    if (priceDiff <= 0.005) {
                        boolean isBreaking = "BUY".equals(signal.getType()) 
                            ? currentPrice > entryPrice 
                            : currentPrice < entryPrice;
                        
                        // Detect if this is a meaningful change (not just same level)
                        boolean isBreakingFreshly = isBreaking && priceDiff <= 0.001; // Close tolerance for break
                        
                        logger.info("📊 {}: {} trendline detected at {} - {}",
                                   symbol, trendlineType, entryPrice, 
                                   isBreakingFreshly ? "🎯 FRESH BREAKOUT" : "📈 TRENDLINE");
                        
                        positionService.updateTrendlineFormation(symbol, trendlineType, 
                                                               entryPrice, isBreakingFreshly);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("⚠️ Error checking trendline status for {}: {}", symbol, e.getMessage());
        }
    }

    /**
     * Get scheduler status
     */
    public String getSchedulerStatus() {
        return String.format("""
                📊 *EMA 200 + Trendline Strategy Status*
                
                🎯 *Strategy:* %s (Cycles: %d)
                📈 *Timeframe:* 1 minute
                ⚡ *Risk-Reward:* 1:2 (0.5%% SL, 1.0%% TP)
                
                🔍 *Strategy Features:*
                • EMA 200 Trend Filter (BTC: 500, ETH: 500 candles)
                • Trendline Breakouts (500 candles)
                • Swing Point Detection
                • Support/Resistance Levels
                • Real-Time Entry Prices
                • Swing Point Based SL/TP (Last 15 Min)
                
                📈 *Symbols:* %s
                
                ✅ *System:* Active
                """,
                strategyEnabled ? "ENABLED" : "DISABLED", strategyCycles,
                String.join(", ", SYMBOLS_TO_MONITOR)
            );
    }
}
