package com.tradingbot.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import java.util.*;

/**
 * Service to fetch historical candlestick data from Delta Exchange
 */
@Service
public class DeltaCandlestickService {

    private static final String BASE_URL = "https://api.india.delta.exchange/v2/history/candles";
    private final RestTemplate restTemplate;

    public DeltaCandlestickService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Get candlestick data for a symbol with specified parameters
     */
    public Map<String, Object> getCandlestickData(String symbol, int limit, String resolution) {
        try {
            // Calculate timestamps
            long endTime = System.currentTimeMillis() / 1000; // Current time in seconds
            long intervalSeconds = getIntervalSeconds(resolution);
            long startTime = endTime - (limit * intervalSeconds);
            
            String url = BASE_URL + "?symbol=" + symbol + "&resolution=" + resolution + 
                        "&start=" + startTime + "&end=" + endTime;
            
            System.out.println("Requesting candlestick data from " + startTime + " to " + endTime);
            System.out.println("URL: " + url);
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                System.out.println("Error fetching candlestick data: " + response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get interval in seconds for different resolutions
     */
    private long getIntervalSeconds(String resolution) {
        switch (resolution) {
            case "1m": return 60;
            case "5m": return 300;
            case "15m": return 900;
            case "1h": return 3600;
            case "4h": return 14400;
            case "1d": return 86400;
            default: return 60; // Default to 1 minute
        }
    }

    /**
     * Get last 15 candles for BTCUSD with 1-minute resolution
     */
    public List<Map<String, Object>> getLast15Candles(String symbol) {
        Map<String, Object> response = getCandlestickData(symbol, 15, "1m");
        
        if (response != null && response.containsKey("result")) {
            return (List<Map<String, Object>>) response.get("result");
        }
        return null;
    }

    /**
     * Analyze last 15 candles and find overall high and low
     */
    public void analyzeLast15Candles(String symbol) {
        List<Map<String, Object>> candles = getLast15Candles(symbol);
        
        if (candles == null || candles.isEmpty()) {
            System.out.println("No candlestick data available for " + symbol);
            return;
        }

        System.out.println("=== Last 15 Candles Analysis for " + symbol + " ===");
        System.out.println("Time Resolution: 1 minute");
        System.out.println("Total Candles: " + candles.size());
        System.out.println();

        double overallHigh = Double.MIN_VALUE;
        double overallLow = Double.MAX_VALUE;
        String highTime = "";
        String lowTime = "";

        System.out.println("Individual Candle Data:");
        System.out.println("Time\t\t\tOpen\t\tHigh\t\tLow\t\tClose\t\tVolume");
        System.out.println("--------------------------------------------------------------------------------");

        for (Map<String, Object> candle : candles) {
            // Handle time field - could be String or Integer (timestamp)
            Object timeObj = candle.get("time");
            String time;
            if (timeObj instanceof Integer) {
                // Convert timestamp to readable format
                long timestamp = ((Integer) timeObj).longValue();
                time = new java.util.Date(timestamp * 1000).toString();
            } else {
                time = timeObj.toString();
            }
            
            double open = Double.parseDouble(candle.get("open").toString());
            double high = Double.parseDouble(candle.get("high").toString());
            double low = Double.parseDouble(candle.get("low").toString());
            double close = Double.parseDouble(candle.get("close").toString());
            double volume = Double.parseDouble(candle.get("volume").toString());

            // Track overall high and low
            if (high > overallHigh) {
                overallHigh = high;
                highTime = time;
            }
            if (low < overallLow) {
                overallLow = low;
                lowTime = time;
            }

            System.out.printf("%s\t%.2f\t\t%.2f\t\t%.2f\t\t%.2f\t\t%.2f%n", 
                time, open, high, low, close, volume);
        }

        System.out.println();
        System.out.println("=== Summary ===");
        System.out.println("Overall High: $" + String.format("%.2f", overallHigh) + " (Time: " + highTime + ")");
        System.out.println("Overall Low: $" + String.format("%.2f", overallLow) + " (Time: " + lowTime + ")");
        System.out.println("Price Range: $" + String.format("%.2f", overallHigh - overallLow));
        System.out.println("Range Percentage: " + String.format("%.2f", ((overallHigh - overallLow) / overallLow) * 100) + "%");
        
        // Get current price for comparison
        Double currentPrice = getCurrentPrice(symbol);
        if (currentPrice != null) {
            System.out.println("Current Price: $" + String.format("%.2f", currentPrice));
            System.out.println("Distance from High: " + String.format("%.2f", ((overallHigh - currentPrice) / currentPrice) * 100) + "%");
            System.out.println("Distance from Low: " + String.format("%.2f", ((currentPrice - overallLow) / currentPrice) * 100) + "%");
        }
        System.out.println("================================================");
    }

    /**
     * Get candlestick data with different resolutions
     */
    public void getCandlestickDataWithResolutions(String symbol) {
        String[] resolutions = {"1m", "5m", "15m", "1h", "1d"};
        
        System.out.println("=== Candlestick Data for " + symbol + " with Different Resolutions ===");
        
        for (String resolution : resolutions) {
            System.out.println("\n--- " + resolution + " Resolution ---");
            Map<String, Object> data = getCandlestickData(symbol, 5, resolution);
            
            if (data != null && data.containsKey("result")) {
                List<Map<String, Object>> candles = (List<Map<String, Object>>) data.get("result");
                if (!candles.isEmpty()) {
                    Map<String, Object> latest = candles.get(0);
                    System.out.println("Latest Candle:");
                    
                    // Handle time field
                    Object timeObj = latest.get("time");
                    if (timeObj instanceof Integer) {
                        long timestamp = ((Integer) timeObj).longValue();
                        System.out.println("Time: " + new java.util.Date(timestamp * 1000));
                    } else {
                        System.out.println("Time: " + timeObj);
                    }
                    
                    System.out.println("Open: $" + latest.get("open"));
                    System.out.println("High: $" + latest.get("high"));
                    System.out.println("Low: $" + latest.get("low"));
                    System.out.println("Close: $" + latest.get("close"));
                    System.out.println("Volume: " + latest.get("volume"));
                }
            }
        }
    }

    /**
     * Get current price for a symbol
     */
    private Double getCurrentPrice(String symbol) {
        try {
            // Get the latest candlestick data
            Map<String, Object> data = getCandlestickData(symbol, 1, "1m");
            if (data != null && data.containsKey("result")) {
                List<Map<String, Object>> candles = (List<Map<String, Object>>) data.get("result");
                if (candles != null && !candles.isEmpty()) {
                    Map<String, Object> latest = candles.get(candles.size() - 1);
                    return Double.parseDouble(latest.get("close").toString());
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting current price for " + symbol + ": " + e.getMessage());
        }
        return null;
    }

}
