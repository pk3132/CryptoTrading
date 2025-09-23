package com.tradingbot.test;

import com.tradingbot.model.Trade;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Entry Time Test
 * Test the entry time accuracy
 */
public class EntryTimeTest {
    
    public static void main(String[] args) {
        System.out.println("⏰ ENTRY TIME TEST");
        System.out.println("=" + "=".repeat(40));
        System.out.println("📊 Testing entry time accuracy");
        System.out.println();
        
        try {
            // Test current time
            LocalDateTime now = LocalDateTime.now();
            System.out.println("🕐 Current System Time: " + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
            
            // Create a test trade
            Trade testTrade = new Trade("BTCUSD", "BUY", 115741.0, 110000.0, 127000.0, 1.0, "Test Trade");
            
            System.out.println("📊 Test Trade Created:");
            System.out.println("   Symbol: " + testTrade.getSymbol());
            System.out.println("   Type: " + testTrade.getType());
            System.out.println("   Entry Price: $" + testTrade.getEntryPrice());
            System.out.println("   Entry Time: " + testTrade.getEntryTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
            
            // Check time difference
            LocalDateTime entryTime = testTrade.getEntryTime();
            long timeDiff = java.time.Duration.between(now, entryTime).toMillis();
            
            System.out.println("\n⏰ TIME ANALYSIS:");
            System.out.println("   System Time: " + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
            System.out.println("   Entry Time: " + entryTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
            System.out.println("   Time Difference: " + timeDiff + " milliseconds");
            
            if (Math.abs(timeDiff) < 1000) { // Less than 1 second
                System.out.println("   ✅ Entry time is accurate (within 1 second)");
            } else {
                System.out.println("   ⚠️ Entry time may have delay");
            }
            
            // Test multiple trades
            System.out.println("\n🔄 MULTIPLE TRADES TEST:");
            for (int i = 1; i <= 3; i++) {
                Trade trade = new Trade("ETHUSD", "SELL", 4467.0, 4700.0, 4000.0, 1.0, "Test Trade " + i);
                LocalDateTime tradeTime = trade.getEntryTime();
                System.out.println("   Trade " + i + ": " + tradeTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
                
                // Small delay between trades
                Thread.sleep(100);
            }
            
            System.out.println("\n✅ Entry time test completed!");
            System.out.println("📊 Entry times are set to current system time when trade is created");
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
