package com.tradingbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
import java.util.ArrayList;
import java.util.List;

/**
 * Position Checker Service
 * Checks existing open positions before placing new orders to avoid duplicates
 */
@Service
public class PositionChecker {
    
    private static final Logger logger = LoggerFactory.getLogger(PositionChecker.class);
    
    // LIVE API credentials
    private static final String API_KEY = "RRn7ddViVddcGGncWnvPZxJoI1OlRY";
    private static final String API_SECRET = "VnlMB2puay73RBJKMLPqarSTJU9jOuRe14AIqhCcViyvvRA2eGMpnfbakkg0";
    private static final String BASE_URL = "https://api.india.delta.exchange";
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public PositionChecker() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Generate HMAC SHA256 signature (FIXED VERSION)
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
     * Get all open positions from Delta Exchange
     */
    public List<PositionInfo> getOpenPositions() {
        List<PositionInfo> positions = new ArrayList<>();
        
        // Check positions for each underlying asset
        String[] assets = {"BTC", "ETH"};
        
        for (String asset : assets) {
            try {
                String method = "GET";
                String path = "/v2/positions";
                String queryString = "?underlying_asset_symbol=" + asset;
                String body = "";
                String timestamp = String.valueOf(Instant.now().getEpochSecond());
                
                // CORRECT signature construction: method + timestamp + path + queryString + body
                String signatureData = method + timestamp + path + queryString + body;
                String signature = generateSignature(API_SECRET, signatureData);
                
                logger.debug("🔐 Position Check Signature Data: '{}'", signatureData);
                logger.debug("🔑 Generated Signature: {}", signature);
                logger.debug("⏰ Timestamp: {}", timestamp);
                
                // Build request with CORRECT headers and query parameters
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + path + queryString))
                    .header("api-key", API_KEY)
                    .header("timestamp", timestamp)
                    .header("signature", signature)
                    .header("User-Agent", "java-rest-client") // REQUIRED to avoid 4XX errors
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();
                
                // Send request
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                logger.debug("📊 Position Check API Response Status for {}: {}", asset, response.statusCode());
                
                if (response.statusCode() == 200) {
                    String responseBody = response.body();
                    JsonNode jsonResponse = objectMapper.readTree(responseBody);
                    
                    if (jsonResponse.has("success") && jsonResponse.get("success").asBoolean()) {
                        JsonNode result = jsonResponse.get("result");
                        if (result != null && result.isArray()) {
                            for (JsonNode position : result) {
                                PositionInfo pos = parsePosition(position);
                                if (pos != null && pos.isOpen()) {
                                    positions.add(pos);
                                }
                            }
                        }
                    } else {
                        logger.debug("❌ Position check failed for {} - Success: false", asset);
                        logger.debug("📄 Response: {}", responseBody);
                    }
                } else {
                    logger.debug("❌ Position check failed for {} with status: {}", asset, response.statusCode());
                    logger.debug("📄 Response: {}", response.body());
                }
                
            } catch (Exception e) {
                logger.debug("❌ Error checking positions for {}: {}", asset, e.getMessage());
            }
        }
        
        return positions;
    }
    
    /**
     * Parse position from API response
     */
    private PositionInfo parsePosition(JsonNode position) {
        try {
            PositionInfo pos = new PositionInfo();
            
            if (position.has("symbol")) {
                pos.symbol = position.get("symbol").asText();
            }
            if (position.has("side")) {
                pos.side = position.get("side").asText();
            }
            if (position.has("size")) {
                pos.size = position.get("size").asDouble();
            }
            if (position.has("entry_price")) {
                pos.entryPrice = position.get("entry_price").asDouble();
            }
            if (position.has("mark_price")) {
                pos.markPrice = position.get("mark_price").asDouble();
            }
            if (position.has("unrealized_pnl")) {
                pos.unrealizedPnl = position.get("unrealized_pnl").asDouble();
            }
            if (position.has("realized_pnl")) {
                pos.realizedPnl = position.get("realized_pnl").asDouble();
            }
            if (position.has("product_id")) {
                pos.productId = position.get("product_id").asInt();
            }
            
            return pos;
        } catch (Exception e) {
            logger.error("❌ Error parsing position: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if we already have an open position for a symbol
     */
    public boolean hasOpenPosition(String symbol) {
        try {
            // Map symbol to underlying asset
            String underlyingAsset;
            if (symbol.equals("BTCUSD")) {
                underlyingAsset = "BTC";
            } else if (symbol.equals("ETHUSD")) {
                underlyingAsset = "ETH";
            } else {
                logger.error("❌ Unknown symbol: {}", symbol);
                return false;
            }
            
            String method = "GET";
            String path = "/v2/positions";
            String queryString = "?underlying_asset_symbol=" + underlyingAsset;
            String body = "";
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            
            // CORRECT signature construction
            String signatureData = method + timestamp + path + queryString + body;
            String signature = generateSignature(API_SECRET, signatureData);
            
            // Build request
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
            
            logger.debug("🔍 Position check API response for {}: Status={}", symbol, response.statusCode());
            
            if (response.statusCode() == 200) {
                JsonNode jsonResponse = objectMapper.readTree(response.body());
                if (jsonResponse.has("success") && jsonResponse.get("success").asBoolean()) {
                    JsonNode result = jsonResponse.get("result");
                    if (result != null && result.isArray() && result.size() > 0) {
                        // Found positions for this underlying asset
                        JsonNode position = result.get(0);
                        double size = position.has("size") ? position.get("size").asDouble() : 0;
                        if (size > 0) {
                            String side = position.has("side") ? position.get("side").asText() : "unknown";
                            double entryPrice = position.has("entry_price") ? position.get("entry_price").asDouble() : 0;
                            int productId = position.has("product_id") ? position.get("product_id").asInt() : 0;
                            logger.warn("⚠️ Open position found for {}: {} {} contracts at ${} (Product ID: {})", 
                                       symbol, side, size, entryPrice, productId);
                            return true;
                        }
                    }
                }
            } else {
                logger.error("❌ Position check failed for {}: {}", symbol, response.statusCode());
                logger.error("📄 Response: {}", response.body());
            }
            
            logger.info("✅ No open position found for {}", symbol);
            return false;
            
        } catch (Exception e) {
            logger.error("❌ Error checking position for {}: {}", symbol, e.getMessage());
            logger.error("⚠️ CRITICAL: Cannot verify existing positions - blocking new orders to prevent duplicates");
            // Return true to be safe and block new orders when we can't verify positions
            return true; // Block new orders if we can't check existing positions
        }
    }
    
    /**
     * Get position details for a specific symbol
     */
    public PositionInfo getPositionForSymbol(String symbol) {
        List<PositionInfo> positions = getOpenPositions();
        
        for (PositionInfo position : positions) {
            if (symbol.equals(position.symbol)) {
                return position;
            }
        }
        
        return null;
    }
    
    /**
     * Print all open positions
     */
    public void printOpenPositions() {
        List<PositionInfo> positions = getOpenPositions();
        
        logger.info("📊 OPEN POSITIONS SUMMARY");
        logger.info("========================");
        
        if (positions.isEmpty()) {
            logger.info("✅ No open positions found");
        } else {
            logger.info("Found {} open position(s):", positions.size());
            logger.info("");
            
            for (PositionInfo position : positions) {
                logger.info("🎯 Position: {}", position.symbol);
                logger.info("   Side: {}", position.side);
                logger.info("   Size: {} contracts", position.size);
                logger.info("   Entry Price: ${}", String.format("%.2f", position.entryPrice));
                logger.info("   Mark Price: ${}", String.format("%.2f", position.markPrice));
                logger.info("   Unrealized PnL: ${}", String.format("%.2f", position.unrealizedPnl));
                logger.info("   Product ID: {}", position.productId);
                logger.info("");
            }
        }
    }
    
    /**
     * Position Information Class
     */
    public static class PositionInfo {
        public String symbol;
        public String side; // "buy" or "sell"
        public double size;
        public double entryPrice;
        public double markPrice;
        public double unrealizedPnl;
        public double realizedPnl;
        public int productId;
        
        public boolean isOpen() {
            return symbol != null && size > 0;
        }
        
        @Override
        public String toString() {
            return String.format("Position{symbol='%s', side='%s', size=%.2f, entry=%.2f, mark=%.2f, pnl=%.2f}", 
                               symbol, side, size, entryPrice, markPrice, unrealizedPnl);
        }
    }
    
    /**
     * Main method for testing
     */
    public static void main(String[] args) {
        PositionChecker checker = new PositionChecker();
        checker.printOpenPositions();
    }
}
