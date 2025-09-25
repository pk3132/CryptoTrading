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
	
	
	

	public static void main(String[] args) {
		logger.info("🚀 Starting Trading Bot Application...");
		logger.info("📊 Application Version: 6.0 - Swing Point Based Strategy");
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
	 * Initialize Aggressive Chart Technical Analysis Strategy Scheduler
	 */
	public void initializeScheduler() {
		logger.info("🚀 INITIALIZING AGGRESSIVE CHART TECHNICAL ANALYSIS STRATEGY SCHEDULER");
		logger.info("=" + "=".repeat(50));
		
		try {
			logger.info("📋 Starting scheduler initialization process...");
			logger.debug("Available services: dualStrategyScheduler={}, sltpMonitoringService={}, positionService={}", 
				dualStrategyScheduler != null, sltpMonitoringService != null, positionService != null);
			
			// Send startup notification
			String startupMessage = """
				🚀 *Aggressive Chart Technical Analysis Strategy Activated*
				
				⏰ *Scheduler Configuration:*
				• Aggressive Chart Strategy: Every 15 minutes
				• Timeframe: 15-minute candles
				
				🔍 *Chart Elements Handled:*
				• Price Movement (Uptrend/Downtrend)
				• Bollinger Bands (Volatility)
				• Support/Resistance Levels
				• Candlestick Patterns (Green/Red)
				• Trend Analysis (SMA5)
				• Risk-Reward: 1:2
				
				📊 *Monitoring:* BTCUSD, ETHUSD
				
				📱 *You'll receive notifications for:*
				• BUY signals (Price uptrend, Support bounces, Bullish patterns)
				• SELL signals (Price downtrend, Resistance rejections, Bearish patterns)
				• Exit notifications (SL/TP)
				
				✅ *Aggressive Chart Technical Analysis Strategy is now running automatically!*
				""";
			
			logger.info("📱 Sending startup notification to Telegram...");
			telegramService.sendTelegramMessage(startupMessage);
			logger.info("✅ Startup notification sent successfully");
			
			// Start SL/TP monitoring (every 50 seconds)
			logger.info("🛡️ Starting SL/TP monitoring service...");
			sltpMonitoringService.startMonitoring();
			logger.info("✅ SL/TP monitoring service started");
			
			logger.info("✅ Aggressive Chart Technical Analysis Strategy Scheduler is now active!");
			logger.info("📱 Check your Telegram for aggressive chart pattern notifications!");
			logger.info("⏰ Strategy runs every 15 minutes");
			
			// Send alert verification message
			logger.info("📢 Sending alert verification message...");
			alertVerificationService.sendAlertVerificationMessage();
			logger.info("✅ Alert verification message sent");
			
			logger.info("🎉 Aggressive Chart Technical Analysis Strategy initialization completed successfully!");
			
		} catch (Exception e) {
			logger.error("❌ Error initializing Aggressive Chart Technical Analysis Strategy Scheduler", e);
			logger.error("Error details: {}", e.getMessage(), e);
		}
	}

	

}
