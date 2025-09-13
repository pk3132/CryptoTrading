package com.tradingbot.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Telegram Notification Service
 * Sends buy/sell signals and exit messages via Telegram
 */
@Service
public class TelegramNotificationService {

    private static final String TELEGRAM_BOT_TOKEN = "8013338174:AAHicTBShgIKtjawhzAZLqbGAzdMnYGZH8w";
    private static final String TELEGRAM_CHAT_ID = "1974091206";
    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage";
    
    private final RestTemplate restTemplate;

    public TelegramNotificationService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Send buy signal notification
     */
    public void sendBuySignal(String symbol, double entryPrice, double stopLoss, double takeProfit, String reason) {
        String message = createBuySignalMessage(symbol, entryPrice, stopLoss, takeProfit, reason);
        sendTelegramMessage(message);
    }

    /**
     * Send sell signal notification
     */
    public void sendSellSignal(String symbol, double entryPrice, double stopLoss, double takeProfit, String reason) {
        String message = createSellSignalMessage(symbol, entryPrice, stopLoss, takeProfit, reason);
        sendTelegramMessage(message);
    }

    /**
     * Send exit notification (stop loss or take profit)
     */
    public void sendExitNotification(String symbol, String signalType, double entryPrice, double exitPrice, 
                                   String exitReason, double pnl) {
        String message = createExitMessage(symbol, signalType, entryPrice, exitPrice, exitReason, pnl);
        sendTelegramMessage(message);
    }

    /**
     * Create buy signal message
     */
    private String createBuySignalMessage(String symbol, double entryPrice, double stopLoss, double takeProfit, String reason) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        return String.format("""
            🟢 *BUY SIGNAL - Strategy 1*
            
            📊 *Symbol:* %s
            💰 *Entry Price:* $%.2f
            🛡️ *Stop Loss:* $%.2f
            🎯 *Take Profit:* $%.2f
            📝 *Reason:* %s
            
            ⏰ *Time:* %s
            
            💡 *Risk-Reward:* 6:1
            📈 *Strategy:* 200-Day MA + Trendline Breakout
            
            ✅ *Ready to execute!*
            """, symbol, entryPrice, stopLoss, takeProfit, reason, timestamp);
    }

    /**
     * Create sell signal message
     */
    private String createSellSignalMessage(String symbol, double entryPrice, double stopLoss, double takeProfit, String reason) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        return String.format("""
            🔴 *SELL SIGNAL - Strategy 1*
            
            📊 *Symbol:* %s
            💰 *Entry Price:* $%.2f
            🛡️ *Stop Loss:* $%.2f
            🎯 *Take Profit:* $%.2f
            📝 *Reason:* %s
            
            ⏰ *Time:* %s
            
            💡 *Risk-Reward:* 6:1
            📉 *Strategy:* 200-Day MA + Trendline Breakout
            
            ✅ *Ready to execute!*
            """, symbol, entryPrice, stopLoss, takeProfit, reason, timestamp);
    }

    /**
     * Create exit message
     */
    private String createExitMessage(String symbol, String signalType, double entryPrice, double exitPrice, 
                                   String exitReason, double pnl) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String emoji = pnl > 0 ? "💰" : "📉";
        String status = pnl > 0 ? "PROFIT" : "LOSS";
        
        return String.format("""
            %s *EXIT NOTIFICATION - Strategy 1*
            
            📊 *Symbol:* %s
            📈 *Signal Type:* %s
            💰 *Entry Price:* $%.2f
            💸 *Exit Price:* $%.2f
            📝 *Exit Reason:* %s
            
            %s *P&L:* $%.2f (%s)
            
            ⏰ *Time:* %s
            
            📊 *Trade Completed*
            """, emoji, symbol, signalType, entryPrice, exitPrice, exitReason, 
                emoji, pnl, status, timestamp);
    }

    /**
     * Send message to Telegram
     */
    public void sendTelegramMessage(String message) {
        try {
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            // Create form data
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("chat_id", TELEGRAM_CHAT_ID);
            formData.add("text", message);
            formData.add("parse_mode", "Markdown");
            
            // Create HTTP entity
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);
            
            // Send POST request
            ResponseEntity<String> response = restTemplate.postForEntity(TELEGRAM_API_URL, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("✅ Telegram notification sent successfully");
            } else {
                System.err.println("❌ Failed to send Telegram notification: " + response.getStatusCode());
                System.err.println("Response: " + response.getBody());
            }
        } catch (Exception e) {
            System.err.println("❌ Error sending Telegram notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send daily summary
     */
    public void sendDailySummary(int totalTrades, int winningTrades, int losingTrades, 
                               double totalPnl, double winRate) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        
        String message = String.format("""
            📊 *DAILY SUMMARY - Strategy 1*
            
            📅 *Date:* %s
            
            📈 *Trades Today:*
            • Total: %d
            • Wins: %d
            • Losses: %d
            • Win Rate: %.1f%%
            
            💰 *P&L:*
            • Total: $%.2f
            • Status: %s
            
            🎯 *Strategy Performance*
            ✅ *200-Day MA + Trendline Breakout*
            """, timestamp, totalTrades, winningTrades, losingTrades, winRate, 
                totalPnl, totalPnl > 0 ? "PROFIT" : "LOSS");
        
        sendTelegramMessage(message);
    }

    /**
     * Send test message
     */
    public void sendTestMessage() {
        String message = """
            🤖 *Trading Bot Test*
            
            ✅ Telegram integration is working!
            
            📊 *Strategy 1* is ready to send notifications:
            • Buy/Sell signals
            • Exit notifications
            • Daily summaries
            
            🚀 *Ready for live trading!*
            """;
        
        sendTelegramMessage(message);
    }
}
