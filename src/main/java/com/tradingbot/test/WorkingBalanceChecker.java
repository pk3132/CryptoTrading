package com.tradingbot.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

/**
 * Working Balance Checker using correct signature method from DeltaOrderService
 */
public class WorkingBalanceChecker {
    
    private static final Logger logger = LoggerFactory.getLogger(WorkingBalanceChecker.class);
    
    // LIVE API credentials
    private static final String API_KEY = "RRn7ddViVddcGGncWnvPZxJoI1OlRY";
    private static final String API_SECRET = "VnlMB2puay73RBJKMLPqarSTJU9jOuRe14AIqhCcViyvvRA2eGMpnfbakkg0";
    private static final String BASE_URL = "https://api.india.delta.exchange";
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public WorkingBalanceChecker() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Generate signature using the same method as DeltaOrderService (hex string)
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
     * Try different balance endpoints to find the correct one
     */
    public void checkBalance() {
        String[] endpoints = {
            "/v2/wallet/balances",
            "/v2/portfolio/balances", 
            "/v2/balances",
            "/v2/account/balances",
            "/v2/user/balances"
        };
        
        for (String endpoint : endpoints) {
            logger.info("🔍 Trying endpoint: {}", endpoint);
            if (tryBalanceEndpoint(endpoint)) {
                return; // Success, stop trying other endpoints
            }
            logger.info("");
        }
        
        // If all endpoints fail, try positions endpoint to verify API works
        logger.info("🔄 All balance endpoints failed. Testing positions endpoint...");
        tryPositionsEndpoint();
    }
    
    private boolean tryBalanceEndpoint(String endpoint) {
        try {
            String method = "GET";
            String path = endpoint;
            String queryString = "";
            String body = "";
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            
            // Generate signature (same format as DeltaOrderService)
            String signatureData = method + timestamp + path + queryString + body;
            String signature = generateSignature(API_SECRET, signatureData);
            
            logger.info("🔐 Signature Data: '{}'", signatureData);
            logger.info("🔑 Generated Signature: {}", signature);
            logger.info("⏰ Timestamp: {}", timestamp);
            
            // Build request
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
            
            // Send request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            logger.info("📊 API Response Status: {}", response.statusCode());
            
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                
                logger.info("✅ SUCCESS! Found working balance endpoint: {}", endpoint);
                logger.info("📄 Response: {}", responseBody);
                
                // Try to parse and display balance information
                displayBalanceInfo(jsonResponse);
                return true;
                
            } else {
                logger.warn("❌ Endpoint {} failed with status: {}", endpoint, response.statusCode());
                logger.debug("📄 Response: {}", response.body());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("❌ Error testing endpoint {}: {}", endpoint, e.getMessage());
            return false;
        }
    }
    
    private void tryPositionsEndpoint() {
        try {
            String method = "GET";
            String path = "/v2/positions";
            String queryString = "";
            String body = "";
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            
            // Generate signature
            String signatureData = method + timestamp + path + queryString + body;
            String signature = generateSignature(API_SECRET, signatureData);
            
            // Build request
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
            
            // Send request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            logger.info("📊 Positions API Response Status: {}", response.statusCode());
            
            if (response.statusCode() == 200) {
                logger.info("✅ Positions endpoint working! API authentication is correct.");
                logger.info("📄 Positions Response: {}", response.body());
            } else {
                logger.error("❌ Positions endpoint failed with status: {}", response.statusCode());
                logger.error("📄 Response: {}", response.body());
            }
            
        } catch (Exception e) {
            logger.error("❌ Error testing positions endpoint: {}", e.getMessage());
        }
    }
    
    private void displayBalanceInfo(JsonNode jsonResponse) {
        try {
            if (jsonResponse.has("success") && jsonResponse.get("success").asBoolean()) {
                logger.info("🎉 Successfully retrieved balance information!");
                
                // Display meta information if available
                if (jsonResponse.has("meta")) {
                    JsonNode meta = jsonResponse.get("meta");
                    logger.info("📊 Account Summary:");
                    if (meta.has("net_equity")) {
                        logger.info("💰 Net Equity: ${}", meta.get("net_equity").asText());
                    }
                    if (meta.has("total_balance")) {
                        logger.info("💎 Total Balance: ${}", meta.get("total_balance").asText());
                    }
                    if (meta.has("available_balance")) {
                        logger.info("✅ Available Balance: ${}", meta.get("available_balance").asText());
                    }
                }
                
                // Display wallet balances if available
                if (jsonResponse.has("result") && jsonResponse.get("result").isArray()) {
                    JsonNode wallets = jsonResponse.get("result");
                    
                    logger.info("💳 Wallet Balances:");
                    logger.info("==================");
                    
                    for (JsonNode wallet : wallets) {
                        if (wallet.has("asset_symbol")) {
                            logger.info("🏦 Asset: {}", wallet.get("asset_symbol").asText());
                        }
                        if (wallet.has("balance")) {
                            logger.info("  💰 Total Balance: ${}", wallet.get("balance").asText());
                        }
                        if (wallet.has("available_balance")) {
                            logger.info("  ✅ Available Balance: ${}", wallet.get("available_balance").asText());
                        }
                        logger.info("");
                    }
                }
                
                logger.info("🎯 LIVE Account Status: ✅ ACTIVE");
                logger.info("🚀 Ready for LIVE TRADING!");
                
            } else {
                logger.warn("⚠️ API call returned success: false");
                logger.info("📄 Full Response: {}", jsonResponse.toString());
            }
            
        } catch (Exception e) {
            logger.error("❌ Error parsing balance information: {}", e.getMessage());
            logger.info("📄 Raw Response: {}", jsonResponse.toString());
        }
    }
    
    public static void main(String[] args) {
        WorkingBalanceChecker balanceChecker = new WorkingBalanceChecker();
        
        logger.info("💰 Delta Exchange LIVE Account Balance Checker");
        logger.info("==============================================");
        logger.info("🔑 Using LIVE API credentials");
        logger.info("🌐 Base URL: {}", BASE_URL);
        logger.info("");
        
        // Try to get balance
        balanceChecker.checkBalance();
        
        logger.info("");
        logger.info("✅ Balance Check Complete!");
    }
}
