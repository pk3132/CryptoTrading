package com.tradingbot.test;

import com.tradingbot.service.CryptoPriceService;
import com.tradingbot.service.DeltaApiClient;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Price Comparison Test - Shows difference between price sources
 */
public class PriceComparisonTest {
    
    public static void main(String[] args) {
        System.out.println("🔍 PRICE COMPARISON TEST");
        System.out.println("=" + "=".repeat(60));
        System.out.println("⏰ Check Time: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("💡 Comparing different price sources used by the system");
        System.out.println();
        
        // Create services
        CryptoPriceService priceService = new CryptoPriceService();
        DeltaApiClient apiClient = new DeltaApiClient();
        
        // Test BTC
        System.out.println("🟠 BITCOIN (BTCUSD) PRICE COMPARISON");
        System.out.println("=" + "=".repeat(50));
        comparePrices("BTCUSD", priceService, apiClient);
        
        System.out.println("\n" + "=".repeat(60) + "\n");
        
        // Test ETH
        System.out.println("🔵 ETHEREUM (ETHUSD) PRICE COMPARISON");
        System.out.println("=" + "=".repeat(50));
        comparePrices("ETHUSD", priceService, apiClient);
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("✅ Price comparison completed!");
    }
    
    private static void comparePrices(String symbol, CryptoPriceService priceService, DeltaApiClient apiClient) {
        try {
            // Method 1: CryptoPriceService (used for order placement)
            Double cryptoServicePrice = null;
            if ("BTCUSD".equals(symbol)) {
                cryptoServicePrice = priceService.getBitcoinPrice();
            } else if ("ETHUSD".equals(symbol)) {
                cryptoServicePrice = priceService.getEthereumPrice();
            }
            
            // Method 2: DeltaApiClient Mark Price (most accurate)
            Double markPrice = apiClient.getCurrentMarkPrice(symbol);
            
            System.out.println("📊 PRICE SOURCES COMPARISON:");
            System.out.println("-" + "-".repeat(40));
            System.out.println("🔧 CryptoPriceService (Order Placement): $" + 
                (cryptoServicePrice != null ? String.format("%.2f", cryptoServicePrice) : "NULL"));
            System.out.println("📈 DeltaApiClient Mark Price (Real-time): $" + 
                (markPrice != null ? String.format("%.2f", markPrice) : "NULL"));
            
            if (cryptoServicePrice != null && markPrice != null) {
                double difference = cryptoServicePrice - markPrice;
                double differencePercent = (difference / markPrice) * 100;
                
                System.out.println("\n📏 DIFFERENCE ANALYSIS:");
                System.out.println("-" + "-".repeat(40));
                System.out.println("💰 Price Difference: $" + String.format("%.2f", difference));
                System.out.println("📊 Difference %: " + String.format("%.2f", differencePercent) + "%");
                
                if (Math.abs(differencePercent) > 1.0) {
                    System.out.println("⚠️ SIGNIFICANT DIFFERENCE DETECTED!");
                    System.out.println("💡 Order placement may use outdated price");
                } else {
                    System.out.println("✅ Prices are reasonably close");
                }
            }
            
            System.out.println("\n🎯 IMPACT ON TRADING:");
            System.out.println("-" + "-".repeat(40));
            System.out.println("📋 Order Placement: Uses CryptoPriceService (1m candle close)");
            System.out.println("🔍 Signal Generation: Uses getCurrentMarketPrice (1m candle close)");
            System.out.println("📊 Most Accurate: DeltaApiClient Mark Price (real-time)");
            
            if (cryptoServicePrice != null && markPrice != null) {
                if (cryptoServicePrice > markPrice) {
                    System.out.println("⚠️ Order price is HIGHER than mark price");
                    System.out.println("💡 This could result in worse fills for BUY orders");
                } else {
                    System.out.println("⚠️ Order price is LOWER than mark price");
                    System.out.println("💡 This could result in worse fills for SELL orders");
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error comparing prices for " + symbol + ": " + e.getMessage());
        }
    }
}
