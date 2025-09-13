package com.tradingbot;

import com.tradingbot.service.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

/**
 * Strategy 2 Trading Bot - 9/20 EMA Crossover Strategy
 * 
 * STRATEGY 2: 9/20 EMA Crossover (Aggressive)
 * - Risk-Reward Ratio: 1:3
 * - Leverage: 25x
 * - Timeframes: 15m, 30m, 1h, 4h
 * - Trades per Day: 2-3
 * - Best for: Trending markets
 * 
 * Features EMA crossover detection, pullback entries, and SL/TP monitoring
 */
@SpringBootApplication
public class Strategy2TradingBot {

    @Autowired
    private Strategy2MonitoringService strategy2Service;
    
    @Autowired
    private SLTPMonitoringService sltpMonitoringService;
    
    @Autowired
    private PositionManagementService positionService;
    
    @Autowired
    private CryptoPriceService priceService;
    
    @Autowired
    private TelegramNotificationService telegramService;
    

    public static void main(String[] args) {
        SpringApplication.run(Strategy2TradingBot.class, args);
    }

    @Bean
    public CommandLineRunner runStrategy2Original() {
        return args -> {
            initialize();
        };
    }

    /**
     * Initialize and start the Strategy 2 trading bot
     */
    public void initialize() {
        System.out.println("🚀 STRATEGY 2 TRADING BOT INITIALIZATION");
        System.out.println("⚡ STRATEGY 2: 9/20 EMA Crossover (Aggressive)");
        System.out.println("=" + "=".repeat(50));
        
        try {
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
            
            // Send Strategy 2 initialization message
            String initMessage = String.format("""
                🚀 *Strategy 2 Trading Bot Started*
                
                ⚡ *Strategy 2* is now active
                ✅ *9/20 EMA Crossover*
                📈 *Risk-Reward:* 1:3
                ⚡ *Leverage:* 25x
                
                📈 *Current Market Analysis:*
                • Bitcoin: $%.2f %s %s
                • Ethereum: $%.2f %s %s
                • Solana: $%.2f %s %s
                
                🎯 *Strategy 2 Features:*
                • EMA crossover signals
                • Pullback entries
                • Trending market focus
                • High frequency trading
                • 25x leverage support
                
                📱 *You'll receive notifications for:*
                • Entry signals (BUY/SELL)
                • Exit notifications (SL/TP)
                • Strategy 2 specific alerts
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
            
            // Start Strategy 2 monitoring
            System.out.println("⚡ Starting Strategy 2 monitoring...");
            strategy2Service.startMonitoring();
            
            // Start SL/TP monitoring (every 50 seconds)
            System.out.println("🛡️ Starting SL/TP monitoring (every 50 seconds)...");
            sltpMonitoringService.startMonitoring();
            
            System.out.println("✅ Strategy 2 Trading Bot is now running!");
            System.out.println("📱 Check your Telegram for notifications!");
            System.out.println();
            System.out.println("Press Ctrl+C to stop all monitoring...");
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("🛑 Shutting down Strategy 2 trading bot...");
                strategy2Service.stopMonitoring();
                sltpMonitoringService.stopMonitoring();
                positionService.closeAllPositions("Application Shutdown");
                telegramService.sendTelegramMessage("🛑 *Strategy 2 Trading Bot Shut Down!* \n\nAll monitoring stopped and open positions closed.");
            }));

        } catch (Exception e) {
            System.err.println("❌ Error during Strategy 2 bot initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get Strategy 2 recommendation based on price levels and trend
     */
    private String getStrategy2Recommendation(String coinName, Double price, double strongLevel, double weakLevel) {
        if (price == null) return "WAIT (No data)";
        
        // Strategy 2 is more aggressive - look for trending conditions
        if (price >= strongLevel) return "BUY (Strong uptrend)";
        if (price <= weakLevel) return "SELL (Strong downtrend)";
        
        // In middle range, check for trending vs ranging
        return "MONITOR (Check EMA trend)";
    }
}
