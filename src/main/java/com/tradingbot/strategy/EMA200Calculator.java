package com.tradingbot.strategy;

import java.util.List;

/**
 * Professional EMA Calculator
 * Clean implementation for calculating Exponential Moving Averages
 */
public class EMA200Calculator {

    /**
     * Calculate EMA for given prices and period (e.g., 200 for EMA-200)
     * @param prices List of closing prices
     * @param period EMA period
     * @return array of EMA values
     */
    public static double[] calculateEMA(List<Double> prices, int period) {
        double[] ema = new double[prices.size()];
        double multiplier = 2.0 / (period + 1);

        if (prices.size() < period) {
            throw new IllegalArgumentException("Not enough data points for EMA-" + period);
        }

        // Step 1: Seed value = SMA of first 'period' closes
        double sma = 0.0;
        for (int i = 0; i < period; i++) {
            sma += prices.get(i);
        }
        sma /= period;
        ema[period - 1] = sma;

        // Step 2: EMA formula for the rest
        for (int i = period; i < prices.size(); i++) {
            ema[i] = (prices.get(i) - ema[i - 1]) * multiplier + ema[i - 1];
        }

        return ema;
    }
    
    /**
     * Get the last EMA value from the calculated array
     * @param prices List of closing prices
     * @param period EMA period
     * @return last EMA value
     */
    public static double getLastEMA(List<Double> prices, int period) {
        double[] ema = calculateEMA(prices, period);
        return ema[ema.length - 1];
    }
    
    /**
     * Get EMA 200 value specifically
     * @param prices List of closing prices
     * @return last EMA 200 value
     */
    public static double getEMA200(List<Double> prices) {
        return getLastEMA(prices, 200);
    }
}
