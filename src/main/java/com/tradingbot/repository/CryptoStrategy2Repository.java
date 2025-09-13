package com.tradingbot.repository;

import com.tradingbot.model.CryptoStrategy2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for CryptoStrategy2 Entity
 * Handles database operations for Strategy 2 trades
 */
@Repository
public interface CryptoStrategy2Repository extends JpaRepository<CryptoStrategy2, Long> {

    /**
     * Find all open trades for a specific symbol
     */
    List<CryptoStrategy2> findBySymbolAndStatus(String symbol, String status);

    /**
     * Find all open trades
     */
    List<CryptoStrategy2> findByStatus(String status);

    /**
     * Find trades by symbol and trade type
     */
    List<CryptoStrategy2> findBySymbolAndTradeType(String symbol, String tradeType);

    /**
     * Find trades by date range
     */
    List<CryptoStrategy2> findByEntryTimeBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find trades by symbol and date range
     */
    List<CryptoStrategy2> findBySymbolAndEntryTimeBetween(String symbol, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find closed trades with profit
     */
    List<CryptoStrategy2> findByStatusAndPnlGreaterThan(String status, Double pnl);

    /**
     * Find closed trades with loss
     */
    List<CryptoStrategy2> findByStatusAndPnlLessThan(String status, Double pnl);

    /**
     * Count total trades for a symbol
     */
    long countBySymbol(String symbol);

    /**
     * Count open trades for a symbol
     */
    long countBySymbolAndStatus(String symbol, String status);

    /**
     * Count winning trades for a symbol
     */
    @Query("SELECT COUNT(c) FROM CryptoStrategy2 c WHERE c.symbol = :symbol AND c.status = 'CLOSED' AND c.pnl > 0")
    long countWinningTradesBySymbol(@Param("symbol") String symbol);

    /**
     * Count losing trades for a symbol
     */
    @Query("SELECT COUNT(c) FROM CryptoStrategy2 c WHERE c.symbol = :symbol AND c.status = 'CLOSED' AND c.pnl < 0")
    long countLosingTradesBySymbol(@Param("symbol") String symbol);

    /**
     * Get total P&L for a symbol
     */
    @Query("SELECT COALESCE(SUM(c.pnl), 0) FROM CryptoStrategy2 c WHERE c.symbol = :symbol AND c.status = 'CLOSED'")
    Double getTotalPnLBySymbol(@Param("symbol") String symbol);

    /**
     * Get total P&L for all trades
     */
    @Query("SELECT COALESCE(SUM(c.pnl), 0) FROM CryptoStrategy2 c WHERE c.status = 'CLOSED'")
    Double getTotalPnL();

    /**
     * Get win rate for a symbol
     */
    @Query("SELECT CASE WHEN COUNT(c) = 0 THEN 0 ELSE " +
           "CAST(SUM(CASE WHEN c.pnl > 0 THEN 1 ELSE 0 END) AS DOUBLE) / COUNT(c) * 100 END " +
           "FROM CryptoStrategy2 c WHERE c.symbol = :symbol AND c.status = 'CLOSED'")
    Double getWinRateBySymbol(@Param("symbol") String symbol);

    /**
     * Get overall win rate
     */
    @Query("SELECT CASE WHEN COUNT(c) = 0 THEN 0 ELSE " +
           "CAST(SUM(CASE WHEN c.pnl > 0 THEN 1 ELSE 0 END) AS DOUBLE) / COUNT(c) * 100 END " +
           "FROM CryptoStrategy2 c WHERE c.status = 'CLOSED'")
    Double getOverallWinRate();

    /**
     * Get average P&L for a symbol
     */
    @Query("SELECT COALESCE(AVG(c.pnl), 0) FROM CryptoStrategy2 c WHERE c.symbol = :symbol AND c.status = 'CLOSED'")
    Double getAveragePnLBySymbol(@Param("symbol") String symbol);

    /**
     * Get average win amount for a symbol
     */
    @Query("SELECT COALESCE(AVG(c.pnl), 0) FROM CryptoStrategy2 c WHERE c.symbol = :symbol AND c.status = 'CLOSED' AND c.pnl > 0")
    Double getAverageWinBySymbol(@Param("symbol") String symbol);

    /**
     * Get average loss amount for a symbol
     */
    @Query("SELECT COALESCE(AVG(c.pnl), 0) FROM CryptoStrategy2 c WHERE c.symbol = :symbol AND c.status = 'CLOSED' AND c.pnl < 0")
    Double getAverageLossBySymbol(@Param("symbol") String symbol);

    /**
     * Find trades by exit reason
     */
    List<CryptoStrategy2> findByExitReason(String exitReason);

    /**
     * Find trades by market condition
     */
    List<CryptoStrategy2> findByMarketCondition(String marketCondition);

    /**
     * Find trades by trend direction
     */
    List<CryptoStrategy2> findByTrendDirection(String trendDirection);

    /**
     * Get recent trades (last N trades)
     */
    @Query("SELECT c FROM CryptoStrategy2 c ORDER BY c.entryTime DESC")
    List<CryptoStrategy2> findRecentTrades();

    /**
     * Get trades for today
     */
    @Query("SELECT c FROM CryptoStrategy2 c WHERE DATE(c.entryTime) = CURRENT_DATE")
    List<CryptoStrategy2> findTradesForToday();

    /**
     * Get trades for this week
     */
    @Query("SELECT c FROM CryptoStrategy2 c WHERE c.entryTime >= :weekStart")
    List<CryptoStrategy2> findTradesForThisWeek(@Param("weekStart") LocalDateTime weekStart);

    /**
     * Get trades for this month
     */
    @Query("SELECT c FROM CryptoStrategy2 c WHERE YEAR(c.entryTime) = YEAR(CURRENT_DATE) AND MONTH(c.entryTime) = MONTH(CURRENT_DATE)")
    List<CryptoStrategy2> findTradesForThisMonth();

    /**
     * Check if there's an open trade for a symbol
     */
    boolean existsBySymbolAndStatus(String symbol, String status);

    /**
     * Find open trade for a specific symbol and type
     */
    Optional<CryptoStrategy2> findBySymbolAndTradeTypeAndStatus(String symbol, String tradeType, String status);

    /**
     * Get strategy performance summary
     */
    @Query("SELECT " +
           "COUNT(c) as totalTrades, " +
           "SUM(CASE WHEN c.pnl > 0 THEN 1 ELSE 0 END) as winningTrades, " +
           "SUM(CASE WHEN c.pnl < 0 THEN 1 ELSE 0 END) as losingTrades, " +
           "COALESCE(SUM(c.pnl), 0) as totalPnL, " +
           "COALESCE(AVG(c.pnl), 0) as averagePnL " +
           "FROM CryptoStrategy2 c WHERE c.status = 'CLOSED'")
    Object[] getStrategyPerformanceSummary();

    /**
     * Get performance by symbol
     */
    @Query("SELECT " +
           "c.symbol, " +
           "COUNT(c) as totalTrades, " +
           "SUM(CASE WHEN c.pnl > 0 THEN 1 ELSE 0 END) as winningTrades, " +
           "COALESCE(SUM(c.pnl), 0) as totalPnL " +
           "FROM CryptoStrategy2 c WHERE c.status = 'CLOSED' " +
           "GROUP BY c.symbol")
    List<Object[]> getPerformanceBySymbol();
}
