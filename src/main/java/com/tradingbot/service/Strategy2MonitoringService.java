package com.tradingbot.service;

import com.tradingbot.strategy.EMAStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Strategy 2 Monitoring Service - 9/20 EMA Crossover
 * 
 * Monitors markets for Strategy 2 signals:
 * - EMA crossovers
 * - Pullback entries
 * - Trending vs ranging market detection
 */
@Service
public class Strategy2MonitoringService {

    @Autowired
    private EMAStrategy emaStrategy;
    
    
    @Autowired
    private PositionManagementService positionService;
    
    @Autowired
    private TelegramNotificationService telegramService;
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private boolean isMonitoring = false;
    private int monitoringCycle = 0;
    
    // Strategy 2 settings
    private static final String[] SYMBOLS_TO_MONITOR = {"BTCUSD", "ETHUSD", "SOLUSD"};
    private static final int MONITORING_INTERVAL_MINUTES = 5; // Check every 5 minutes
    private static final int CANDLES_TO_FETCH = 50; // Need 50 candles for EMA calculations
    
    /**
     * Start Strategy 2 monitoring
     */
    public void startMonitoring() {
        if (isMonitoring) {
            System.out.println("⚠️ Strategy 2 monitoring is already running!");
            return;
        }
        
        System.out.println("🚀 Starting Strategy 2 Monitoring (9/20 EMA Crossover)");
        System.out.println("📊 Monitoring: " + String.join(", ", SYMBOLS_TO_MONITOR) + " every " + MONITORING_INTERVAL_MINUTES + " minutes");
        System.out.println("⚡ Strategy: 1:3 Risk-Reward, 25x Leverage");
        System.out.println("🎯 Focus: Trending markets, EMA crossovers & pullbacks");
        
        isMonitoring = true;
        monitoringCycle = 0;
        
        // Start monitoring every 5 minutes
        scheduler.scheduleAtFixedRate(this::monitorStrategy2Signals, 0, MONITORING_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }
    
    /**
     * Stop Strategy 2 monitoring
     */
    public void stopMonitoring() {
        if (!isMonitoring) {
            System.out.println("⚠️ Strategy 2 monitoring is not running!");
            return;
        }
        
        System.out.println("🛑 Stopping Strategy 2 monitoring...");
        isMonitoring = false;
        scheduler.shutdown();
    }
    
    /**
     * Monitor for Strategy 2 signals
     */
    private void monitorStrategy2Signals() {
        monitoringCycle++;
        String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        System.out.println("\n🔍 Strategy 2 Signal Check #" + monitoringCycle + " - " + timestamp);
        
        for (String symbol : SYMBOLS_TO_MONITOR) {
            try {
                monitorSymbol(symbol, timestamp);
            } catch (Exception e) {
                System.err.println("❌ Error monitoring " + symbol + ": " + e.getMessage());
            }
        }
        
        System.out.println("✅ Strategy 2 monitoring cycle completed\n");
    }
    
    /**
     * Monitor individual symbol for Strategy 2 signals
     */
    private void monitorSymbol(String symbol, String timestamp) {
        System.out.println("📊 Checking " + symbol + " for Strategy 2 signals...");
        
        try {
            // Fetch historical data for EMA calculations
            List<Map<String, Object>> candles = getHistoricalData(symbol, CANDLES_TO_FETCH);
            
            if (candles == null || candles.size() < 20) {
                System.out.println("⚠️ Not enough data for " + symbol + " (need at least 20 candles)");
                return;
            }
            
            // Add data to Strategy 2
            emaStrategy.addCandleData(candles);
            
            // Get current trend and market condition
            String trend = emaStrategy.getCurrentTrend(symbol);
            boolean isTrending = emaStrategy.isTrendingMarket(symbol);
            Map<String, Double> emaValues = emaStrategy.getCurrentEMAValues(symbol);
            
            System.out.println("📈 " + symbol + " - Trend: " + trend + ", Trending Market: " + (isTrending ? "✅" : "❌"));
            if (emaValues.containsKey("EMA9") && emaValues.containsKey("EMA20")) {
                System.out.println("📊 EMA9: $" + String.format("%.2f", emaValues.get("EMA9")) + 
                                 ", EMA20: $" + String.format("%.2f", emaValues.get("EMA20")));
            }
            
            // Only trade in trending markets (avoid ranging markets)
            if (!isTrending) {
                System.out.println("⚠️ " + symbol + " is in ranging market - Strategy 2 avoids these conditions");
                return;
            }
            
            // Check for existing position
            if (positionService.hasOpenPosition(symbol, "BUY") || positionService.hasOpenPosition(symbol, "SELL")) {
                System.out.println("📋 " + symbol + " already has open position - skipping new signals");
                return;
            }
            
            // Check for EMA crossover signals
            List<EMAStrategy.TradeSignal> crossoverSignals = emaStrategy.checkEMASignals();
            if (!crossoverSignals.isEmpty()) {
                for (EMAStrategy.TradeSignal signal : crossoverSignals) {
                    if (symbol.equals(signal.getSymbol())) {
                        System.out.println("🎯 STRATEGY 2 SIGNAL DETECTED: " + signal.getType() + " " + symbol);
                        openPositionFromSignal(signal, timestamp);
                    }
                }
            }
            
            // Check for pullback entry signals
            List<EMAStrategy.TradeSignal> pullbackSignals = emaStrategy.checkPullbackEntries();
            if (!pullbackSignals.isEmpty()) {
                for (EMAStrategy.TradeSignal signal : pullbackSignals) {
                    if (symbol.equals(signal.getSymbol())) {
                        System.out.println("🎯 STRATEGY 2 PULLBACK SIGNAL: " + signal.getType() + " " + symbol);
                        openPositionFromSignal(signal, timestamp);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error processing " + symbol + ": " + e.getMessage());
        }
    }
    
    /**
     * Open position from Strategy 2 signal
     */
    private void openPositionFromSignal(EMAStrategy.TradeSignal signal, String timestamp) {
        try {
            // Open position with Strategy 2 parameters
            positionService.openPosition(
                signal.getSymbol(),
                signal.getType(),
                signal.getEntryPrice(),
                signal.getStopLoss(),
                signal.getTakeProfit(),
                signal.getReason()
            );
            
            // Send Strategy 2 notification
            String message = String.format("""
                🚀 *Strategy 2 Signal - 9/20 EMA*
                
                📊 *Symbol:* %s
                🎯 *Action:* %s
                💰 *Entry:* $%.2f
                🛡️ *Stop Loss:* $%.2f
                🎯 *Take Profit:* $%.2f
                ⚡ *Leverage:* %.0fx
                📈 *Risk-Reward:* 1:3
                
                💡 *Strategy:* %s
                ⏰ *Time:* %s
                """,
                signal.getSymbol(),
                signal.getType(),
                signal.getEntryPrice(),
                signal.getStopLoss(),
                signal.getTakeProfit(),
                signal.getLeverage(),
                signal.getReason(),
                timestamp
            );
            
            telegramService.sendTelegramMessage(message);
            System.out.println("✅ Strategy 2 position opened and notification sent for " + signal.getSymbol());
            
        } catch (Exception e) {
            System.err.println("❌ Error opening Strategy 2 position: " + e.getMessage());
        }
    }
    
    /**
     * Get historical candlestick data for Strategy 2 analysis
     */
    private List<Map<String, Object>> getHistoricalData(String symbol, int candleCount) {
        try {
            // Calculate timestamps for the required period
            long endTime = System.currentTimeMillis() / 1000;
            long startTime = endTime - (candleCount * 15 * 60); // 15 minutes per candle
            
            String url = "https://api.india.delta.exchange/v2/history/candles?symbol=" + symbol + 
                        "&resolution=15m&start=" + startTime + "&end=" + endTime;
            
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
                if (responseBody.containsKey("result") && responseBody.get("result") instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> result = (List<Map<String, Object>>) responseBody.get("result");
                    return result;
                }
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("❌ Error fetching historical data for " + symbol + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Get Strategy 2 status
     */
    public String getStrategy2Status() {
        if (!isMonitoring) {
            return "❌ Strategy 2 monitoring is not active";
        }
        
        StringBuilder status = new StringBuilder();
        status.append("✅ Strategy 2 Monitoring Active\n");
        status.append("📊 Symbols: ").append(String.join(", ", SYMBOLS_TO_MONITOR)).append("\n");
        status.append("⏰ Interval: ").append(MONITORING_INTERVAL_MINUTES).append(" minutes\n");
        status.append("🎯 Strategy: 9/20 EMA Crossover\n");
        status.append("⚡ Risk-Reward: 1:3, Leverage: 25x\n");
        status.append("📈 Focus: Trending markets only\n");
        status.append("🔄 Cycles: ").append(monitoringCycle);
        
        return status.toString();
    }
}
