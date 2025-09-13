package com.tradingbot.service;

import org.springframework.stereotype.Service;
import com.tradingbot.service.DeltaCandlestickService;
import com.tradingbot.service.TelegramNotificationService;
import com.tradingbot.service.CryptoPriceService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Simple Monitoring Service
 * Continuously monitors crypto prices and detects basic trading setups
 */
@Service
public class SimpleMonitoringService {

    private static final int MONITORING_INTERVAL_MINUTES = 5; // Check every 5 minutes
    private static final String[] SYMBOLS_TO_MONITOR = {"BTCUSD", "ETHUSD", "SOLUSD"};
    
    private final DeltaCandlestickService candlestickService;
    private final TelegramNotificationService telegramService;
    private final CryptoPriceService priceService;
    private final ScheduledExecutorService scheduler;
    
    private boolean isMonitoring = false;
    private int monitoringCycle = 0;
    private Map<String, Double> previousPrices = new java.util.HashMap<>();

    public SimpleMonitoringService() {
        this.candlestickService = new DeltaCandlestickService();
        this.telegramService = new TelegramNotificationService();
        this.priceService = new CryptoPriceService();
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    /**
     * Start continuous monitoring
     */
    public void startMonitoring() {
        if (isMonitoring) {
            System.out.println("⚠️ Monitoring is already running!");
            return;
        }
        
        System.out.println("🚀 Starting Simple Price Monitoring...");
        System.out.println("📊 Monitoring: " + String.join(", ", SYMBOLS_TO_MONITOR) + " every " + MONITORING_INTERVAL_MINUTES + " minutes");
        
        // No redundant startup message - main bot already sends comprehensive startup message
        
        isMonitoring = true;
        monitoringCycle = 0;
        
        // Start the monitoring loop
        scheduler.scheduleAtFixedRate(this::monitorPrices, 0, MONITORING_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * Stop continuous monitoring
     */
    public void stopMonitoring() {
        if (!isMonitoring) {
            System.out.println("⚠️ Monitoring is not running!");
            return;
        }
        
        System.out.println("🛑 Stopping Price Monitoring...");
        scheduler.shutdown();
        isMonitoring = false;
        
        String stopMessage = """
            🛑 *Price Monitoring Stopped*
            
            📊 *Session Summary:*
            • Total cycles: %d
            • Monitoring duration: %d minutes
            • Status: Stopped by user
            
            🎯 *Strategy 1* remains ready for manual signals
            """.formatted(monitoringCycle, monitoringCycle * MONITORING_INTERVAL_MINUTES);
        
        telegramService.sendTelegramMessage(stopMessage);
    }

    /**
     * Main monitoring loop
     */
    private void monitorPrices() {
        if (!isMonitoring) return;
        
        monitoringCycle++;
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        System.out.println("🔍 Monitoring Cycle #" + monitoringCycle + " - " + timestamp);
        System.out.println("-".repeat(60));
        
        try {
            // Monitor each symbol
            for (String symbol : SYMBOLS_TO_MONITOR) {
                monitorSymbol(symbol, timestamp);
            }
            
            // No redundant status updates - main bot already provides comprehensive monitoring
            
        } catch (Exception e) {
            System.err.println("❌ Error in monitoring cycle: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("✅ Monitoring cycle completed");
        System.out.println();
    }

    /**
     * Monitor a specific symbol for trading setups
     */
    private void monitorSymbol(String symbol, String timestamp) {
        try {
            System.out.println("📊 Checking " + symbol + " for setups...");
            
            // Get current price
            Double currentPrice = getCurrentPrice(symbol);
            if (currentPrice == null) {
                System.out.println("❌ Could not fetch price for " + symbol);
                return;
            }
            
            System.out.println("💰 " + symbol + " current price: $" + String.format("%.2f", currentPrice));
            
            // Check for price movements
            checkPriceMovement(symbol, currentPrice, timestamp);
            
            // Get recent candlestick data for pattern analysis
            checkRecentPatterns(symbol, currentPrice, timestamp);
            
        } catch (Exception e) {
            System.err.println("❌ Error monitoring " + symbol + ": " + e.getMessage());
        }
    }

    /**
     * Get current price for a symbol
     */
    private Double getCurrentPrice(String symbol) {
        try {
            if (symbol.equals("BTCUSD")) {
                return priceService.getBitcoinPrice();
            } else if (symbol.equals("ETHUSD")) {
                return priceService.getEthereumPrice();
            } else if (symbol.equals("SOLUSD")) {
                return priceService.getSolanaPrice();
            }
        } catch (Exception e) {
            System.err.println("❌ Error fetching price for " + symbol + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Check for significant price movements
     */
    private void checkPriceMovement(String symbol, double currentPrice, String timestamp) {
        try {
            Double previousPrice = previousPrices.get(symbol);
            
            if (previousPrice != null) {
                double priceChange = ((currentPrice - previousPrice) / previousPrice) * 100;
                
                System.out.println("📈 " + symbol + " price change: " + String.format("%.2f", priceChange) + "%");
                
                // Log significant movements but don't send alerts (too many notifications)
                if (Math.abs(priceChange) > 2.0) {
                    String direction = priceChange > 0 ? "🟢 UP" : "🔴 DOWN";
                    System.out.println("📊 SIGNIFICANT MOVEMENT: " + direction + " " + String.format("%.2f", Math.abs(priceChange)) + "% - Monitoring for signal confirmation");
                    // No alert sent - only actual signals will trigger notifications
                }
            }
            
            // Update previous price
            previousPrices.put(symbol, currentPrice);
            
        } catch (Exception e) {
            System.err.println("❌ Error checking price movement for " + symbol + ": " + e.getMessage());
        }
    }

    /**
     * Check recent candlestick patterns
     */
    private void checkRecentPatterns(String symbol, double currentPrice, String timestamp) {
        try {
            // Get recent daily candles (last 10 days)
            Map<String, Object> response = candlestickService.getCandlestickData(symbol, 10, "1d");
            
            if (response != null && response.containsKey("result")) {
                List<Map<String, Object>> candles = (List<Map<String, Object>>) response.get("result");
                
                if (candles != null && candles.size() >= 3) {
                    // Analyze recent patterns
                    analyzeRecentCandles(symbol, candles, currentPrice, timestamp);
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error checking patterns for " + symbol + ": " + e.getMessage());
        }
    }

    /**
     * Analyze recent candles for patterns
     */
    private void analyzeRecentCandles(String symbol, List<Map<String, Object>> candles, double currentPrice, String timestamp) {
        try {
            if (candles.size() < 3) return;
            
            // Get the last 3 candles
            Map<String, Object> candle1 = candles.get(candles.size() - 3);
            Map<String, Object> candle2 = candles.get(candles.size() - 2);
            Map<String, Object> candle3 = candles.get(candles.size() - 1);
            
            double close1 = Double.parseDouble(candle1.get("close").toString());
            double close2 = Double.parseDouble(candle2.get("close").toString());
            double close3 = Double.parseDouble(candle3.get("close").toString());
            
            // Check for breakout pattern (3 consecutive higher closes) - Log only, no alert
            if (close1 < close2 && close2 < close3) {
                System.out.println("📊 BREAKOUT PATTERN detected in " + symbol + " - Monitoring for signal confirmation");
                // No alert sent - only actual buy signals will trigger notifications
            }
            
            // Check for breakdown pattern (3 consecutive lower closes) - Log only, no alert
            else if (close1 > close2 && close2 > close3) {
                System.out.println("📊 BREAKDOWN PATTERN detected in " + symbol + " - Monitoring for signal confirmation");
                // No alert sent - only actual sell signals will trigger notifications
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error analyzing candles for " + symbol + ": " + e.getMessage());
        }
    }

    /**
     * Send periodic status update
     */
    private void sendStatusUpdate() {
        try {
            String statusMessage = """
                📊 *Monitoring Status Update*
                
                ⏰ *Time:* %s
                🔄 *Cycles Completed:* %d
                ⏱️ *Monitoring Duration:* %d minutes
                
                📈 *Current Prices:*
                • Bitcoin: $%.2f
                • Ethereum: $%.2f
                • Solana: $%.2f
                
                🎯 *Simple Monitoring* is actively watching for:
                • Price movements (>2%%)
                • Breakout patterns
                • Breakdown patterns
                
                ✅ *System Status:* Active
                """.formatted(
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    monitoringCycle,
                    monitoringCycle * MONITORING_INTERVAL_MINUTES,
                    priceService.getBitcoinPrice(),
                    priceService.getEthereumPrice(),
                    priceService.getSolanaPrice()
                );
            
            telegramService.sendTelegramMessage(statusMessage);
            
        } catch (Exception e) {
            System.err.println("❌ Error sending status update: " + e.getMessage());
        }
    }

    /**
     * Check if monitoring is active
     */
    public boolean isMonitoring() {
        return isMonitoring;
    }

    /**
     * Get monitoring statistics
     */
    public String getMonitoringStats() {
        return String.format("Monitoring Cycle: %d, Duration: %d minutes", 
                           monitoringCycle, monitoringCycle * MONITORING_INTERVAL_MINUTES);
    }
}
