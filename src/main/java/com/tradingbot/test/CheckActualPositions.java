package com.tradingbot.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

/**
 * Check actual positions using different API endpoints
 */
public class CheckActualPositions {
    
    private static final Logger logger = LoggerFactory.getLogger(CheckActualPositions.class);
    
    // LIVE API credentials
    private static final String API_KEY = "RRn7ddViVddcGGncWnvPZxJoI1OlRY";
    private static final String API_SECRET = "VnlMB2puay73RBJKMLPqarSTJU9jOuRe14AIqhCcViyvvRA2eGMpnfbakkg0";
    private static final String BASE_URL = "https://api.india.delta.exchange";
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public CheckActualPositions() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Generate HMAC SHA256 signature
     */
    private String generateSignature(String secret, String message) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * Try different endpoints to find open positions
     */
    public void checkAllPositionEndpoints() {
        logger.info("🔍 CHECKING ALL POSITION ENDPOINTS");
        logger.info("===================================");
        
        // Try different endpoints
        String[] endpoints = {
            "/v2/positions",
            "/v2/positions?product_id=84",
            "/v2/positions?product_id=1699",
            "/v2/positions?state=open",
            "/v2/orders?state=open",
            "/v2/orders?state=open&product_id=84",
            "/v2/orders?state=open&product_id=1699"
        };
        
        for (String endpoint : endpoints) {
            logger.info("");
            logger.info("📊 Testing endpoint: {}", endpoint);
            checkEndpoint(endpoint);
        }
        
        // Also try to get all orders
        logger.info("");
        logger.info("📊 Testing all orders endpoint:");
        checkAllOrders();
    }
    
    private void checkEndpoint(String endpoint) {
        try {
            String method = "GET";
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            String body = "";
            
            // Extract path and query string
            String path;
            String queryString;
            if (endpoint.contains("?")) {
                String[] parts = endpoint.split("\\?", 2);
                path = parts[0];
                queryString = "?" + parts[1];
            } else {
                path = endpoint;
                queryString = "";
            }
            
            String signatureData = method + timestamp + path + queryString + body;
            String signature = generateSignature(API_SECRET, signatureData);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .header("api-key", API_KEY)
                .header("timestamp", timestamp)
                .header("signature", signature)
                .header("User-Agent", "java-rest-client")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            logger.info("   Status: {}", response.statusCode());
            
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                
                if (jsonResponse.has("success") && jsonResponse.get("success").asBoolean()) {
                    JsonNode result = jsonResponse.get("result");
                    if (result != null && result.isArray()) {
                        logger.info("   ✅ Found {} items", result.size());
                        
                        for (JsonNode item : result) {
                            displayItem(item);
                        }
                    } else {
                        logger.info("   📄 Response: {}", responseBody);
                    }
                } else {
                    logger.info("   ❌ Success: false");
                    logger.info("   📄 Response: {}", responseBody);
                }
            } else {
                logger.info("   ❌ Error: {}", response.statusCode());
                logger.info("   📄 Response: {}", response.body());
            }
            
        } catch (Exception e) {
            logger.error("   ❌ Exception: {}", e.getMessage());
        }
    }
    
    private void checkAllOrders() {
        try {
            String method = "GET";
            String path = "/v2/orders";
            String queryString = "?state=open";
            String body = "";
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            
            String signatureData = method + timestamp + path + queryString + body;
            String signature = generateSignature(API_SECRET, signatureData);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path + queryString))
                .header("api-key", API_KEY)
                .header("timestamp", timestamp)
                .header("signature", signature)
                .header("User-Agent", "java-rest-client")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            logger.info("   Status: {}", response.statusCode());
            
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                
                if (jsonResponse.has("success") && jsonResponse.get("success").asBoolean()) {
                    JsonNode result = jsonResponse.get("result");
                    if (result != null && result.isArray()) {
                        logger.info("   ✅ Found {} open orders", result.size());
                        
                        for (JsonNode order : result) {
                            displayOrder(order);
                        }
                    }
                } else {
                    logger.info("   📄 Response: {}", responseBody);
                }
            } else {
                logger.info("   ❌ Error: {}", response.statusCode());
                logger.info("   📄 Response: {}", response.body());
            }
            
        } catch (Exception e) {
            logger.error("   ❌ Exception: {}", e.getMessage());
        }
    }
    
    private void displayItem(JsonNode item) {
        try {
            logger.info("   📋 Item Details:");
            
            if (item.has("symbol")) {
                logger.info("      Symbol: {}", item.get("symbol").asText());
            }
            if (item.has("side")) {
                logger.info("      Side: {}", item.get("side").asText());
            }
            if (item.has("size")) {
                logger.info("      Size: {}", item.get("size").asText());
            }
            if (item.has("entry_price")) {
                logger.info("      Entry Price: {}", item.get("entry_price").asText());
            }
            if (item.has("mark_price")) {
                logger.info("      Mark Price: {}", item.get("mark_price").asText());
            }
            if (item.has("unrealized_pnl")) {
                logger.info("      Unrealized PnL: {}", item.get("unrealized_pnl").asText());
            }
            if (item.has("product_id")) {
                logger.info("      Product ID: {}", item.get("product_id").asText());
            }
            if (item.has("state")) {
                logger.info("      State: {}", item.get("state").asText());
            }
            
        } catch (Exception e) {
            logger.error("      ❌ Error displaying item: {}", e.getMessage());
        }
    }
    
    private void displayOrder(JsonNode order) {
        try {
            logger.info("   📋 Order Details:");
            
            if (order.has("symbol")) {
                logger.info("      Symbol: {}", order.get("symbol").asText());
            }
            if (order.has("side")) {
                logger.info("      Side: {}", order.get("side").asText());
            }
            if (order.has("size")) {
                logger.info("      Size: {}", order.get("size").asText());
            }
            if (order.has("order_type")) {
                logger.info("      Order Type: {}", order.get("order_type").asText());
            }
            if (order.has("state")) {
                logger.info("      State: {}", order.get("state").asText());
            }
            if (order.has("product_id")) {
                logger.info("      Product ID: {}", order.get("product_id").asText());
            }
            
        } catch (Exception e) {
            logger.error("      ❌ Error displaying order: {}", e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        CheckActualPositions checker = new CheckActualPositions();
        checker.checkAllPositionEndpoints();
    }
}
