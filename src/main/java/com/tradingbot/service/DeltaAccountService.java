package com.tradingbot.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Delta Exchange Account Service
 * Handles account-related operations like balance checking
 */
public class DeltaAccountService {

    private final String apiKey;
    private final String apiSecret;
    private final String baseUrl;

    public DeltaAccountService(String apiKey, String apiSecret, String baseUrl) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.baseUrl = baseUrl;
    }

    /**
     * Check account balance
     */
    public String checkBalance() throws Exception {
        String path = "/v2/wallet/balances";
        String method = "GET";
        String queryString = "";
        String payload = "";
        long timestamp = Instant.now().getEpochSecond();

        // Build signature according to Delta API docs: method + timestamp + path + queryString + payload
        String prehash = method + timestamp + path + queryString + payload;
        String signature = generateSignature(apiSecret, prehash);

        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("api-key", apiKey)
                .header("timestamp", String.valueOf(timestamp))
                .header("signature", signature)
                .GET()
                .build();

        System.out.println("Sending request to: " + baseUrl + path);
        System.out.println("API Key: " + apiKey.substring(0, 8) + "...");
        System.out.println("Timestamp: " + timestamp);
        System.out.println("Signature: " + signature.substring(0, 16) + "...");
        System.out.println();
        
        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("HTTP Status: " + resp.statusCode());
        System.out.println("Response body:\n" + resp.body());
        
        if (resp.statusCode() == 200) {
            System.out.println("\n✅ SUCCESS: API connection working! You should see JSON balances above.");
            return resp.body();
        } else {
            System.out.println("\n❌ ERROR: Expected HTTP 200, got " + resp.statusCode());
            System.out.println("Check your API key, secret, and IP whitelist settings.");
            return null;
        }
    }

    private String generateSignature(String secret, String message) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256"));
        byte[] sig = mac.doFinal(message.getBytes("UTF-8"));
        
        // Convert to hex string (not Base64)
        StringBuilder hexString = new StringBuilder();
        for (byte b : sig) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Main method for testing
     */
    public static void main(String[] args) throws Exception {
        String apiKey = System.getenv("DELTA_API_KEY");
        String apiSecret = System.getenv("DELTA_API_SECRET");
        String baseUrl = System.getenv("DELTA_BASE_URL");
        
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.india.delta.exchange";
        }

        if (apiKey == null || apiSecret == null) {
            System.err.println("ERROR: Set DELTA_API_KEY and DELTA_API_SECRET env variables first.");
            System.err.println("Current values:");
            System.err.println("  DELTA_API_KEY: " + (apiKey != null ? "SET" : "NOT SET"));
            System.err.println("  DELTA_API_SECRET: " + (apiSecret != null ? "SET" : "NOT SET"));
            System.err.println("  DELTA_BASE_URL: " + baseUrl);
            System.exit(1);
        }

        DeltaAccountService accountService = new DeltaAccountService(apiKey, apiSecret, baseUrl);
        accountService.checkBalance();
    }
}
