package com.tradingbot;

import com.tradingbot.service.*;
import com.tradingbot.repository.CryptoStrategy2Repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Strategy 2 One-Hour Trading Bot - 9/20 EMA Crossover Strategy
 * 
 * STRATEGY 2: 9/20 EMA Crossover (1-Hour Timeframe - OPTIMAL)
 * - Risk-Reward Ratio: 1:2
 * - Leverage: 5x
 * - Timeframe: 1 Hour (Best performance from backtest)
 * - Trades per Day: 1-2
 * - Best for: Trending markets
 * 
 * Features EMA crossover detection, pullback entries, and SL/TP monitoring
 * with comprehensive database tracking and Telegram notifications
 */
@SpringBootApplication
public class Strategy2OneHourTradingBot {

    @Autowired
    private Strategy2OneHourMonitoringService strategy2MonitoringService;
    
    @Autowired
    private Strategy2SLTPMonitoringService sltpMonitoringService;
    
    @Autowired
    private Strategy2PositionService positionService;
    
    @Autowired
    private CryptoPriceService priceService;
    
    @Autowired
    private TelegramNotificationService telegramService;
    
    @Autowired
    private StartupRecoveryService recoveryService;
    

    public static void main(String[] args) {
        SpringApplication.run(Strategy2OneHourTradingBot.class, args);
    }

    @Bean
    public CommandLineRunner runStrategy2() {
        return args -> {
            initialize();
        };
    }

    /**
     * Initialize and start the Strategy 2 One-Hour trading bot
     */
    public void initialize() {
        System.out.println("🚀 STRATEGY 2 ONE-HOUR TRADING BOT INITIALIZATION");
        System.out.println("⚡ STRATEGY 2: 9/20 EMA Crossover (1-Hour Timeframe - OPTIMAL)");
        System.out.println("=" + "=".repeat(60));
        
        try {
            // Recover existing open trades first
            System.out.println("🔄 Checking for existing open trades...");
            recoveryService.recoverExistingTrades();
            
            // Display current prices
            System.out.println("📊 Fetching current crypto prices...");
            priceService.displayCurrentPrices();
            
            // Get current prices for startup message
            Double btcPrice = priceService.getBitcoinPrice();
            Double ethPrice = priceService.getEthereumPrice();
            Double solPrice = priceService.getSolanaPrice();
            
            // Get individual coin recommendations based on Strategy 2
            String btcRecommendation = getStrategy2Recommendation("Bitcoin", btcPrice, 100000, 50000);
            String ethRecommendation = getStrategy2Recommendation("Ethereum", ethPrice, 5000, 3000);
            String solRecommendation = getStrategy2Recommendation("Solana", solPrice, 200, 100);
            
            // Send Strategy 2 1-hour initialization message
            String initMessage = String.format("""
                🚀 *Strategy 2 One-Hour Trading Bot Started*
                
                ⚡ *Strategy 2* is now active
                ✅ *9/20 EMA Crossover*
                📈 *Risk-Reward:* 1:2
                ⚡ *Leverage:* 5x
                🕐 *Timeframe:* 1 Hour (OPTIMAL)
                
                📈 *Current Market Analysis:*
                • Bitcoin: $%.2f %s %s
                • Ethereum: $%.2f %s %s
                • Solana: $%.2f %s %s
                
                🎯 *Strategy 2 Features:*
                • EMA crossover signals
                • Pullback entries
                • Trending market focus
                • 1-hour timeframe (best performance)
                • 5x leverage support
                • Comprehensive database tracking
                
                📱 *You'll receive notifications for:*
                • Entry signals (BUY/SELL)
                • Exit notifications (SL/TP)
                • Strategy 2 specific alerts
                • Database updates
                
                🗄️ *Database:* CryptoStrategy2 table
                """,
                btcPrice != null ? btcPrice : 0.0,
                btcPrice != null && btcPrice > 100000 ? "🟢" : btcPrice != null && btcPrice > 50000 ? "🟡" : "🔴",
                btcRecommendation,
                ethPrice != null ? ethPrice : 0.0,
                ethPrice != null && ethPrice > 5000 ? "🟢" : ethPrice != null && ethPrice > 3000 ? "🟡" : "🔴",
                ethRecommendation,
                solPrice != null ? solPrice : 0.0,
                solPrice != null && solPrice > 200 ? "🟢" : solPrice != null && solPrice > 100 ? "🟡" : "🔴",
                solRecommendation
            );
            
            telegramService.sendTelegramMessage(initMessage);
            
            // Start Strategy 2 1-hour monitoring
            System.out.println("⚡ Starting Strategy 2 1-hour monitoring...");
            strategy2MonitoringService.startMonitoring();
            
            // Start SL/TP monitoring (every 30 seconds)
            System.out.println("🛡️ Starting Strategy 2 SL/TP monitoring (every 30 seconds)...");
            sltpMonitoringService.startMonitoring();
            
            // Start performance reporting (every 6 hours)
            System.out.println("📊 Starting Strategy 2 performance reporting (every 6 hours)...");
            startPerformanceReporting();
            
            System.out.println("✅ Strategy 2 One-Hour Trading Bot is now running!");
            System.out.println("📱 Check your Telegram for notifications!");
            System.out.println("🗄️ All trades are being tracked in CryptoStrategy2 database table!");
            System.out.println();
            System.out.println("Press Ctrl+C to stop all monitoring...");
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("🛑 Shutting down Strategy 2 One-Hour trading bot...");
                strategy2MonitoringService.stopMonitoring();
                sltpMonitoringService.stopMonitoring();
                positionService.closeAllOpenPositions("Application Shutdown");
                telegramService.sendTelegramMessage("🛑 *Strategy 2 One-Hour Trading Bot Shut Down!* \n\nAll monitoring stopped and open positions closed.");
            }));

        } catch (Exception e) {
            System.err.println("❌ Error during Strategy 2 One-Hour bot initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Start performance reporting
     */
    private void startPerformanceReporting() {
        ScheduledExecutorService performanceScheduler = Executors.newScheduledThreadPool(1);
        
        // Send performance report every 6 hours
        performanceScheduler.scheduleAtFixedRate(() -> {
            try {
                String performanceReport = positionService.getStrategy2PerformanceSummary();
                String statusReport = strategy2MonitoringService.getStrategy2OneHourStatus();
                String sltpStatus = sltpMonitoringService.getStrategy2SLTPStatus();
                
                String report = String.format("""
                    📊 *Strategy 2 Performance Report (6-Hour Update)*
                    
                    %s
                    
                    %s
                    
                    %s
                    
                    🕐 *Timeframe:* 1 Hour (Optimal)
                    🗄️ *Database:* CryptoStrategy2 table
                    """, performanceReport, statusReport, sltpStatus);
                
                telegramService.sendTelegramMessage(report);
                
            } catch (Exception e) {
                System.err.println("❌ Error sending performance report: " + e.getMessage());
            }
        }, 6, 6, TimeUnit.HOURS);
    }

    /**
     * Get Strategy 2 recommendation based on price levels and trend
     */
    private String getStrategy2Recommendation(String coinName, Double price, double strongLevel, double weakLevel) {
        if (price == null) return "WAIT (No data)";
        
        // Strategy 2 is optimized for trending conditions
        if (price >= strongLevel) return "BUY (Strong uptrend)";
        if (price <= weakLevel) return "SELL (Strong downtrend)";
        
        // In middle range, Strategy 2 waits for clear trending signals
        return "MONITOR (Wait for EMA trend)";
    }
}
