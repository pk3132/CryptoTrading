package com.tradingbot.repository;

import com.tradingbot.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Trade Repository - Data access layer for Trade entities
 */
@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {

    /**
     * Find all open trades
     */
    List<Trade> findByStatus(String status);

    /**
     * Find open trades for a specific symbol
     */
    List<Trade> findBySymbolAndStatus(String symbol, String status);

    /**
     * Find all trades for a specific symbol
     */
    List<Trade> findBySymbol(String symbol);

    /**
     * Find trades by symbol and type
     */
    List<Trade> findBySymbolAndType(String symbol, String type);

    /**
     * Find trades created within a time range
     */
    List<Trade> findByEntryTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Find trades closed within a time range
     */
    List<Trade> findByExitTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Count open trades
     */
    long countByStatus(String status);

    /**
     * Count open trades for a specific symbol
     */
    long countBySymbolAndStatus(String symbol, String status);

    /**
     * Find the most recent trade for a symbol
     */
    Optional<Trade> findFirstBySymbolOrderByEntryTimeDesc(String symbol);

    /**
     * Find trades with positive P&L
     */
    @Query("SELECT t FROM Trade t WHERE t.pnl > 0 AND t.status = 'CLOSED'")
    List<Trade> findProfitableTrades();

    /**
     * Find trades with negative P&L
     */
    @Query("SELECT t FROM Trade t WHERE t.pnl < 0 AND t.status = 'CLOSED'")
    List<Trade> findLosingTrades();

    /**
     * Calculate total P&L for closed trades
     */
    @Query("SELECT SUM(t.pnl) FROM Trade t WHERE t.status = 'CLOSED'")
    Optional<Double> calculateTotalPnL();

    /**
     * Calculate total P&L for a specific symbol
     */
    @Query("SELECT SUM(t.pnl) FROM Trade t WHERE t.symbol = ?1 AND t.status = 'CLOSED'")
    Optional<Double> calculateTotalPnLBySymbol(String symbol);

    /**
     * Find trades that need monitoring (open trades)
     */
    @Query("SELECT t FROM Trade t WHERE t.status = 'OPEN' ORDER BY t.entryTime ASC")
    List<Trade> findTradesForMonitoring();
}
