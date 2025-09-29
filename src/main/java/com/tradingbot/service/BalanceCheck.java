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
import java.util.HashMap;
import java.util.Map;

/**
 * Reusable Balance Check Service for Delta Exchange
 * Provides easy methods to check account balance whenever required
 */
@Service
public class BalanceCheck {
    
    private static final Logger logger = LoggerFactory.getLogger(BalanceCheck.class);
    
    // LIVE API credentials
    private static final String API_KEY = "RRn7ddViVddcGGncWnvPZxJoI1OlRY";
    private static final String API_SECRET = "VnlMB2puay73RBJKMLPqarSTJU9jOuRe14AIqhCcViyvvRA2eGMpnfbakkg0";
    private static final String BASE_URL = "https://api.india.delta.exchange";
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public BalanceCheck() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Generate HMAC SHA256 signature for Delta Exchange API
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
     * Get current account balance from Delta Exchange
     * @return Map containing balance information or null if failed
     */
    public Map<String, Object> getCurrentBalance() {
        try {
            String method = "GET";
            String path = "/v2/wallet/balances";
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
            
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                
                if (jsonResponse.has("success") && jsonResponse.get("success").asBoolean()) {
                    return parseBalanceResponse(jsonResponse);
                } else {
                    logger.error("❌ API returned success: false");
                    return null;
                }
            } else {
                logger.error("❌ Balance check failed with status: {}", response.statusCode());
                logger.error("📄 Response: {}", response.body());
                return null;
            }
            
        } catch (Exception e) {
            logger.error("❌ Error checking balance: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Parse balance response and return structured data
     */
    private Map<String, Object> parseBalanceResponse(JsonNode jsonResponse) {
        Map<String, Object> balanceInfo = new HashMap<>();
        
        try {
            // Extract meta information
            if (jsonResponse.has("meta")) {
                JsonNode meta = jsonResponse.get("meta");
                Map<String, Object> metaInfo = new HashMap<>();
                
                if (meta.has("net_equity")) {
                    metaInfo.put("net_equity", meta.get("net_equity").asText());
                }
                if (meta.has("robo_trading_equity")) {
                    metaInfo.put("robo_trading_equity", meta.get("robo_trading_equity").asText());
                }
                if (meta.has("tracker_equity")) {
                    metaInfo.put("tracker_equity", meta.get("tracker_equity").asText());
                }
                
                balanceInfo.put("meta", metaInfo);
            }
            
            // Extract wallet balances
            if (jsonResponse.has("result") && jsonResponse.get("result").isArray()) {
                JsonNode wallets = jsonResponse.get("result");
                
                for (JsonNode wallet : wallets) {
                    Map<String, Object> walletInfo = new HashMap<>();
                    
                    if (wallet.has("asset_symbol")) {
                        walletInfo.put("asset_symbol", wallet.get("asset_symbol").asText());
                    }
                    if (wallet.has("balance")) {
                        walletInfo.put("balance", wallet.get("balance").asText());
                    }
                    if (wallet.has("available_balance")) {
                        walletInfo.put("available_balance", wallet.get("available_balance").asText());
                    }
                    if (wallet.has("available_balance_for_robo")) {
                        walletInfo.put("available_balance_for_robo", wallet.get("available_balance_for_robo").asText());
                    }
                    if (wallet.has("balance_inr")) {
                        walletInfo.put("balance_inr", wallet.get("balance_inr").asText());
                    }
                    if (wallet.has("blocked_margin")) {
                        walletInfo.put("blocked_margin", wallet.get("blocked_margin").asText());
                    }
                    if (wallet.has("position_margin")) {
                        walletInfo.put("position_margin", wallet.get("position_margin").asText());
                    }
                    if (wallet.has("order_margin")) {
                        walletInfo.put("order_margin", wallet.get("order_margin").asText());
                    }
                    
                    balanceInfo.put("wallet", walletInfo);
                    break; // Assuming single wallet for now
                }
            }
            
            balanceInfo.put("success", true);
            balanceInfo.put("timestamp", Instant.now().toString());
            
        } catch (Exception e) {
            logger.error("❌ Error parsing balance response: {}", e.getMessage());
            balanceInfo.put("success", false);
            balanceInfo.put("error", e.getMessage());
        }
        
        return balanceInfo;
    }
    
    /**
     * Get balance as a formatted string for display
     * @return Formatted balance string
     */
    public String getBalanceSummary() {
        Map<String, Object> balance = getCurrentBalance();
        
        if (balance == null || !(Boolean) balance.getOrDefault("success", false)) {
            return "❌ Failed to retrieve balance information";
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append("💰 Delta Exchange Balance Summary\n");
        summary.append("================================\n");
        
        // Add meta information
        if (balance.containsKey("meta")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> meta = (Map<String, Object>) balance.get("meta");
            
            if (meta.containsKey("net_equity")) {
                summary.append("💰 Net Equity: $").append(meta.get("net_equity")).append("\n");
            }
            if (meta.containsKey("robo_trading_equity")) {
                summary.append("🤖 Robo Trading Equity: $").append(meta.get("robo_trading_equity")).append("\n");
            }
        }
        
        // Add wallet information
        if (balance.containsKey("wallet")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> wallet = (Map<String, Object>) balance.get("wallet");
            
            summary.append("\n💳 Wallet Details:\n");
            summary.append("🏦 Asset: ").append(wallet.getOrDefault("asset_symbol", "N/A")).append("\n");
            summary.append("💰 Total Balance: $").append(wallet.getOrDefault("balance", "0")).append("\n");
            summary.append("✅ Available Balance: $").append(wallet.getOrDefault("available_balance", "0")).append("\n");
            summary.append("🤖 Available for Robo: $").append(wallet.getOrDefault("available_balance_for_robo", "0")).append("\n");
            
            if (wallet.containsKey("balance_inr")) {
                summary.append("💱 Balance in INR: ₹").append(wallet.get("balance_inr")).append("\n");
            }
        }
        
        summary.append("\n🎯 Account Status: ✅ ACTIVE");
        summary.append("\n🚀 Ready for LIVE TRADING!");
        
        return summary.toString();
    }
    
    /**
     * Get available balance as a double for calculations
     * @return Available balance as double or 0.0 if failed
     */
    public double getAvailableBalanceAsDouble() {
        Map<String, Object> balance = getCurrentBalance();
        
        if (balance == null || !(Boolean) balance.getOrDefault("success", false)) {
            logger.error("❌ Failed to get available balance");
            return 0.0;
        }
        
        if (balance.containsKey("wallet")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> wallet = (Map<String, Object>) balance.get("wallet");
            
            if (wallet.containsKey("available_balance")) {
                try {
                    return Double.parseDouble(wallet.get("available_balance").toString());
                } catch (NumberFormatException e) {
                    logger.error("❌ Error parsing available balance: {}", e.getMessage());
                    return 0.0;
                }
            }
        }
        
        return 0.0;
    }
    
    /**
     * Check if account has sufficient balance for trading
     * @param requiredAmount Minimum amount required
     * @return true if sufficient balance available
     */
    public boolean hasSufficientBalance(double requiredAmount) {
        double availableBalance = getAvailableBalanceAsDouble();
        boolean sufficient = availableBalance >= requiredAmount;
        
        logger.info("💰 Balance Check: Available=${}, Required=${}, Sufficient={}", 
                   availableBalance, requiredAmount, sufficient);
        
        return sufficient;
    }
    
    /**
     * Print balance information to console
     */
    public void printBalance() {
        logger.info("🔍 Checking Delta Exchange Balance...");
        String summary = getBalanceSummary();
        logger.info("\n{}", summary);
    }
    
    /**
     * Main method for standalone execution
     */
    public static void main(String[] args) {
        BalanceCheck balanceCheck = new BalanceCheck();
        balanceCheck.printBalance();
    }
}
