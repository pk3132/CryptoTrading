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
import java.util.Base64;

/**
 * Delta Exchange Balance Checker
 * Checks wallet balances for both Demo and Live accounts
 */
public class DeltaExchangeBalanceChecker {
    
    private static final Logger logger = LoggerFactory.getLogger(DeltaExchangeBalanceChecker.class);
    
    // LIVE API credentials
    private static final String LIVE_API_KEY = "RRn7ddViVddcGGncWnvPZxJoI1OlRY";
    private static final String LIVE_API_SECRET = "VnlMB2puay73RBJKMLPqarSTJU9jOuRe14AIqhCcViyvvRA2eGMpnfbakkg0";
    private static final String LIVE_BASE_URL = "https://api.india.delta.exchange/v2";
    
    // DEMO API credentials (commented out - demo account removed)
    // private static final String DEMO_API_KEY = "nABsIN02ZlNaaf7pk9WJeldBei7lnx";
    // private static final String DEMO_API_SECRET = "JRIGVnKeoH5MSxfPTPOeFhh41JCT8PHprQkQsJvcLZjGnR7YRacwMd9Pd9Gt";
    // private static final String DEMO_BASE_URL = "https://cdn-ind.testnet.deltaex.org/v2";
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public DeltaExchangeBalanceChecker() {
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
        
        // Convert to hex string (Delta Exchange expects hex, not base64)
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    /**
     * Get wallet balances from Delta Exchange
     */
    public void getWalletBalances() {
        try {
            String method = "GET";
            String path = "/wallet/balances";
            String queryString = ""; // Empty for GET request without parameters
            String body = ""; // Empty for GET request
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            
            // Use LIVE API credentials
            String apiKey = LIVE_API_KEY;
            String apiSecret = LIVE_API_SECRET;
            String baseUrl = LIVE_BASE_URL;
            
            // Generate signature (correct format: method + timestamp + path + queryString + body)
            // For GET /wallet/balances: GET + timestamp + /wallet/balances + "" + ""
            String signatureData = method + timestamp + path + queryString + body;
            String signature = generateSignature(apiSecret, signatureData);
            
            // Debug signature data
            logger.info("🔐 Signature Data: '{}'", signatureData);
            logger.info("🔑 Generated Signature: {}", signature);
            logger.info("⏰ Timestamp: {}", timestamp);
            
            // Build request with correct headers
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("api-key", apiKey)
                .header("timestamp", timestamp)
                .header("signature", signature)
                .header("User-Agent", "java-rest-client") // Required to avoid 4XX errors
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .GET()
                .build();
            
            // Send request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            logger.info("🔍 Fetching Delta Exchange LIVE Account Balances...");
            logger.info("🌐 Base URL: {}", baseUrl);
            logger.info("📊 API Response Status: {}", response.statusCode());
            logger.info("");
            
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                
                if (jsonResponse.has("success") && jsonResponse.get("success").asBoolean()) {
                    logger.info("✅ Successfully retrieved wallet balances!");
                    logger.info("");
                    
                    // Display meta information
                    if (jsonResponse.has("meta")) {
                        JsonNode meta = jsonResponse.get("meta");
                        logger.info("📊 Account Summary:");
                        if (meta.has("net_equity")) {
                            logger.info("💰 Net Equity: ${}", meta.get("net_equity").asText());
                        }
                        if (meta.has("robo_trading_equity")) {
                            logger.info("🤖 Robo Trading Equity: ${}", meta.get("robo_trading_equity").asText());
                        }
                        logger.info("");
                    }
                    
                    // Display wallet balances
                    if (jsonResponse.has("result") && jsonResponse.get("result").isArray()) {
                        JsonNode wallets = jsonResponse.get("result");
                        
                        logger.info("💳 Wallet Balances:");
                        logger.info("==================");
                        
                        for (JsonNode wallet : wallets) {
                            logger.info("🏦 Asset: {}", wallet.get("asset_symbol").asText());
                            logger.info("  💰 Total Balance: ${}", wallet.get("balance").asText());
                            logger.info("  ✅ Available Balance: ${}", wallet.get("available_balance").asText());
                            logger.info("  🤖 Available for Robo: ${}", wallet.get("available_balance_for_robo").asText());
                            logger.info("  🔒 Blocked Margin: ${}", wallet.get("blocked_margin").asText());
                            logger.info("  📈 Position Margin: ${}", wallet.get("position_margin").asText());
                            logger.info("  📋 Order Margin: ${}", wallet.get("order_margin").asText());
                            logger.info("  💸 Commission Blocked: ${}", wallet.get("commission").asText());
                            
                            // Cross margin details
                            if (wallet.has("cross_position_margin")) {
                                logger.info("  🔄 Cross Position Margin: ${}", wallet.get("cross_position_margin").asText());
                            }
                            if (wallet.has("cross_order_margin")) {
                                logger.info("  🔄 Cross Order Margin: ${}", wallet.get("cross_order_margin").asText());
                            }
                            logger.info("");
                        }
                        
                        logger.info("🎯 LIVE Account Status: ✅ ACTIVE");
                        logger.info("🚀 Ready for LIVE TRADING!");
                        
                    } else {
                        logger.warn("⚠️ No wallet data found in response");
                    }
                    
                } else {
                    logger.error("❌ API call failed - Success: false");
                    logger.error("📄 Response: {}", responseBody);
                }
                
            } else {
                logger.error("❌ API call failed with status: {}", response.statusCode());
                logger.error("📄 Response: {}", response.body());
            }
            
        } catch (Exception e) {
            logger.error("❌ Error processing response: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get balance for a specific asset
     */
    public void getAssetBalance(String assetSymbol) {
        try {
            getWalletBalances(); // Get all balances first
            // You could filter here for specific asset if needed
        } catch (Exception e) {
            logger.error("❌ Error getting asset balance: {}", e.getMessage(), e);
        }
    }
    
    public static void main(String[] args) {
        DeltaExchangeBalanceChecker balanceChecker = new DeltaExchangeBalanceChecker();
        
        logger.info("💰 Delta Exchange LIVE Account Balance Checker");
        logger.info("==============================================");
        logger.info("");
        
        // Get all wallet balances
        balanceChecker.getWalletBalances();
        
        logger.info("");
        logger.info("✅ Balance Check Complete!");
    }
}
