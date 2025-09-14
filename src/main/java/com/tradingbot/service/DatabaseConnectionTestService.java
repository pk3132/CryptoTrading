package com.tradingbot.service;

import com.tradingbot.model.CryptoStrategy2;
import com.tradingbot.repository.CryptoStrategy2Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Database Connection Test Service
 * 
 * Tests database connectivity and Strategy 2 repository functionality
 */
@Service
public class DatabaseConnectionTestService {

    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private CryptoStrategy2Repository strategy2Repository;
    
    @Autowired
    private TelegramNotificationService telegramService;

    /**
     * Test database connection
     */
    public boolean testDatabaseConnection() {
        try {
            System.out.println("🔍 Testing database connection...");
            
            // Test basic connection
            try (Connection connection = dataSource.getConnection()) {
                if (connection != null && !connection.isClosed()) {
                    System.out.println("✅ Database connection successful!");
                    System.out.println("Database URL: " + connection.getMetaData().getURL());
                    System.out.println("Database Product: " + connection.getMetaData().getDatabaseProductName());
                    System.out.println("Database Version: " + connection.getMetaData().getDatabaseProductVersion());
                    return true;
                }
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Database connection failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Test Strategy 2 repository functionality
     */
    public boolean testStrategy2Repository() {
        try {
            System.out.println("🔍 Testing Strategy 2 repository...");
            
            // Test repository methods
            long count = strategy2Repository.count();
            System.out.println("✅ Repository count() method working. Current count: " + count);
            
            // Test if we can create a simple entity
            CryptoStrategy2 testEntity = new CryptoStrategy2();
            testEntity.setSymbol("TEST");
            testEntity.setTradeType("BUY");
            testEntity.setEntryPrice(100.0);
            testEntity.setStopLoss(95.0);
            testEntity.setTakeProfit(110.0);
            testEntity.setQuantity(1.0);
            testEntity.setLeverage(25.0);
            testEntity.setRiskRewardRatio(3.0);
            testEntity.setTimeframe("1h");
            testEntity.setStrategyName("Strategy 2 - 9/20 EMA Crossover");
            testEntity.setEntryReason("Database connection test");
            testEntity.setStatus("OPEN");
            testEntity.setEntryTime(LocalDateTime.now());
            testEntity.setEma9Value(98.0);
            testEntity.setEma20Value(96.0);
            testEntity.setTrendDirection("BULLISH");
            testEntity.setMarketCondition("TRENDING");
            testEntity.setCreatedAt(LocalDateTime.now());
            
            // Try to save
            CryptoStrategy2 savedEntity = strategy2Repository.save(testEntity);
            if (savedEntity != null && savedEntity.getId() != null) {
                System.out.println("✅ Repository save() method working. Saved entity ID: " + savedEntity.getId());
                
                // Clean up test data
                strategy2Repository.delete(savedEntity);
                System.out.println("✅ Repository delete() method working. Test data cleaned up.");
                
                return true;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Strategy 2 repository test failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Run complete database test
     */
    public void runCompleteDatabaseTest() {
        try {
            System.out.println("🧪 Starting complete database test...");
            
            // Test 1: Database connection
            boolean connectionOk = testDatabaseConnection();
            
            // Test 2: Repository functionality
            boolean repositoryOk = testStrategy2Repository();
            
            // Send results via Telegram
            String testResults = String.format("""
                🧪 *DATABASE CONNECTION TEST RESULTS*
                
                🔗 *Database Connection:*
                %s
                
                📊 *Strategy 2 Repository:*
                %s
                
                📋 *Overall Status:*
                %s
                
                ⏰ *Test Time:* %s
                """,
                connectionOk ? "✅ SUCCESS" : "❌ FAILED",
                repositoryOk ? "✅ SUCCESS" : "❌ FAILED",
                (connectionOk && repositoryOk) ? "✅ ALL TESTS PASSED" : "❌ SOME TESTS FAILED",
                LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
            
            telegramService.sendTelegramMessage(testResults);
            
            if (connectionOk && repositoryOk) {
                System.out.println("🎉 All database tests passed!");
            } else {
                System.out.println("❌ Some database tests failed!");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Complete database test failed: " + e.getMessage());
            e.printStackTrace();
            telegramService.sendTelegramMessage("❌ Database test failed: " + e.getMessage());
        }
    }
}
