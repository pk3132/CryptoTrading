package com.tradingbot.service;

import com.tradingbot.strategy.EMA200TrendlineStrategy;
import com.tradingbot.service.DeltaApiClient;
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
 * Runs EMA 200 + Trendline Breakout Strategy: Every 15 minutes
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
    private static final String[] SYMBOLS_TO_MONITOR = {"BTCUSD", "ETHUSD", "SOLUSD"};
    private static final String TIMEFRAME = "15m";
    private static final int CANDLES_TO_FETCH = 500;

    /**
     * EMA 200 + Trendline Breakout Strategy
     * Runs every 15 minutes - BUY/SELL based on EMA200 and trendline breakouts
     */
    @Scheduled(fixedRate = 900000) // 15 minutes = 900,000 milliseconds
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
        logger.info("⚡ Risk-Reward: 1:2 (2% SL, 4% TP)");
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
            
            // Get historical data for EMA 200 + Trendline Strategy (15m candles)
            logger.debug("📊 Fetching historical data for {} (EMA 200 + Trendline needs {} candles)", symbol, CANDLES_TO_FETCH);
            long now = System.currentTimeMillis() / 1000;
            long start = now - (CANDLES_TO_FETCH * 15 * 60); // 15 minutes per candle
            
            List<Map<String, Object>> candles = deltaApiClient.fetchOhlcv(symbol, TIMEFRAME, start, now);
            if (candles == null || candles.isEmpty()) {
                logger.warn("❌ No historical data received for {}", symbol);
                return;
            }
            
            logger.debug("✅ Retrieved {} candles for {} EMA 200 + Trendline analysis", candles.size(), symbol);
            
            // Add data to strategy
            ema200TrendlineStrategy.addCandleData(symbol, candles);

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
            positionService.openPosition(
                symbol,
                signal.getType(),
                signal.getEntryPrice(),
                signal.getStopLoss(),
                signal.getTakeProfit(),
                signal.getReason()
            );
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
     * Get scheduler status
     */
    public String getSchedulerStatus() {
        return String.format("""
                📊 *Dynamic Chart Strategy Status*
                
                🎯 *Strategy:* %s (Cycles: %d)
                📈 *Timeframe:* 15 minutes
                ⚡ *Risk-Reward:* 1:2
                
                🔍 *Dynamic Features:*
                • EMA 200 Trend Filter (Auto Detection)
                • Bollinger Bands (Volatility)
                • Support/Resistance Levels
                • Candlestick Patterns (Green/Red)
                • Trend Analysis (SMA5)
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
