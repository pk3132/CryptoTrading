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
                🔔 *ALERT VERIFICATION - AGGRESSIVE CHART TECHNICAL ANALYSIS STRATEGY ENABLED*
                
                🔍 *Aggressive Chart Technical Analysis Alerts:*
                ✅ Price Movement (Uptrend/Downtrend)
                ✅ Bollinger Bands (Volatility)
                ✅ Support/Resistance Levels
                ✅ Candlestick Patterns (Green/Red)
                ✅ Trend Analysis (SMA5)
                ✅ Entry Notifications
                ✅ Stop Loss Alerts
                ✅ Take Profit Alerts
                
                🛡️ *Monitoring Alerts:*
                ✅ SL/TP Monitoring (Every 50 seconds)
                ✅ Position Status Updates
                ✅ Exit Notifications
                
                📱 *Notification Types:*
                • 🟢 BUY SIGNALS (Price Uptrend, Support Bounces, Bullish Patterns)
                • 📉 SELL SIGNALS (Price Downtrend, Resistance Rejections, Bearish Patterns)
                • 🎯 TAKE PROFIT
                • 🔴 STOP LOSS
                • 📊 STATUS UPDATES
                
                ⚡ *Strategy Details:*
                • Timeframe: 15 minutes
                • Risk-Reward: 1:2
                • Symbols: BTCUSD, ETHUSD
                • Professional Grade Aggressive Chart Analysis
                
                ⏰ *Verification Time:* %s
                
                🚀 *Aggressive Chart Technical Analysis Strategy is now active and monitored!*
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
         * Send EMA Trendline breakout alert with enhanced details
         */
        public void sendEmaTrendlineAlert(String symbol, String type, double entryPrice,
                                         double stopLoss, double takeProfit, String reason) {
            String message = String.format("""
                🎯 *EMA TRENDLINE BREAKOUT ALERT*
                
                📊 *Symbol:* %s
                🎯 *Action:* %s
                💰 *Entry Price:* $%.2f
                🛡️ *Stop Loss:* $%.2f
                🎯 *Take Profit:* $%.2f
                
                📈 *Strategy Analysis:*
                • EMA 200 Trend Filter
                • Swing Point Detection
                • Trendline Breakout Confirmation
                • 15-Minute Timeframe
                
                ⚡ *Strategy Details:*
                • EMA 200 + Trendline Breakout
                • Risk-Reward Ratio: 1:2
                • Market Order Execution
                • Professional Grade Strategy
                
                📝 *Reason:* %s
                ⏰ *Time:* %s
                
                🚨 *TRENDLINE BREAKOUT CONFIRMED!*
                """,
                symbol, type, entryPrice, stopLoss, takeProfit, reason,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );

            telegramService.sendTelegramMessage(message);
            System.out.println("✅ EMA Trendline alert sent for " + symbol);
        }

        /**
         * Send EMA 200 + Trendline alert with enhanced details
         */
        public void sendEMA200TrendlineAlert(String symbol, String type, double entryPrice,
                                           double stopLoss, double takeProfit, String reason) {
            String message = String.format("""
                🎯 *EMA 200 + TRENDLINE BREAKOUT ALERT*
                
                📊 *Symbol:* %s
                🎯 *Action:* %s
                💰 *Entry Price:* $%.2f
                🛡️ *Stop Loss:* $%.2f
                🎯 *Take Profit:* $%.2f
                
                🔍 *Strategy Analysis:*
                • EMA 200 Trend Filter
                • Swing Point Detection (Last 5 candles)
                • Trendline Fitting (Linear Regression)
                • Resistance/Support Breakouts
                • 15-Minute Timeframe
                
                ⚡ *Strategy Details:*
                • EMA 200 + Trendline Breakout
                • BUY: Price > EMA200 + breaks above descending resistance
                • SELL: Price < EMA200 + breaks below ascending support
                • Risk-Reward Ratio: 1:2 (2%% SL, 4%% TP)
                • Market Order Execution
                • Professional Grade Strategy
                
                📝 *Reason:* %s
                ⏰ *Time:* %s
                
                🚨 *TRENDLINE BREAKOUT CONFIRMED!*
                """,
                symbol, type, entryPrice, stopLoss, takeProfit, reason,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );

            telegramService.sendTelegramMessage(message);
            System.out.println("✅ EMA 200 + Trendline alert sent for " + symbol);
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
