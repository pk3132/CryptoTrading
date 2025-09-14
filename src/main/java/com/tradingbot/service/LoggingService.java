package com.tradingbot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Centralized Logging Service for Trading Bot
 * 
 * Provides structured logging methods for different types of events:
 * - Trade execution logs
 * - Error logs with full stack traces
 * - Performance logs
 * - Database operation logs
 * - API call logs
 */
@Service
public class LoggingService {

    private static final Logger logger = LoggerFactory.getLogger(LoggingService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * Log trade execution with detailed information
     */
    public void logTradeExecution(String strategy, String symbol, String tradeType, 
                                 Double entryPrice, Double stopLoss, Double takeProfit, 
                                 String reason, String status) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        
        logger.info("""
            📊 TRADE EXECUTION LOG
            ⏰ Time: {}
            🎯 Strategy: {}
            💰 Symbol: {}
            📈 Type: {}
            💵 Entry: ${}
            🛡️ Stop Loss: ${}
            🎯 Take Profit: ${}
            📝 Reason: {}
            ✅ Status: {}
            """, 
            timestamp, strategy, symbol, tradeType, 
            String.format("%.2f", entryPrice),
            String.format("%.2f", stopLoss),
            String.format("%.2f", takeProfit),
            reason, status
        );
    }

    /**
     * Log database operations with timing
     */
    public void logDatabaseOperation(String operation, String table, String details, long durationMs) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        
        logger.debug("""
            🗄️ DATABASE OPERATION
            ⏰ Time: {}
            🔧 Operation: {}
            📊 Table: {}
            📝 Details: {}
            ⚡ Duration: {}ms
            """,
            timestamp, operation, table, details, durationMs
        );
    }

    /**
     * Log API calls with response details
     */
    public void logApiCall(String endpoint, String method, int statusCode, long durationMs, String details) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String statusEmoji = statusCode >= 200 && statusCode < 300 ? "✅" : "❌";
        
        logger.debug("""
            🌐 API CALL LOG
            ⏰ Time: {}
            {} Status: {}
            🔗 Endpoint: {}
            📡 Method: {}
            ⚡ Duration: {}ms
            📝 Details: {}
            """,
            timestamp, statusEmoji, statusCode, endpoint, method, durationMs, details
        );
    }

    /**
     * Log strategy performance metrics
     */
    public void logStrategyPerformance(String strategy, String symbol, int signalsGenerated, 
                                     int positionsOpened, double winRate, long executionTimeMs) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        
        logger.info("""
            📈 STRATEGY PERFORMANCE
            ⏰ Time: {}
            🎯 Strategy: {}
            💰 Symbol: {}
            📊 Signals Generated: {}
            🚀 Positions Opened: {}
            🏆 Win Rate: {:.2f}%
            ⚡ Execution Time: {}ms
            """,
            timestamp, strategy, symbol, signalsGenerated, positionsOpened, winRate, executionTimeMs
        );
    }

    /**
     * Log market data updates
     */
    public void logMarketDataUpdate(String symbol, Double price, String source, long latencyMs) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        
        logger.debug("""
            📊 MARKET DATA UPDATE
            ⏰ Time: {}
            💰 Symbol: {}
            💵 Price: ${}
            📡 Source: {}
            ⚡ Latency: {}ms
            """,
            timestamp, symbol, String.format("%.2f", price), source, latencyMs
        );
    }

    /**
     * Log notification delivery
     */
    public void logNotificationDelivery(String type, String recipient, String status, String details) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String statusEmoji = "SUCCESS".equals(status) ? "✅" : "❌";
        
        logger.info("""
            📱 NOTIFICATION LOG
            ⏰ Time: {}
            {} Status: {}
            📧 Type: {}
            👤 Recipient: {}
            📝 Details: {}
            """,
            timestamp, statusEmoji, status, type, recipient, details
        );
    }

    /**
     * Log system health metrics
     */
    public void logSystemHealth(String component, String status, double cpuUsage, long memoryUsage, String details) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String statusEmoji = "HEALTHY".equals(status) ? "✅" : "⚠️";
        
        logger.info("""
            🏥 SYSTEM HEALTH
            ⏰ Time: {}
            {} Status: {}
            🔧 Component: {}
            💻 CPU Usage: {:.1f}%
            🧠 Memory: {}MB
            📝 Details: {}
            """,
            timestamp, statusEmoji, status, component, cpuUsage, memoryUsage / (1024 * 1024), details
        );
    }

    /**
     * Log configuration changes
     */
    public void logConfigurationChange(String component, String parameter, String oldValue, String newValue) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        
        logger.info("""
            ⚙️ CONFIGURATION CHANGE
            ⏰ Time: {}
            🔧 Component: {}
            📋 Parameter: {}
            🔄 Old Value: {}
            🆕 New Value: {}
            """,
            timestamp, component, parameter, oldValue, newValue
        );
    }

    /**
     * Log application lifecycle events
     */
    public void logApplicationLifecycle(String event, String details) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        
        logger.info("""
            🔄 APPLICATION LIFECYCLE
            ⏰ Time: {}
            📅 Event: {}
            📝 Details: {}
            """,
            timestamp, event, details
        );
    }

    /**
     * Log error with full context and stack trace
     */
    public void logErrorWithContext(String context, String operation, Exception error, String additionalInfo) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        
        logger.error("""
            ❌ ERROR WITH CONTEXT
            ⏰ Time: {}
            📍 Context: {}
            🔧 Operation: {}
            💥 Error: {}
            📝 Additional Info: {}
            🔍 Stack Trace:
            """,
            timestamp, context, operation, error.getMessage(), additionalInfo
        );
        
        logger.error("Full stack trace:", error);
    }

    /**
     * Log warning with context
     */
    public void logWarningWithContext(String context, String operation, String warning, String details) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        
        logger.warn("""
            ⚠️ WARNING WITH CONTEXT
            ⏰ Time: {}
            📍 Context: {}
            🔧 Operation: {}
            ⚠️ Warning: {}
            📝 Details: {}
            """,
            timestamp, context, operation, warning, details
        );
    }

    /**
     * Log debug information with context
     */
    public void logDebugWithContext(String context, String operation, String details) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        
        logger.debug("""
            🔍 DEBUG INFO
            ⏰ Time: {}
            📍 Context: {}
            🔧 Operation: {}
            📝 Details: {}
            """,
            timestamp, context, operation, details
        );
    }
}
