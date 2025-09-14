package com.tradingbot;

import com.tradingbot.service.CryptoPriceService;
import com.tradingbot.service.TelegramNotificationService;
import com.tradingbot.service.DualStrategySchedulerService;
import com.tradingbot.service.SLTPMonitoringService;
import com.tradingbot.service.PositionManagementService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
@EnableScheduling
public class TradingbotApplication implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(TradingbotApplication.class);

	@Autowired
	private DualStrategySchedulerService dualStrategyScheduler;
	
	@Autowired
	private SLTPMonitoringService sltpMonitoringService;
	
	@Autowired
	private PositionManagementService positionService;
	
	@Autowired
	private TelegramNotificationService telegramService;
	
	@Autowired
	private com.tradingbot.service.AlertVerificationService alertVerificationService;
	
	
	@Autowired
	private com.tradingbot.service.Strategy2PositionService strategy2PositionService;
	

	public static void main(String[] args) {
		logger.info("🚀 Starting Trading Bot Application...");
		logger.info("📊 Application Version: 2.0 - Dual Strategy Scheduler");
		logger.info("⏰ Startup Time: {}", java.time.LocalDateTime.now());
		
		try {
			SpringApplication.run(TradingbotApplication.class, args);
			logger.info("✅ Trading Bot Application started successfully!");
		} catch (Exception e) {
			logger.error("❌ Failed to start Trading Bot Application", e);
			throw e;
		}
	}

	@Override
	public void run(String... args) throws Exception {
		logger.info("🔄 CommandLineRunner started - Initializing scheduler...");
		initializeScheduler();
		logger.info("✅ CommandLineRunner completed successfully");
	}
	
	/**
	 * Initialize Dual Strategy Scheduler
	 */
	public void initializeScheduler() {
		logger.info("🚀 INITIALIZING DUAL STRATEGY SCHEDULER");
		logger.info("=" + "=".repeat(50));
		
		try {
			logger.info("📋 Starting scheduler initialization process...");
			logger.debug("Available services: dualStrategyScheduler={}, sltpMonitoringService={}, positionService={}", 
				dualStrategyScheduler != null, sltpMonitoringService != null, positionService != null);
			
			// Send startup notification
			String startupMessage = """
				🚀 *Dual Strategy Scheduler Activated*
				
				⏰ *Scheduler Configuration:*
				• Strategy 1: Every 1 minute
				• Strategy 2: Every 2 minutes
				
				🎯 *Strategy 1:* 200-Day MA + Trendline Breakout
				📈 *Risk-Reward:* 6:1 (Conservative)
				📊 *Accuracy:* 92.2%%
				
				⚡ *Strategy 2:* 9/20 EMA Crossover
				📈 *Risk-Reward:* 1:3 (Aggressive)
				⚡ *Leverage:* 25x
				
				📊 *Monitoring:* BTCUSD, ETHUSD, SOLUSD
				
				📱 *You'll receive notifications for:*
				• Strategy 1 signals (every 1 minute)
				• Strategy 2 signals (every 2 minutes)
				• Exit notifications (SL/TP)
				
				✅ *Both strategies are now running automatically!*
				""";
			
			logger.info("📱 Sending startup notification to Telegram...");
			telegramService.sendTelegramMessage(startupMessage);
			logger.info("✅ Startup notification sent successfully");
			
			// Start SL/TP monitoring (every 50 seconds)
			logger.info("🛡️ Starting SL/TP monitoring service...");
			sltpMonitoringService.startMonitoring();
			logger.info("✅ SL/TP monitoring service started");
			
			logger.info("✅ Dual Strategy Scheduler is now active!");
			logger.info("📱 Check your Telegram for both Strategy 1 & 2 notifications!");
			logger.info("⏰ Strategy 1 runs every 1 minute, Strategy 2 every 2 minutes");
			
			// Send alert verification message
			logger.info("📢 Sending alert verification message...");
			alertVerificationService.sendAlertVerificationMessage();
			logger.info("✅ Alert verification message sent");
			
			// Test database connection first (temporarily disabled for debugging)
			logger.debug("Database connection test temporarily disabled");
			// databaseConnectionTestService.runCompleteDatabaseTest();
			
			// Create a simple test trade to verify database storage (temporarily disabled for debugging)
			logger.debug("Test trade creation temporarily disabled");
			// createSimpleTestTrade();
			
			logger.info("🎉 Scheduler initialization completed successfully!");
			
		} catch (Exception e) {
			logger.error("❌ Error initializing Dual Strategy Scheduler", e);
			logger.error("Error details: {}", e.getMessage(), e);
		}
	}

	
	/**
	 * Create a simple test trade to verify Strategy 2 database storage
	 */
	private void createSimpleTestTrade() {
		logger.info("🧪 Creating simple test trade for Strategy 2...");
		
		try {
			logger.debug("Waiting 2 seconds for database connection to be fully established...");
			Thread.sleep(2000);
			
			// Create a test trade using Strategy2PositionService
			com.tradingbot.model.CryptoStrategy2 testTrade = strategy2PositionService.openStrategy2Position(
				"BTCUSD",
				"BUY",
				65000.0,
				62000.0,
				71000.0,
				"Startup test trade - Database verification",
				64800.0,  // EMA9
				64200.0,  // EMA20
				"BULLISH", // Trend direction
				"TRENDING" // Market condition
			);
			
			if (testTrade != null) {
				logger.info("✅ Test trade created successfully!");
				logger.info("Trade ID: {}", testTrade.getId());
				logger.info("Symbol: {}", testTrade.getSymbol());
				logger.info("Entry Price: ${}", testTrade.getEntryPrice());
				logger.info("EMA9: ${}", testTrade.getEma9Value());
				logger.info("EMA20: ${}", testTrade.getEma20Value());
				logger.info("Status: {}", testTrade.getStatus());
				logger.debug("Test trade details: {}", testTrade);
				
				// Send verification message
				String verificationMessage = String.format("""
					🧪 *STRATEGY 2 DATABASE TEST*
					
					✅ *Test Trade Created Successfully!*
					
					📊 *Trade Details:*
					• ID: %d
					• Symbol: %s
					• Type: %s
					• Entry Price: $%.2f
					• EMA9: $%.2f
					• EMA20: $%.2f
					• Status: %s
					
					💾 *Data successfully stored in crypto_strategy2 table!*
					
					⏰ *Test Time:* %s
					""",
					testTrade.getId(),
					testTrade.getSymbol(),
					testTrade.getTradeType(),
					testTrade.getEntryPrice(),
					testTrade.getEma9Value(),
					testTrade.getEma20Value(),
					testTrade.getStatus(),
					java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
				);
				
				logger.info("📱 Sending verification message to Telegram...");
				telegramService.sendTelegramMessage(verificationMessage);
				logger.info("✅ Verification message sent successfully");
				
			} else {
				logger.error("❌ Test trade creation failed - Strategy2PositionService returned null");
				logger.warn("This might indicate an existing open position for BTCUSD");
				telegramService.sendTelegramMessage("❌ Strategy 2 test trade creation failed - Strategy2PositionService returned null. Check if there's already an open position for BTCUSD.");
			}
			
		} catch (Exception e) {
			logger.error("❌ Error creating test trade", e);
			logger.error("Error details: {}", e.getMessage(), e);
			
			String errorMessage = String.format("""
				❌ *STRATEGY 2 TEST TRADE ERROR*
				
				🔴 *Error Details:*
				• Error: %s
				• Time: %s
				
				🔍 *Possible Causes:*
				• Database connection issue
				• Missing database table
				• Repository configuration problem
				
				🚨 *Please check database connection!*
				""",
				e.getMessage(),
				java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
			);
			
			logger.info("📱 Sending error message to Telegram...");
			telegramService.sendTelegramMessage(errorMessage);
			logger.info("✅ Error message sent successfully");
		}
	}

}
