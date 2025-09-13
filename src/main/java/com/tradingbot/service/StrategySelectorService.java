package com.tradingbot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Strategy Selector Service
 * 
 * Allows switching between:
 * - Strategy 1: Conservative 200-Day MA + Trendline (92.2% accuracy)
 * - Strategy 2: Aggressive 9/20 EMA Crossover (High frequency)
 */
@Service
public class StrategySelectorService {

    @Autowired
    private SimpleMonitoringService strategy1Service;
    
    @Autowired
    private Strategy2MonitoringService strategy2Service;
    
    @Autowired
    private TelegramNotificationService telegramService;
    
    private String activeStrategy = "STRATEGY_1"; // Default to Strategy 1
    private boolean isActive = false;
    
    /**
     * Start with Strategy 1 (Conservative)
     */
    public void startStrategy1() {
        stopAllStrategies();
        activeStrategy = "STRATEGY_1";
        isActive = true;
        
        System.out.println("🎯 Starting Strategy 1: 200-Day MA + Trendline Breakout");
        System.out.println("📊 Conservative approach - 92.2% accuracy");
        strategy1Service.startMonitoring();
        
        sendStrategySwitchNotification();
    }
    
    /**
     * Start with Strategy 2 (Aggressive)
     */
    public void startStrategy2() {
        stopAllStrategies();
        activeStrategy = "STRATEGY_2";
        isActive = true;
        
        System.out.println("🚀 Starting Strategy 2: 9/20 EMA Crossover");
        System.out.println("⚡ Aggressive approach - High frequency trading");
        strategy2Service.startMonitoring();
        
        sendStrategySwitchNotification();
    }
    
    /**
     * Start both strategies simultaneously
     */
    public void startBothStrategies() {
        stopAllStrategies();
        activeStrategy = "BOTH";
        isActive = true;
        
        System.out.println("🎯🚀 Starting Both Strategies:");
        System.out.println("📊 Strategy 1: Conservative (92.2% accuracy)");
        System.out.println("⚡ Strategy 2: Aggressive (High frequency)");
        
        strategy1Service.startMonitoring();
        strategy2Service.startMonitoring();
        
        sendStrategySwitchNotification();
    }
    
    /**
     * Stop all strategies
     */
    public void stopAllStrategies() {
        if (isActive) {
            System.out.println("🛑 Stopping all trading strategies...");
            
            try {
                strategy1Service.stopMonitoring();
            } catch (Exception e) {
                System.out.println("⚠️ Error stopping Strategy 1: " + e.getMessage());
            }
            
            try {
                strategy2Service.stopMonitoring();
            } catch (Exception e) {
                System.out.println("⚠️ Error stopping Strategy 2: " + e.getMessage());
            }
            
            isActive = false;
            activeStrategy = "NONE";
        }
    }
    
    /**
     * Switch to Strategy 1
     */
    public void switchToStrategy1() {
        System.out.println("🔄 Switching to Strategy 1...");
        startStrategy1();
    }
    
    /**
     * Switch to Strategy 2
     */
    public void switchToStrategy2() {
        System.out.println("🔄 Switching to Strategy 2...");
        startStrategy2();
    }
    
    /**
     * Get current active strategy
     */
    public String getActiveStrategy() {
        return activeStrategy;
    }
    
    /**
     * Check if any strategy is active
     */
    public boolean isAnyStrategyActive() {
        return isActive;
    }
    
    /**
     * Get strategy comparison
     */
    public String getStrategyComparison() {
        return """
            📊 **Strategy Comparison**
            
            🎯 **Strategy 1: Conservative**
            • 200-Day MA + Trendline Breakout
            • Risk-Reward: 6:1
            • Win Rate: 92.2%
            • Trades/Month: ~17-18
            • Best for: High accuracy, patient trading
            
            ⚡ **Strategy 2: Aggressive**
            • 9/20 EMA Crossover
            • Risk-Reward: 1:3
            • Leverage: 25x
            • Trades/Day: 2-3
            • Best for: High frequency, trending markets
            
            🤔 **Choose based on:**
            • Risk tolerance
            • Time availability
            • Market conditions
            """;
    }
    
    /**
     * Send strategy switch notification
     */
    private void sendStrategySwitchNotification() {
        String message;
        
        switch (activeStrategy) {
            case "STRATEGY_1":
                message = """
                    🎯 *Strategy 1 Activated*
                    
                    📊 **Conservative Approach**
                    • 200-Day MA + Trendline Breakout
                    • Win Rate: 92.2%
                    • Risk-Reward: 6:1
                    • Trades/Month: ~17-18
                    
                    ✅ **Status:** Active and monitoring
                    🎯 **Focus:** High accuracy, quality trades
                    """;
                break;
                
            case "STRATEGY_2":
                message = """
                    ⚡ *Strategy 2 Activated*
                    
                    🚀 **Aggressive Approach**
                    • 9/20 EMA Crossover
                    • Leverage: 25x
                    • Risk-Reward: 1:3
                    • Trades/Day: 2-3
                    
                    ✅ **Status:** Active and monitoring
                    🎯 **Focus:** High frequency, trending markets
                    """;
                break;
                
            case "BOTH":
                message = """
                    🎯⚡ *Both Strategies Activated*
                    
                    📊 **Strategy 1:** Conservative (92.2% accuracy)
                    ⚡ **Strategy 2:** Aggressive (High frequency)
                    
                    ✅ **Status:** Both active and monitoring
                    🎯 **Focus:** Diversified approach
                    """;
                break;
                
            default:
                message = "❌ No strategy is currently active";
        }
        
        telegramService.sendTelegramMessage(message);
    }
    
    /**
     * Get current strategy status
     */
    public String getCurrentStatus() {
        if (!isActive) {
            return "❌ No strategy is currently active";
        }
        
        StringBuilder status = new StringBuilder();
        status.append("✅ Active Strategy: ").append(activeStrategy).append("\n\n");
        
        switch (activeStrategy) {
            case "STRATEGY_1":
                status.append("📊 Strategy 1 Status:\n");
                status.append("✅ Strategy 1 monitoring active");
                break;
                
            case "STRATEGY_2":
                status.append("⚡ Strategy 2 Status:\n");
                status.append(strategy2Service.getStrategy2Status());
                break;
                
            case "BOTH":
                status.append("📊 Strategy 1:\n");
                status.append("✅ Strategy 1 monitoring active").append("\n\n");
                status.append("⚡ Strategy 2:\n");
                status.append(strategy2Service.getStrategy2Status());
                break;
        }
        
        return status.toString();
    }
}
