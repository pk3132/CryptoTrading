package com.tradingbot;

import com.tradingbot.service.TelegramNotificationService;
import com.tradingbot.service.DualStrategySchedulerService;
import com.tradingbot.service.SLTPMonitoringService;
import com.tradingbot.service.PositionManagementService;
import com.tradingbot.service.PositionChecker;
import com.tradingbot.service.BalanceCheck;
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
	private PositionChecker positionChecker;
	
	@Autowired
	private BalanceCheck balanceCheck;
	
	
	

	public static void main(String[] args) {
		logger.info("🚀 Starting Trading Bot Application...");
		logger.info("📊 Application Version: 7.0 - EMA 200 + Trendline Strategy (1m)");
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
		logger.info("🔄 CommandLineRunner started - Checking existing positions...");
		
		// First, check for existing positions to prevent duplicates
		checkExistingPositions();
		
		// Then initialize the scheduler
		initializeScheduler();
		logger.info("✅ CommandLineRunner completed successfully");
	}
	
	/**
	 * Check for existing positions on startup to prevent duplicates
	 */
	public void checkExistingPositions() {
		logger.info("🔍 CHECKING EXISTING POSITIONS ON STARTUP");
		logger.info("==========================================");
		
		try {
			// Check balance first
			logger.info("💰 Checking current balance...");
			balanceCheck.printBalance();
			
			// Check for existing positions
			logger.info("📊 Checking for existing positions...");
			
			boolean hasBtcPosition = positionChecker.hasOpenPosition("BTCUSD");
			boolean hasEthPosition = positionChecker.hasOpenPosition("ETHUSD");
			
			logger.info("📋 POSITION SUMMARY:");
			logger.info("===================");
			logger.info("BTCUSD Position: {}", hasBtcPosition ? "⚠️ OPEN" : "✅ NONE");
			logger.info("ETHUSD Position: {}", hasEthPosition ? "⚠️ OPEN" : "✅ NONE");
			
			if (hasBtcPosition || hasEthPosition) {
				logger.warn("⚠️ EXISTING POSITIONS DETECTED - Duplicate Prevention ACTIVE");
				logger.warn("🛡️ New BUY orders for existing positions will be BLOCKED");
				logger.warn("✅ SELL orders to close positions will be ALLOWED");
				
				// Send notification about existing positions
				String positionMessage = String.format("""
					🛡️ *Position Check Complete*
					
					📊 *Current Positions:*
					• BTCUSD: %s
					• ETHUSD: %s
					
					🛡️ *Duplicate Prevention: ACTIVE*
					• Existing positions protected from duplicate BUY orders
					• SELL orders allowed to close positions
					
					✅ *Bot ready for trading with position protection!*
					""", 
					hasBtcPosition ? "⚠️ OPEN" : "✅ NONE",
					hasEthPosition ? "⚠️ OPEN" : "✅ NONE"
				);
				
				telegramService.sendTelegramMessage(positionMessage);
			} else {
				logger.info("✅ No existing positions found - Ready for new trades");
			}
			
			logger.info("✅ Position check completed successfully");
			
		} catch (Exception e) {
			logger.error("❌ Error checking existing positions: {}", e.getMessage(), e);
			logger.warn("⚠️ Continuing with startup - position checking will be done per-trade");
		}
	}
	
	/**
	 * Initialize EMA 200 + Trendline Strategy Scheduler
	 */
	public void initializeScheduler() {
		logger.info("🚀 INITIALIZING EMA 200 + TRENDLINE STRATEGY SCHEDULER");
		logger.info("=" + "=".repeat(50));
		
		try {
			logger.info("📋 Starting scheduler initialization process...");
			logger.debug("Available services: dualStrategyScheduler={}, sltpMonitoringService={}, positionService={}", 
				dualStrategyScheduler != null, sltpMonitoringService != null, positionService != null);
			
			// Send startup notification
			String startupMessage = """
				🚀 *EMA 200 + Trendline Strategy Activated*
				
				⏰ *Scheduler Configuration:*
				• EMA 200 + Trendline Strategy: Every 5 minutes
				• Timeframe: 1-minute candles
				
				🔍 *Strategy Features:*
				• EMA 200 Trend Filter (200-period)
				• Swing Point Detection
				• Trendline Breakouts (Support/Resistance)
				• Fresh Trendline Validation
				• Risk-Reward: 1:2 (0.5% SL, 1.0% TP)
				
				📊 *Monitoring:* BTCUSD, ETHUSD
				
				📱 *You'll receive notifications for:*
				• BUY signals (Price > EMA200 + Resistance breakout)
				• SELL signals (Price < EMA200 + Support breakout)
				• Exit notifications (SL/TP)
				
				✅ *EMA 200 + Trendline Strategy is now running automatically!*
				""";
			
			logger.info("📱 Sending startup notification to Telegram...");
			telegramService.sendTelegramMessage(startupMessage);
			logger.info("✅ Startup notification sent successfully");
			
			// Start SL/TP monitoring (every 10 seconds)
			logger.info("🛡️ Starting SL/TP monitoring service...");
			sltpMonitoringService.startMonitoring();
			logger.info("✅ SL/TP monitoring service started");
			
			logger.info("✅ EMA 200 + Trendline Strategy Scheduler is now active!");
			logger.info("📱 Check your Telegram for EMA 200 + Trendline notifications!");
			logger.info("⏰ Strategy runs every 5 minutes");
			
			// Send alert verification message
			logger.info("📢 Sending alert verification message...");
			alertVerificationService.sendAlertVerificationMessage();
			logger.info("✅ Alert verification message sent");
			
			logger.info("🎉 EMA 200 + Trendline Strategy initialization completed successfully!");
			
		} catch (Exception e) {
			logger.error("❌ Error initializing EMA 200 + Trendline Strategy Scheduler", e);
			logger.error("Error details: {}", e.getMessage(), e);
		}
	}

	

}
