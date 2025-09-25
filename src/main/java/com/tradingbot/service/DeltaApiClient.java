package com.tradingbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.List;
import java.util.HashMap;

/**
 * Delta Exchange API Client
 * Handles authenticated API calls to Delta Exchange
 */
@Service
public class DeltaApiClient {

    private static final String BASE_URL = "https://api.india.delta.exchange/v2";
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    
    @Value("${delta.api.key:your_api_key_here}")
    private String apiKey;
    
    @Value("${delta.api.secret:your_api_secret_here}")
    private String apiSecret;

    public DeltaApiClient() {
        this.restTemplate = new RestTemplate();
        this.mapper = new ObjectMapper();
    }

    /**
     * Generate HMAC signature for Delta Exchange API
     */
    private String generateSignature(String method, String timestamp, String requestPath, String queryString, String body) {
        String prehash = method + timestamp + requestPath + queryString + body;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(prehash.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Signature generation failed", e);
        }
    }

    /**
     * Check if API credentials are configured
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.equals("your_api_key_here") &&
               apiSecret != null && !apiSecret.isEmpty() && !apiSecret.equals("your_api_secret_here");
    }

    /**
     * Fetch OHLCV candles from Delta Exchange
     */
    public List<Map<String, Object>> fetchOhlcv(String symbol, String resolution, long start, long end) {
        try {
            String url = BASE_URL + "/history/candles?resolution=" + resolution + 
                        "&symbol=" + symbol + "&price_type=mark&start=" + start + "&end=" + end;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody.containsKey("result")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> result = (List<Map<String, Object>>) responseBody.get("result");
                    return result;
                }
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("❌ Error fetching OHLCV data: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get current mark price for a symbol from tickers endpoint
     */
    public Double getCurrentMarkPrice(String symbol) {
        try {
            String url = BASE_URL + "/tickers?symbol=" + symbol;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object resultObj = response.getBody().get("result");
                if (resultObj instanceof java.util.List) {
                    java.util.List list = (java.util.List) resultObj;
                    
                    // Find the correct symbol in the list instead of just taking the first one
                    for (Object item : list) {
                        if (item instanceof java.util.Map) {
                            java.util.Map itemMap = (java.util.Map) item;
                            String itemSymbol = (String) itemMap.get("symbol");
                            if (symbol.equals(itemSymbol)) {
                                Object markObj = itemMap.get("mark_price");
                                if (markObj != null) {
                                    return Double.parseDouble(markObj.toString());
                                }
                            }
                        }
                    }
                    
                    // Fallback: if symbol not found, use first item (old behavior)
                    if (!list.isEmpty() && list.get(0) instanceof java.util.Map) {
                        java.util.Map first = (java.util.Map) list.get(0);
                        Object markObj = first.get("mark_price");
                        if (markObj != null) {
                            System.err.println("⚠️ Symbol " + symbol + " not found, using first available symbol: " + first.get("symbol"));
                            return Double.parseDouble(markObj.toString());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error fetching current mark price for " + symbol + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Place market order (requires API credentials)
     */
    public Map<String, Object> placeMarketOrder(String symbol, String side, int size) {
        if (!isConfigured()) {
            System.err.println("❌ API credentials not configured. Cannot place orders.");
            return null;
        }
        
        try {
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            String method = "POST";
            String requestPath = "/orders";
            String queryString = "";
            
            Map<String, Object> orderData = new HashMap<>();
            orderData.put("product_symbol", symbol);
            orderData.put("size", size);
            orderData.put("side", side);
            orderData.put("order_type", "market");
            
            String body = mapper.writeValueAsString(orderData);
            String signature = generateSignature(method, timestamp, requestPath, queryString, body);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json");
            headers.set("api-key", apiKey);
            headers.set("timestamp", timestamp);
            headers.set("signature", signature);

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                BASE_URL + requestPath, 
                HttpMethod.POST, 
                entity, 
                Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                System.err.println("❌ Order failed: " + response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            System.err.println("❌ Error placing market order: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get account balance (requires API credentials)
     */
    public Map<String, Object> getAccountBalance() {
        if (!isConfigured()) {
            System.err.println("❌ API credentials not configured. Cannot fetch balance.");
            return null;
        }
        
        try {
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            String method = "GET";
            String requestPath = "/account/balances";
            String queryString = "";
            String body = "";
            
            String signature = generateSignature(method, timestamp, requestPath, queryString, body);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            headers.set("api-key", apiKey);
            headers.set("timestamp", timestamp);
            headers.set("signature", signature);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                BASE_URL + requestPath, 
                HttpMethod.GET, 
                entity, 
                Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                System.err.println("❌ Failed to fetch balance: " + response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            System.err.println("❌ Error fetching account balance: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get current positions (requires API credentials)
     */
    public List<Map<String, Object>> getPositions() {
        if (!isConfigured()) {
            System.err.println("❌ API credentials not configured. Cannot fetch positions.");
            return null;
        }
        
        try {
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            String method = "GET";
            String requestPath = "/positions";
            String queryString = "";
            String body = "";
            
            String signature = generateSignature(method, timestamp, requestPath, queryString, body);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            headers.set("api-key", apiKey);
            headers.set("timestamp", timestamp);
            headers.set("signature", signature);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                BASE_URL + requestPath, 
                HttpMethod.GET, 
                entity, 
                Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody.containsKey("result")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> result = (List<Map<String, Object>>) responseBody.get("result");
                    return result;
                }
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("❌ Error fetching positions: " + e.getMessage());
            return null;
        }
    }
}
