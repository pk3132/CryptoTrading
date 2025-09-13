package com.tradingbot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Trade Entity - Represents an active trading position
 */
@Entity
@Table(name = "trades")
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private String type; // BUY or SELL

    @Column(nullable = false)
    private Double entryPrice;

    @Column(nullable = false)
    private Double stopLoss;

    @Column(nullable = false)
    private Double takeProfit;

    @Column(nullable = false)
    private Double quantity;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private String status; // OPEN, CLOSED, STOPPED

    @Column
    private Double exitPrice;

    @Column
    private Double pnl;

    @Column
    private String exitReason;

    @Column(nullable = false)
    private LocalDateTime entryTime;

    @Column
    private LocalDateTime exitTime;

    // Constructors
    public Trade() {}

    public Trade(String symbol, String type, Double entryPrice, Double stopLoss, 
                 Double takeProfit, Double quantity, String reason) {
        this.symbol = symbol;
        this.type = type;
        this.entryPrice = entryPrice;
        this.stopLoss = stopLoss;
        this.takeProfit = takeProfit;
        this.quantity = quantity;
        this.reason = reason;
        this.status = "OPEN";
        this.entryTime = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getEntryPrice() {
        return entryPrice;
    }

    public void setEntryPrice(Double entryPrice) {
        this.entryPrice = entryPrice;
    }

    public Double getStopLoss() {
        return stopLoss;
    }

    public void setStopLoss(Double stopLoss) {
        this.stopLoss = stopLoss;
    }

    public Double getTakeProfit() {
        return takeProfit;
    }

    public void setTakeProfit(Double takeProfit) {
        this.takeProfit = takeProfit;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getExitPrice() {
        return exitPrice;
    }

    public void setExitPrice(Double exitPrice) {
        this.exitPrice = exitPrice;
    }

    public Double getPnl() {
        return pnl;
    }

    public void setPnl(Double pnl) {
        this.pnl = pnl;
    }

    public String getExitReason() {
        return exitReason;
    }

    public void setExitReason(String exitReason) {
        this.exitReason = exitReason;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(LocalDateTime entryTime) {
        this.entryTime = entryTime;
    }

    public LocalDateTime getExitTime() {
        return exitTime;
    }

    public void setExitTime(LocalDateTime exitTime) {
        this.exitTime = exitTime;
    }

    /**
     * Calculate P&L for the trade
     */
    public Double calculatePnL(Double currentPrice) {
        if (type.equals("BUY")) {
            return (currentPrice - entryPrice) * quantity;
        } else {
            return (entryPrice - currentPrice) * quantity;
        }
    }

    /**
     * Check if stop loss is hit
     */
    public boolean isStopLossHit(Double currentPrice) {
        if (type.equals("BUY")) {
            return currentPrice <= stopLoss;
        } else {
            return currentPrice >= stopLoss;
        }
    }

    /**
     * Check if take profit is hit
     */
    public boolean isTakeProfitHit(Double currentPrice) {
        if (type.equals("BUY")) {
            return currentPrice >= takeProfit;
        } else {
            return currentPrice <= takeProfit;
        }
    }

    /**
     * Close the trade
     */
    public void closeTrade(Double exitPrice, String exitReason) {
        this.exitPrice = exitPrice;
        this.exitReason = exitReason;
        this.pnl = calculatePnL(exitPrice);
        this.status = "CLOSED";
        this.exitTime = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return String.format("Trade{id=%d, symbol='%s', type=%s, entryPrice=%.2f, stopLoss=%.2f, takeProfit=%.2f, status=%s}",
                id, symbol, type, entryPrice, stopLoss, takeProfit, status);
    }
}
