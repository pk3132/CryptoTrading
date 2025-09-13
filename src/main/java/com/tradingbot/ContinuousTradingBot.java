package com.tradingbot;

import com.tradingbot.service.SimpleMonitoringService;
import com.tradingbot.service.CryptoPriceService;
import com.tradingbot.service.TelegramNotificationService;

/**
 * Continuous Trading Bot
 * Continuously monitors crypto prices and sends trading signals
 */
public class ContinuousTradingBot {

    private final SimpleMonitoringService monitoringService;
    private final CryptoPriceService priceService;
    private final TelegramNotificationService telegramService;

    public ContinuousTradingBot() {
        this.monitoringService = new SimpleMonitoringService();
        this.priceService = new CryptoPriceService();
        this.telegramService = new TelegramNotificationService();
    }

    /**
     * Initialize and start the continuous trading bot
     */
    public void initialize() {
        System.out.println("🚀 INITIALIZING CONTINUOUS TRADING BOT");
        System.out.println("=" + "=".repeat(50));
        
        try {
            // Display current prices
            System.out.println("📊 Fetching current crypto prices...");
            priceService.displayCurrentPrices();
            
            // Send prices to Telegram
            String priceReport = priceService.createPriceReport();
            telegramService.sendTelegramMessage(priceReport);
            
            // Send initialization message
            // No redundant startup message - EnhancedTradingBot already sends comprehensive startup message
            
            // Start continuous monitoring
            System.out.println("🔍 Starting continuous price monitoring...");
            monitoringService.startMonitoring();
            
            // Keep the application running
            System.out.println("✅ Continuous Trading Bot is now running!");
            System.out.println("📱 Check your Telegram for real-time alerts!");
            System.out.println();
            System.out.println("Press Ctrl+C to stop monitoring...");
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n🛑 Shutting down Continuous Trading Bot...");
                monitoringService.stopMonitoring();
                System.out.println("✅ Shutdown complete!");
            }));
            
            // Keep the main thread alive
            while (true) {
                Thread.sleep(60000); // Sleep for 1 minute
                
                // Display monitoring stats every 10 minutes
                if (monitoringService.isMonitoring()) {
                    System.out.println("📊 " + monitoringService.getMonitoringStats() + " - Bot is active");
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error initializing Continuous Trading Bot: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ContinuousTradingBot bot = new ContinuousTradingBot();
        bot.initialize();
    }
}
