package com.tradingbot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * CryptoStrategy2 Entity - For 9/20 EMA Crossover Strategy (1-Hour Timeframe)
 * 
 * Stores all Strategy 2 trades with comprehensive tracking
 */
@Entity
@Table(name = "crypto_strategy2")
public class CryptoStrategy2 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol", nullable = false)
    private String symbol; // BTCUSD, ETHUSD, SOLUSD

    @Column(name = "trade_type", nullable = false)
    private String tradeType; // BUY or SELL

    @Column(name = "entry_price", nullable = false)
    private Double entryPrice;

    @Column(name = "stop_loss", nullable = false)
    private Double stopLoss;

    @Column(name = "take_profit", nullable = false)
    private Double takeProfit;

    @Column(name = "quantity", nullable = false)
    private Double quantity;

    @Column(name = "leverage", nullable = false)
    private Double leverage = 5.0; // Strategy 2 uses 5x leverage

    @Column(name = "risk_reward_ratio", nullable = false)
    private Double riskRewardRatio = 2.0; // 1:2 ratio

    @Column(name = "timeframe", nullable = false)
    private String timeframe = "1h"; // 1-hour timeframe

    @Column(name = "strategy_name", nullable = false)
    private String strategyName = "Strategy 2 - 9/20 EMA Crossover";

    @Column(name = "entry_reason", length = 500)
    private String entryReason; // EMA crossover, pullback, etc.

    @Column(name = "status", nullable = false)
    private String status = "OPEN"; // OPEN, CLOSED, CANCELLED

    @Column(name = "exit_price")
    private Double exitPrice;

    @Column(name = "exit_reason", length = 500)
    private String exitReason; // Take Profit Hit, Stop Loss Hit, Manual Close

    @Column(name = "pnl")
    private Double pnl; // Profit or Loss

    @Column(name = "pnl_percentage")
    private Double pnlPercentage; // P&L as percentage

    @Column(name = "entry_time", nullable = false)
    private LocalDateTime entryTime;

    @Column(name = "exit_time")
    private LocalDateTime exitTime;

    @Column(name = "duration_minutes")
    private Long durationMinutes; // How long the trade was held

    @Column(name = "ema9_value")
    private Double ema9Value; // EMA 9 value at entry

    @Column(name = "ema20_value")
    private Double ema20Value; // EMA 20 value at entry

    @Column(name = "trend_direction")
    private String trendDirection; // BULLISH, BEARISH, NEUTRAL

    @Column(name = "market_condition")
    private String marketCondition; // TRENDING, SIDEWAYS, VOLATILE

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public CryptoStrategy2() {}

    public CryptoStrategy2(String symbol, String tradeType, Double entryPrice, Double stopLoss, 
                          Double takeProfit, Double quantity, String entryReason) {
        this.symbol = symbol;
        this.tradeType = tradeType;
        this.entryPrice = entryPrice;
        this.stopLoss = stopLoss;
        this.takeProfit = takeProfit;
        this.quantity = quantity;
        this.entryReason = entryReason;
        this.entryTime = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getTradeType() { return tradeType; }
    public void setTradeType(String tradeType) { this.tradeType = tradeType; }

    public Double getEntryPrice() { return entryPrice; }
    public void setEntryPrice(Double entryPrice) { this.entryPrice = entryPrice; }

    public Double getStopLoss() { return stopLoss; }
    public void setStopLoss(Double stopLoss) { this.stopLoss = stopLoss; }

    public Double getTakeProfit() { return takeProfit; }
    public void setTakeProfit(Double takeProfit) { this.takeProfit = takeProfit; }

    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }

    public Double getLeverage() { return leverage; }
    public void setLeverage(Double leverage) { this.leverage = leverage; }

    public Double getRiskRewardRatio() { return riskRewardRatio; }
    public void setRiskRewardRatio(Double riskRewardRatio) { this.riskRewardRatio = riskRewardRatio; }

    public String getTimeframe() { return timeframe; }
    public void setTimeframe(String timeframe) { this.timeframe = timeframe; }

    public String getStrategyName() { return strategyName; }
    public void setStrategyName(String strategyName) { this.strategyName = strategyName; }

    public String getEntryReason() { return entryReason; }
    public void setEntryReason(String entryReason) { this.entryReason = entryReason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getExitPrice() { return exitPrice; }
    public void setExitPrice(Double exitPrice) { this.exitPrice = exitPrice; }

    public String getExitReason() { return exitReason; }
    public void setExitReason(String exitReason) { this.exitReason = exitReason; }

    public Double getPnl() { return pnl; }
    public void setPnl(Double pnl) { this.pnl = pnl; }

    public Double getPnlPercentage() { return pnlPercentage; }
    public void setPnlPercentage(Double pnlPercentage) { this.pnlPercentage = pnlPercentage; }

    public LocalDateTime getEntryTime() { return entryTime; }
    public void setEntryTime(LocalDateTime entryTime) { this.entryTime = entryTime; }

    public LocalDateTime getExitTime() { return exitTime; }
    public void setExitTime(LocalDateTime exitTime) { this.exitTime = exitTime; }

    public Long getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Long durationMinutes) { this.durationMinutes = durationMinutes; }

    public Double getEma9Value() { return ema9Value; }
    public void setEma9Value(Double ema9Value) { this.ema9Value = ema9Value; }

    public Double getEma20Value() { return ema20Value; }
    public void setEma20Value(Double ema20Value) { this.ema20Value = ema20Value; }

    public String getTrendDirection() { return trendDirection; }
    public void setTrendDirection(String trendDirection) { this.trendDirection = trendDirection; }

    public String getMarketCondition() { return marketCondition; }
    public void setMarketCondition(String marketCondition) { this.marketCondition = marketCondition; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Business Logic Methods

    /**
     * Check if stop loss is hit
     */
    public boolean isStopLossHit(Double currentPrice) {
        if (tradeType.equals("BUY")) {
            return currentPrice <= stopLoss;
        } else { // SELL
            return currentPrice >= stopLoss;
        }
    }

    /**
     * Check if take profit is hit
     */
    public boolean isTakeProfitHit(Double currentPrice) {
        if (tradeType.equals("BUY")) {
            return currentPrice >= takeProfit;
        } else { // SELL
            return currentPrice <= takeProfit;
        }
    }

    /**
     * Calculate P&L
     */
    public void calculatePnL(Double exitPrice) {
        this.exitPrice = exitPrice;
        
        if (tradeType.equals("BUY")) {
            this.pnl = (exitPrice - entryPrice) * quantity * leverage;
        } else { // SELL
            this.pnl = (entryPrice - exitPrice) * quantity * leverage;
        }
        
        this.pnlPercentage = (pnl / (entryPrice * quantity * leverage)) * 100;
    }

    /**
     * Close the trade
     */
    public void closeTrade(Double exitPrice, String exitReason) {
        this.exitPrice = exitPrice;
        this.exitReason = exitReason;
        this.exitTime = LocalDateTime.now();
        this.status = "CLOSED";
        this.updatedAt = LocalDateTime.now();
        
        if (entryTime != null && exitTime != null) {
            this.durationMinutes = java.time.Duration.between(entryTime, exitTime).toMinutes();
        }
        
        calculatePnL(exitPrice);
    }

    /**
     * Get trade summary
     */
    public String getTradeSummary() {
        return String.format("Strategy2 %s %s: Entry=%.2f, Exit=%.2f, P&L=%.2f (%.2f%%)",
            tradeType, symbol, entryPrice, exitPrice != null ? exitPrice : 0.0,
            pnl != null ? pnl : 0.0, pnlPercentage != null ? pnlPercentage : 0.0);
    }

    @Override
    public String toString() {
        return "CryptoStrategy2{" +
                "id=" + id +
                ", symbol='" + symbol + '\'' +
                ", tradeType='" + tradeType + '\'' +
                ", entryPrice=" + entryPrice +
                ", stopLoss=" + stopLoss +
                ", takeProfit=" + takeProfit +
                ", status='" + status + '\'' +
                ", pnl=" + pnl +
                ", entryTime=" + entryTime +
                '}';
    }
}
