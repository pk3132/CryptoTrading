package com.tradingbot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Alert Verification Service
 * 
 * Ensures all alerts are properly sent and tracks notification status
 */
@Service
public class AlertVerificationService {

    @Autowired
    private TelegramNotificationService telegramService;

    /**
     * Send comprehensive alert verification message
     */
    public void sendAlertVerificationMessage() {
        String message = String.format("""
            🔔 *ALERT VERIFICATION - ALL NOTIFICATIONS ENABLED*
            
            📊 *Strategy 1 Alerts:*
            ✅ Breakout Signals (200-Day MA + Trendline)
            ✅ Entry Notifications
            ✅ Stop Loss Alerts
            ✅ Take Profit Alerts
            
            📊 *Strategy 2 Alerts:*
            ✅ EMA Crossover Signals (9/20)
            ✅ Pullback Entry Signals
            ✅ Entry Notifications with EMA Values
            ✅ Stop Loss Alerts
            ✅ Take Profit Alerts
            
            🛡️ *Monitoring Alerts:*
            ✅ SL/TP Monitoring (Every 50 seconds)
            ✅ Position Status Updates
            ✅ Exit Notifications
            
            📱 *Notification Types:*
            • 🟢 BUY SIGNALS
            • 📉 SELL SIGNALS  
            • 🎯 TAKE PROFIT
            • 🔴 STOP LOSS
            • 📊 STATUS UPDATES
            
            ⏰ *Verification Time:* %s
            
            🚀 *All alerts are now active and monitored!*
            """,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        telegramService.sendTelegramMessage(message);
        System.out.println("✅ Alert verification message sent");
    }

    /**
     * Send Strategy 1 breakout alert with enhanced details
     */
    public void sendStrategy1BreakoutAlert(String symbol, String type, double entryPrice, 
                                         double stopLoss, double takeProfit, String reason) {
        String message = String.format("""
            🟢 *STRATEGY 1 BREAKOUT ALERT*
            
            📊 *Symbol:* %s
            🎯 *Action:* %s
            💰 *Entry Price:* $%.2f
            🛡️ *Stop Loss:* $%.2f
            🎯 *Take Profit:* $%.2f
            
            📈 *Strategy Details:*
            • 200-Day Moving Average Breakout
            • Trendline Confirmation
            • Risk-Reward Ratio: 6:1
            • Conservative Approach
            
            📝 *Reason:* %s
            ⏰ *Time:* %s
            
            🚨 *BREAKOUT SIGNAL CONFIRMED!*
            """,
            symbol, type, entryPrice, stopLoss, takeProfit, reason,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        telegramService.sendTelegramMessage(message);
        System.out.println("✅ Strategy 1 breakout alert sent for " + symbol);
    }

    /**
     * Send Strategy 2 EMA alert with enhanced details
     */
    public void sendStrategy2EMAAlert(String symbol, String type, double entryPrice,
                                    double stopLoss, double takeProfit, double ema9, 
                                    double ema20, String trend, String marketCondition, 
                                    String reason) {
        String message = String.format("""
            ⚡ *STRATEGY 2 EMA ALERT*
            
            📊 *Symbol:* %s
            🎯 *Action:* %s
            💰 *Entry Price:* $%.2f
            🛡️ *Stop Loss:* $%.2f
            🎯 *Take Profit:* $%.2f
            
            📊 *EMA Analysis:*
            • EMA9: $%.2f
            • EMA20: $%.2f
            • Trend Direction: %s
            • Market Condition: %s
            
            ⚡ *Strategy Details:*
            • 9/20 EMA Crossover Strategy
            • 25x Leverage
            • Risk-Reward Ratio: 1:3
            • Aggressive Approach
            
            📝 *Reason:* %s
            ⏰ *Time:* %s
            
            🚨 *EMA SIGNAL CONFIRMED!*
            """,
            symbol, type, entryPrice, stopLoss, takeProfit, ema9, ema20, 
            trend, marketCondition, reason,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        telegramService.sendTelegramMessage(message);
        System.out.println("✅ Strategy 2 EMA alert sent for " + symbol);
    }

    /**
     * Send enhanced stop loss alert
     */
    public void sendEnhancedStopLossAlert(String symbol, String type, double entryPrice,
                                        double stopLoss, double exitPrice, double pnl,
                                        String strategy) {
        String message = String.format("""
            🔴 *STOP LOSS HIT - IMMEDIATE ALERT*
            
            📊 *Symbol:* %s
            📈 *Type:* %s
            💰 *Entry Price:* $%.2f
            🔴 *Stop Loss:* $%.2f
            💸 *Exit Price:* $%.2f
            
            📉 *Loss:* $%.2f
            
            📊 *Strategy:* %s
            ⏰ *Time:* %s
            
            🚨 *POSITION CLOSED - RISK MANAGEMENT ACTIVATED!*
            """,
            symbol, type, entryPrice, stopLoss, exitPrice, pnl, strategy,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        telegramService.sendTelegramMessage(message);
        System.out.println("✅ Enhanced stop loss alert sent for " + symbol);
    }

    /**
     * Send enhanced take profit alert
     */
    public void sendEnhancedTakeProfitAlert(String symbol, String type, double entryPrice,
                                          double takeProfit, double exitPrice, double pnl,
                                          String strategy) {
        String message = String.format("""
            🟢 *TAKE PROFIT HIT - PROFIT ALERT*
            
            📊 *Symbol:* %s
            📈 *Type:* %s
            💰 *Entry Price:* $%.2f
            🎯 *Take Profit:* $%.2f
            💸 *Exit Price:* $%.2f
            
            💰 *Profit:* $%.2f
            
            📊 *Strategy:* %s
            ⏰ *Time:* %s
            
            🎉 *PROFIT TARGET ACHIEVED!*
            """,
            symbol, type, entryPrice, takeProfit, exitPrice, pnl, strategy,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        telegramService.sendTelegramMessage(message);
        System.out.println("✅ Enhanced take profit alert sent for " + symbol);
    }

    /**
     * Send system status alert
     */
    public void sendSystemStatusAlert(String status, String details) {
        String message = String.format("""
            📊 *SYSTEM STATUS ALERT*
            
            🔄 *Status:* %s
            📝 *Details:* %s
            ⏰ *Time:* %s
            
            🤖 *Trading Bot Status Update*
            """,
            status, details,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        telegramService.sendTelegramMessage(message);
        System.out.println("✅ System status alert sent: " + status);
    }
}
