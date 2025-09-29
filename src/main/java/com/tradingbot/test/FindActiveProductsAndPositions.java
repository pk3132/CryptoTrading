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
 * Find active products and check for positions
 */
public class FindActiveProductsAndPositions {
    
    private static final Logger logger = LoggerFactory.getLogger(FindActiveProductsAndPositions.class);
    
    // LIVE API credentials
    private static final String API_KEY = "RRn7ddViVddcGGncWnvPZxJoI1OlRY";
    private static final String API_SECRET = "VnlMB2puay73RBJKMLPqarSTJU9jOuRe14AIqhCcViyvvRA2eGMpnfbakkg0";
    private static final String BASE_URL = "https://api.india.delta.exchange";
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public FindActiveProductsAndPositions() {
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
     * Get all products to find active BTC and ETH contracts
     */
    public void findActiveProducts() {
        logger.info("🔍 FINDING ACTIVE PRODUCTS");
        logger.info("==========================");
        
        try {
            String method = "GET";
            String path = "/v2/products";
            String queryString = "";
            String body = "";
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            
            String signatureData = method + timestamp + path + queryString + body;
            String signature = generateSignature(API_SECRET, signatureData);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("api-key", API_KEY)
                .header("timestamp", timestamp)
                .header("signature", signature)
                .header("User-Agent", "java-rest-client")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            logger.info("Products API Status: {}", response.statusCode());
            
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                
                if (jsonResponse.has("success") && jsonResponse.get("success").asBoolean()) {
                    JsonNode result = jsonResponse.get("result");
                    if (result != null && result.isArray()) {
                        logger.info("Found {} products", result.size());
                        logger.info("");
                        
                        // Look for BTC and ETH products
                        for (JsonNode product : result) {
                            if (product.has("symbol")) {
                                String symbol = product.get("symbol").asText();
                                if (symbol.contains("BTC") || symbol.contains("ETH")) {
                                    displayProduct(product);
                                }
                            }
                        }
                    }
                } else {
                    logger.error("❌ Failed to get products: {}", responseBody);
                }
            } else {
                logger.error("❌ Products API error: {} - {}", response.statusCode(), response.body());
            }
            
        } catch (Exception e) {
            logger.error("❌ Error getting products: {}", e.getMessage(), e);
        }
    }
    
    private void displayProduct(JsonNode product) {
        try {
            logger.info("📋 Product: {}", product.get("symbol").asText());
            logger.info("   ID: {}", product.get("id").asText());
            logger.info("   Status: {}", product.has("status") ? product.get("status").asText() : "unknown");
            logger.info("   Settlement Status: {}", product.has("settlement_status") ? product.get("settlement_status").asText() : "unknown");
            logger.info("   Type: {}", product.has("type") ? product.get("type").asText() : "unknown");
            logger.info("");
        } catch (Exception e) {
            logger.error("   ❌ Error displaying product: {}", e.getMessage());
        }
    }
    
    /**
     * Check positions using underlying asset symbol
     */
    public void checkPositionsByAsset() {
        logger.info("🔍 CHECKING POSITIONS BY ASSET SYMBOL");
        logger.info("====================================");
        
        String[] assets = {"BTC", "ETH"};
        
        for (String asset : assets) {
            logger.info("");
            logger.info("📊 Checking positions for underlying asset: {}", asset);
            
            try {
                String method = "GET";
                String path = "/v2/positions";
                String queryString = "?underlying_asset_symbol=" + asset;
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
                            logger.info("   ✅ Found {} positions", result.size());
                            
                            for (JsonNode position : result) {
                                displayPosition(position);
                            }
                        } else {
                            logger.info("   📄 No positions array in response");
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
    }
    
    private void displayPosition(JsonNode position) {
        try {
            logger.info("   🎯 POSITION FOUND:");
            logger.info("      Symbol: {}", position.has("symbol") ? position.get("symbol").asText() : "unknown");
            logger.info("      Side: {}", position.has("side") ? position.get("side").asText() : "unknown");
            logger.info("      Size: {}", position.has("size") ? position.get("size").asText() : "unknown");
            logger.info("      Entry Price: {}", position.has("entry_price") ? position.get("entry_price").asText() : "unknown");
            logger.info("      Mark Price: {}", position.has("mark_price") ? position.get("mark_price").asText() : "unknown");
            logger.info("      Unrealized PnL: {}", position.has("unrealized_pnl") ? position.get("unrealized_pnl").asText() : "unknown");
            logger.info("      Product ID: {}", position.has("product_id") ? position.get("product_id").asText() : "unknown");
            logger.info("");
        } catch (Exception e) {
            logger.error("      ❌ Error displaying position: {}", e.getMessage());
        }
    }
    
    /**
     * Check all orders to see if there are any open positions
     */
    public void checkAllOrders() {
        logger.info("🔍 CHECKING ALL ORDERS");
        logger.info("======================");
        
        try {
            String method = "GET";
            String path = "/v2/orders";
            String queryString = "";
            String body = "";
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            
            String signatureData = method + timestamp + path + queryString + body;
            String signature = generateSignature(API_SECRET, signatureData);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("api-key", API_KEY)
                .header("timestamp", timestamp)
                .header("signature", signature)
                .header("User-Agent", "java-rest-client")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            logger.info("Orders API Status: {}", response.statusCode());
            
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                
                if (jsonResponse.has("success") && jsonResponse.get("success").asBoolean()) {
                    JsonNode result = jsonResponse.get("result");
                    if (result != null && result.isArray()) {
                        logger.info("Found {} total orders", result.size());
                        logger.info("");
                        
                        // Look for any orders that might indicate open positions
                        for (JsonNode order : result) {
                            if (order.has("symbol") && (order.get("symbol").asText().contains("BTC") || order.get("symbol").asText().contains("ETH"))) {
                                displayOrder(order);
                            }
                        }
                    }
                } else {
                    logger.error("❌ Failed to get orders: {}", responseBody);
                }
            } else {
                logger.error("❌ Orders API error: {} - {}", response.statusCode(), response.body());
            }
            
        } catch (Exception e) {
            logger.error("❌ Error getting orders: {}", e.getMessage(), e);
        }
    }
    
    private void displayOrder(JsonNode order) {
        try {
            logger.info("📋 Order: {}", order.get("symbol").asText());
            logger.info("   ID: {}", order.get("id").asText());
            logger.info("   Side: {}", order.has("side") ? order.get("side").asText() : "unknown");
            logger.info("   Size: {}", order.has("size") ? order.get("size").asText() : "unknown");
            logger.info("   State: {}", order.has("state") ? order.get("state").asText() : "unknown");
            logger.info("   Order Type: {}", order.has("order_type") ? order.get("order_type").asText() : "unknown");
            logger.info("   Product ID: {}", order.has("product_id") ? order.get("product_id").asText() : "unknown");
            logger.info("");
        } catch (Exception e) {
            logger.error("   ❌ Error displaying order: {}", e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        FindActiveProductsAndPositions finder = new FindActiveProductsAndPositions();
        
        // Step 1: Find active products
        finder.findActiveProducts();
        
        // Step 2: Check positions by asset symbol
        finder.checkPositionsByAsset();
        
        // Step 3: Check all orders
        finder.checkAllOrders();
    }
}
