package com.tradingbot;

import com.tradingbot.service.CryptoPriceService;
import com.tradingbot.service.TelegramNotificationService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TradingbotApplication {

	public static void main(String[] args) {
		System.out.println("🚀 Starting Trading Bot Application...");
		System.out.println("=" + "=".repeat(40));
		
		// Display current crypto prices on startup
		try {
			CryptoPriceService priceService = new CryptoPriceService();
			TelegramNotificationService telegramService = new TelegramNotificationService();
			
			// Display prices in console
			priceService.displayCurrentPrices();
			
			// Get current market analysis
			Double btcPrice = priceService.getBitcoinPrice();
			Double ethPrice = priceService.getEthereumPrice();
			Double solPrice = priceService.getSolanaPrice();
			
			// Determine current market sentiment
			String marketSentiment = analyzeMarketSentiment(btcPrice, ethPrice, solPrice);
			String recommendedSide = getRecommendedSide(btcPrice, ethPrice, solPrice);
			
			// Get individual coin recommendations
			String btcRecommendation = getCoinRecommendation("Bitcoin", btcPrice, 100000, 50000);
			String ethRecommendation = getCoinRecommendation("Ethereum", ethPrice, 5000, 3000);
			String solRecommendation = getCoinRecommendation("Solana", solPrice, 200, 100);
			
			// No startup message - EnhancedTradingBot will send the startup message
			
		} catch (Exception e) {
			System.err.println("❌ Error fetching prices on startup: " + e.getMessage());
		}
		
		System.out.println("🤖 Initializing Spring Boot Application...");
		SpringApplication.run(TradingbotApplication.class, args);
	}

	/**
	 * Analyze current market sentiment based on crypto prices
	 */
	private static String analyzeMarketSentiment(Double btcPrice, Double ethPrice, Double solPrice) {
		int bullishCount = 0;
		int totalCount = 0;
		
		if (btcPrice != null) {
			totalCount++;
			if (btcPrice > 100000) bullishCount++;
		}
		
		if (ethPrice != null) {
			totalCount++;
			if (ethPrice > 5000) bullishCount++;
		}
		
		if (solPrice != null) {
			totalCount++;
			if (solPrice > 200) bullishCount++;
		}
		
		if (totalCount == 0) return "📊 Neutral (No data)";
		
		double bullishPercentage = (double) bullishCount / totalCount * 100;
		
		if (bullishPercentage >= 67) {
			return "🚀 Bullish (Strong upward momentum)";
		} else if (bullishPercentage >= 33) {
			return "⚖️ Mixed (Sideways movement)";
		} else {
			return "📉 Bearish (Downward pressure)";
		}
	}

	/**
	 * Get recommended trading side based on current market conditions
	 */
	private static String getRecommendedSide(Double btcPrice, Double ethPrice, Double solPrice) {
		int strongCount = 0;
		int weakCount = 0;
		
		if (btcPrice != null) {
			if (btcPrice > 100000) strongCount++;
			else if (btcPrice < 50000) weakCount++;
		}
		
		if (ethPrice != null) {
			if (ethPrice > 5000) strongCount++;
			else if (ethPrice < 3000) weakCount++;
		}
		
		if (solPrice != null) {
			if (solPrice > 200) strongCount++;
			else if (solPrice < 100) weakCount++;
		}
		
		if (strongCount > weakCount) {
			return "🟢 BUY (Market showing strength)";
		} else if (weakCount > strongCount) {
			return "🔴 SELL (Market showing weakness)";
		} else {
			return "⚖️ WAIT (Mixed signals - wait for clear direction)";
		}
	}

	/**
	 * Get individual coin recommendation based on current price levels
	 */
	private static String getCoinRecommendation(String coinName, Double price, double strongLevel, double weakLevel) {
		if (price == null) {
			return "❌ No Data";
		}
		
		if (price > strongLevel) {
			return "🟢 BUY";
		} else if (price < weakLevel) {
			return "🔴 SELL";
		} else {
			return "🟡 WAIT";
		}
	}

}
