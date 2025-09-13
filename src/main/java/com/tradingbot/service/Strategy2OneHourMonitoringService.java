package com.tradingbot.service;

import com.tradingbot.strategy.EMAStrategy;
import com.tradingbot.repository.CryptoStrategy2Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Strategy 2 One-Hour Monitoring Service
 * 
 * Monitors markets for Strategy 2 signals using 1-hour timeframe
 * - EMA crossovers
 * - Pullback entries
 * - Trending vs ranging market detection
 */
@Service
public class Strategy2OneHourMonitoringService {

    @Autowired
    private EMAStrategy emaStrategy;
    
    @Autowired
    private Strategy2PositionService positionService;
    
    
    @Autowired
    private CryptoStrategy2Repository strategy2Repository;
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private boolean isMonitoring = false;
    private int monitoringCycle = 0;
    
    // Strategy 2 1-hour settings
    private static final String[] SYMBOLS_TO_MONITOR = {"BTCUSD", "ETHUSD", "SOLUSD"};
    private static final int MONITORING_INTERVAL_MINUTES = 5; // Check every 5 minutes (ultra-fast signal detection)
    private static final int CANDLES_TO_FETCH = 50; // Need 50 candles for EMA calculations

    /**
     * Start Strategy 2 1-hour monitoring
     */
    public void startMonitoring() {
        if (isMonitoring) {
            System.out.println("⚠️ Strategy 2 1-hour monitoring is already running!");
            return;
        }
        
        System.out.println("🚀 Starting Strategy 2 1-Hour Monitoring (9/20 EMA Crossover)");
        System.out.println("📊 Monitoring: " + String.join(", ", SYMBOLS_TO_MONITOR) + " every " + MONITORING_INTERVAL_MINUTES + " minutes (Ultra-Fast Signal Detection)");
        System.out.println("⚡ Strategy: 1:2 Risk-Reward, 5x Leverage");
        System.out.println("🎯 Timeframe: 1-Hour (Optimal for Strategy 2)");
        System.out.println("📈 Focus: Trending markets, EMA crossovers & pullbacks");
        
        isMonitoring = true;
        monitoringCycle = 0;
        
        // Start monitoring every 5 minutes (ultra-fast signal detection)
        scheduler.scheduleAtFixedRate(this::monitorStrategy2Signals, 0, MONITORING_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }
    
    /**
     * Stop Strategy 2 1-hour monitoring
     */
    public void stopMonitoring() {
        if (!isMonitoring) {
            System.out.println("⚠️ Strategy 2 1-hour monitoring is not running!");
            return;
        }
        
        System.out.println("🛑 Stopping Strategy 2 1-hour monitoring...");
        isMonitoring = false;
        scheduler.shutdown();
    }
    
    /**
     * Monitor for Strategy 2 signals using 1-hour timeframe
     */
    private void monitorStrategy2Signals() {
        monitoringCycle++;
        String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        System.out.println("\n🔍 Strategy 2 1-Hour Signal Check #" + monitoringCycle + " - " + timestamp);
        
        for (String symbol : SYMBOLS_TO_MONITOR) {
            try {
                monitorSymbol(symbol, timestamp);
            } catch (Exception e) {
                System.err.println("❌ Error monitoring " + symbol + ": " + e.getMessage());
            }
        }
        
        System.out.println("✅ Strategy 2 1-hour monitoring cycle completed\n");
    }
    
    /**
     * Monitor individual symbol for Strategy 2 signals using 1-hour timeframe
     */
    private void monitorSymbol(String symbol, String timestamp) {
        System.out.println("📊 Checking " + symbol + " for Strategy 2 signals (1H timeframe)...");
        
        try {
            // Fetch historical data for EMA calculations (1-hour timeframe)
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
            if (positionService.hasOpenPosition(symbol)) {
                System.out.println("📋 " + symbol + " already has open Strategy 2 position - skipping new signals");
                return;
            }
            
            // Check for EMA crossover signals
            List<EMAStrategy.TradeSignal> crossoverSignals = emaStrategy.checkEMASignals();
            if (!crossoverSignals.isEmpty()) {
                for (EMAStrategy.TradeSignal signal : crossoverSignals) {
                    if (symbol.equals(signal.getSymbol())) {
                        System.out.println("🎯 STRATEGY 2 1H SIGNAL DETECTED: " + signal.getType() + " " + symbol);
                        openPositionFromSignal(signal, timestamp, emaValues, trend, isTrending ? "TRENDING" : "RANGING");
                    }
                }
            }
            
            // Check for pullback entry signals
            List<EMAStrategy.TradeSignal> pullbackSignals = emaStrategy.checkPullbackEntries();
            if (!pullbackSignals.isEmpty()) {
                for (EMAStrategy.TradeSignal signal : pullbackSignals) {
                    if (symbol.equals(signal.getSymbol())) {
                        System.out.println("🎯 STRATEGY 2 1H PULLBACK SIGNAL: " + signal.getType() + " " + symbol);
                        openPositionFromSignal(signal, timestamp, emaValues, trend, isTrending ? "TRENDING" : "RANGING");
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
    private void openPositionFromSignal(EMAStrategy.TradeSignal signal, String timestamp, 
                                      Map<String, Double> emaValues, String trend, String marketCondition) {
        try {
            // Open Strategy 2 position with 1-hour timeframe parameters
            com.tradingbot.model.CryptoStrategy2 position = positionService.openStrategy2Position(
                signal.getSymbol(),
                signal.getType(),
                signal.getEntryPrice(),
                signal.getStopLoss(),
                signal.getTakeProfit(),
                signal.getReason() + " (1H Timeframe)",
                emaValues.getOrDefault("EMA9", 0.0),
                emaValues.getOrDefault("EMA20", 0.0),
                trend,
                marketCondition
            );
            
            if (position != null) {
                System.out.println("✅ Strategy 2 1H position opened and notification sent for " + signal.getSymbol());
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error opening Strategy 2 1H position: " + e.getMessage());
        }
    }
    
    /**
     * Get historical candlestick data for Strategy 2 analysis (1-hour timeframe)
     */
    private List<Map<String, Object>> getHistoricalData(String symbol, int candleCount) {
        try {
            // Calculate timestamps for the required period (1-hour candles)
            long endTime = System.currentTimeMillis() / 1000;
            long startTime = endTime - (candleCount * 60 * 60); // 60 minutes per candle (1 hour)
            
            String url = "https://api.india.delta.exchange/v2/history/candles?symbol=" + symbol + 
                        "&resolution=1h&start=" + startTime + "&end=" + endTime;
            
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
            System.err.println("❌ Error fetching 1H historical data for " + symbol + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Get Strategy 2 1-hour status
     */
    public String getStrategy2OneHourStatus() {
        if (!isMonitoring) {
            return "❌ Strategy 2 1-hour monitoring is not active";
        }
        
        StringBuilder status = new StringBuilder();
        status.append("✅ Strategy 2 1-Hour Monitoring Active\n");
        status.append("📊 Symbols: ").append(String.join(", ", SYMBOLS_TO_MONITOR)).append("\n");
        status.append("⏰ Interval: ").append(MONITORING_INTERVAL_MINUTES).append(" minutes (1 hour)\n");
        status.append("🎯 Strategy: 9/20 EMA Crossover\n");
        status.append("⚡ Risk-Reward: 1:2, Leverage: 5x\n");
        status.append("📈 Focus: Trending markets only\n");
        status.append("🔄 Cycles: ").append(monitoringCycle);
        
        return status.toString();
    }

    /**
     * Get Strategy 2 1-hour performance summary
     */
    public String getStrategy2OneHourPerformance() {
        try {
            Object[] summary = strategy2Repository.getStrategyPerformanceSummary();
            
            if (summary == null || summary.length < 5) {
                return "📊 No Strategy 2 trades found yet";
            }

            long totalTrades = ((Number) summary[0]).longValue();
            long winningTrades = ((Number) summary[1]).longValue();
            long losingTrades = ((Number) summary[2]).longValue();
            double totalPnL = ((Number) summary[3]).doubleValue();
            double averagePnL = ((Number) summary[4]).doubleValue();

            double winRate = totalTrades > 0 ? (double) winningTrades / totalTrades * 100 : 0;

            return String.format("""
                📊 Strategy 2 (1H) Performance Summary:
                
                🔄 Total Trades: %d
                ✅ Winning Trades: %d (%.1f%%)
                ❌ Losing Trades: %d (%.1f%%)
                💰 Total P&L: $%.2f
                📈 Average P&L: $%.2f
                🎯 Win Rate: %.1f%%
                ⏰ Timeframe: 1 Hour
                """, totalTrades, winningTrades, winRate, losingTrades, 100-winRate, totalPnL, averagePnL);
                
        } catch (Exception e) {
            return "❌ Error getting Strategy 2 performance: " + e.getMessage();
        }
    }
}
