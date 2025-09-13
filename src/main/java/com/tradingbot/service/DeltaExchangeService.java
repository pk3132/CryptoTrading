package com.tradingbot.service;

import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import java.util.*;

/**
 * Delta Exchange Ticker API Service
 * Fetches market data for trading pairs like BTCUSD
 */
public class DeltaExchangeService {

    private static final String BASE_URL = "https://api.india.delta.exchange/v2/tickers";
    private final RestTemplate restTemplate;

    public DeltaExchangeService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Get market data for a specific trading pair
     */
    public Map<String, Object> getTickerData(String symbol) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(BASE_URL, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> body = response.getBody();

                if (body != null && body.containsKey("result")) {
                    List<Map<String, Object>> tickers = (List<Map<String, Object>>) body.get("result");

                    for (Map<String, Object> ticker : tickers) {
                        if (symbol.equals(ticker.get("symbol"))) {
                            return ticker;
                        }
                    }
                }
            } else {
                System.out.println("Error: " + response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            System.out.println("Error fetching ticker data: " + e.getMessage());
            return null;
        }
        return null;
    }

    /**
     * Print formatted ticker data for a symbol
     */
    public void printTickerData(String symbol) {
        Map<String, Object> tickerData = getTickerData(symbol);
        
        if (tickerData != null) {
            System.out.println("=== " + symbol + " Market Data ===");
            System.out.println("Symbol: " + tickerData.get("symbol"));
            System.out.println("Current Price (Close): $" + tickerData.get("close"));
            System.out.println("Mark Price: $" + tickerData.get("mark_price"));
            System.out.println("Spot Price: $" + tickerData.get("spot_price"));
            System.out.println("High (24h): $" + tickerData.get("high"));
            System.out.println("Low (24h): $" + tickerData.get("low"));
            System.out.println("Open (24h): $" + tickerData.get("open"));
            System.out.println("Volume (24h): " + tickerData.get("volume"));
            System.out.println("Change (24h): " + tickerData.get("mark_change_24h") + "%");
            
            // Extract bid/ask from quotes
            Map<String, Object> quotes = (Map<String, Object>) tickerData.get("quotes");
            if (quotes != null) {
                System.out.println("Best Bid: $" + quotes.get("best_bid"));
                System.out.println("Best Ask: $" + quotes.get("best_ask"));
                System.out.println("Bid Size: " + quotes.get("bid_size"));
                System.out.println("Ask Size: " + quotes.get("ask_size"));
            }
            
            System.out.println("Open Interest: " + tickerData.get("oi") + " " + tickerData.get("oi_value_symbol"));
            System.out.println("Funding Rate: " + tickerData.get("funding_rate"));
            System.out.println("================================");
        } else {
            System.out.println("No data found for symbol: " + symbol);
        }
    }

    /**
     * Get current price for a symbol
     */
    public Double getCurrentPrice(String symbol) {
        Map<String, Object> tickerData = getTickerData(symbol);
        if (tickerData != null && tickerData.get("close") != null) {
            return Double.parseDouble(tickerData.get("close").toString());
        }
        return null;
    }

    /**
     * Get high price for a symbol (24h)
     */
    public Double getHighPrice(String symbol) {
        Map<String, Object> tickerData = getTickerData(symbol);
        if (tickerData != null && tickerData.get("high") != null) {
            return Double.parseDouble(tickerData.get("high").toString());
        }
        return null;
    }

    /**
     * Get low price for a symbol (24h)
     */
    public Double getLowPrice(String symbol) {
        Map<String, Object> tickerData = getTickerData(symbol);
        if (tickerData != null && tickerData.get("low") != null) {
            return Double.parseDouble(tickerData.get("low").toString());
        }
        return null;
    }

    /**
     * Main method for testing
     */
    public static void main(String[] args) {
        DeltaExchangeService service = new DeltaExchangeService();
        
        // Test BTCUSD ticker data (correct symbol for Delta Exchange India)
        service.printTickerData("BTCUSD");
        
        // Test individual price methods
        System.out.println("\n=== Individual Price Methods ===");
        Double currentPrice = service.getCurrentPrice("BTCUSD");
        Double highPrice = service.getHighPrice("BTCUSD");
        Double lowPrice = service.getLowPrice("BTCUSD");
        
        if (currentPrice != null) {
            System.out.println("Current BTCUSD Price: $" + currentPrice);
        }
        if (highPrice != null) {
            System.out.println("24h High: $" + highPrice);
        }
        if (lowPrice != null) {
            System.out.println("24h Low: $" + lowPrice);
        }
        
        // Test other popular pairs
        System.out.println("\n=== Other Popular Pairs ===");
        service.printTickerData("ETHUSD");
        service.printTickerData("XRPUSD");
    }
}
