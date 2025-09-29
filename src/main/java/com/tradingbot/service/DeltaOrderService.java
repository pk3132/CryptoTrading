package com.tradingbot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class DeltaOrderService {

    // LIVE API credentials (Production)
    private static final String API_KEY = "RRn7ddViVddcGGncWnvPZxJoI1OlRY";
    private static final String API_SECRET = "VnlMB2puay73RBJKMLPqarSTJU9jOuRe14AIqhCcViyvvRA2eGMpnfbakkg0";
    private static final String BASE_URL = "https://api.india.delta.exchange";

    // CORRECT PRODUCT IDs (verified from API)
    private static final String BTC_PRODUCT_ID = "84"; // BTCUSD Perpetual
    private static final String ETH_PRODUCT_ID = "1699"; // ETHUSD Perpetual

    /**
     * Place BUY order
     */
    public Map<String, Object> placeBuyOrder(BuyOrderRequest request) {
        return placeOrder(request.toOrderData("buy"));
    }

    /**
     * Place SELL order
     */
    public Map<String, Object> placeSellOrder(SellOrderRequest request) {
        return placeOrder(request.toOrderData("sell"));
    }

    /**
     * Get correct product ID for symbol
     */
    public static String getProductId(String symbol) {
        if ("BTCUSD".equals(symbol)) {
            return BTC_PRODUCT_ID;
        } else if ("ETHUSD".equals(symbol)) {
            return ETH_PRODUCT_ID;
        }
        return null; // Unknown symbol
    }

    /**
     * Check if symbol is supported for trading
     */
    public static boolean isSymbolSupported(String symbol) {
        return "BTCUSD".equals(symbol) || "ETHUSD".equals(symbol);
    }

    /**
     * Get contract size for symbol
     */
    public static double getContractSize(String symbol) {
        if ("BTCUSD".equals(symbol)) {
            return 0.001; // 0.001 BTC per contract
        } else if ("ETHUSD".equals(symbol)) {
            return 0.01; // 0.01 ETH per contract
        }
        return 1.0; // Default
    }

    /**
     * Set leverage for a product
     */
    public Map<String, Object> setLeverage(String productId, int leverage) {
        try {
            String method = "POST";
            String path = "/v2/products/" + productId + "/orders/leverage";
            String queryString = "";
            String payload = "{\"leverage\":\"" + leverage + "\"}";
            String timestamp = String.valueOf(Instant.now().getEpochSecond());

            // Generate signature
            String signatureData = method + timestamp + path + queryString + payload;
            String signature = generateSignature(API_SECRET, signatureData);

            // Build request
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("api-key", API_KEY)
                .header("timestamp", timestamp)
                .header("signature", signature)
                .header("User-Agent", "java-rest-client")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

            // Send request
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(response.body(), Map.class);
            } else {
                System.err.println("Leverage setup failed with status: " + response.statusCode());
                System.err.println("Response: " + response.body());
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error setting leverage: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Core order placement method
     */
    private Map<String, Object> placeOrder(Map<String, Object> orderData) {
        try {
            String method = "POST";
            String path = "/v2/orders";
            String queryString = "";
            String payload = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(orderData);
            String timestamp = String.valueOf(Instant.now().getEpochSecond());

            // Generate signature
            String signatureData = method + timestamp + path + queryString + payload;
            String signature = generateSignature(API_SECRET, signatureData);

            // Build request
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("api-key", API_KEY)
                .header("timestamp", timestamp)
                .header("signature", signature)
                .header("User-Agent", "java-rest-client")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

            // Send request
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(response.body(), Map.class);
            } else {
                System.err.println("Order placement failed with status: " + response.statusCode());
                System.err.println("Response: " + response.body());
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error placing order: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

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
     * BUY Order Request Class
     */
    public static class BuyOrderRequest {
        private String productSymbol;
        private String productId;
        private Integer size;
        private String orderType;
        private String limitPrice;
        private String stopOrderType;
        private String stopPrice;
        private String trailAmount;
        private String stopTriggerMethod = "mark_price";
        private String timeInForce = "gtc";
        private Boolean postOnly = false;
        private Boolean reduceOnly = false;
        private String clientOrderId;
        private String leverage;

        // Constructors
        public BuyOrderRequest() {}

        public BuyOrderRequest(String productSymbol, Integer size, String orderType) {
            this.productSymbol = productSymbol;
            this.size = size;
            this.orderType = orderType;
        }

        // Getters and Setters
        public String getProductSymbol() { return productSymbol; }
        public void setProductSymbol(String productSymbol) { this.productSymbol = productSymbol; }

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }

        public Integer getSize() { return size; }
        public void setSize(Integer size) { this.size = size; }

        public String getOrderType() { return orderType; }
        public void setOrderType(String orderType) { this.orderType = orderType; }

        public String getLimitPrice() { return limitPrice; }
        public void setLimitPrice(String limitPrice) { this.limitPrice = limitPrice; }

        public String getStopOrderType() { return stopOrderType; }
        public void setStopOrderType(String stopOrderType) { this.stopOrderType = stopOrderType; }

        public String getStopPrice() { return stopPrice; }
        public void setStopPrice(String stopPrice) { this.stopPrice = stopPrice; }

        public String getTrailAmount() { return trailAmount; }
        public void setTrailAmount(String trailAmount) { this.trailAmount = trailAmount; }

        public String getStopTriggerMethod() { return stopTriggerMethod; }
        public void setStopTriggerMethod(String stopTriggerMethod) { this.stopTriggerMethod = stopTriggerMethod; }

        public String getTimeInForce() { return timeInForce; }
        public void setTimeInForce(String timeInForce) { this.timeInForce = timeInForce; }

        public Boolean getPostOnly() { return postOnly; }
        public void setPostOnly(Boolean postOnly) { this.postOnly = postOnly; }

        public Boolean getReduceOnly() { return reduceOnly; }
        public void setReduceOnly(Boolean reduceOnly) { this.reduceOnly = reduceOnly; }

        public String getClientOrderId() { return clientOrderId; }
        public void setClientOrderId(String clientOrderId) { this.clientOrderId = clientOrderId; }

        public String getLeverage() { return leverage; }
        public void setLeverage(String leverage) { this.leverage = leverage; }

        public Map<String, Object> toOrderData(String side) {
            Map<String, Object> orderData = new HashMap<>();
            if (productId != null) {
                orderData.put("product_id", Integer.parseInt(productId));
            } else {
                orderData.put("product_symbol", productSymbol);
            }
            orderData.put("side", side);
            orderData.put("size", size);
            orderData.put("order_type", orderType);

            if (orderType.equals("limit_order") && limitPrice != null) {
                orderData.put("limit_price", limitPrice);
            }

            if (stopOrderType != null) {
                orderData.put("stop_order_type", stopOrderType);
            }

            if (stopPrice != null) {
                orderData.put("stop_price", stopPrice);
            }

            if (trailAmount != null) {
                orderData.put("trail_amount", trailAmount);
            }

            if (stopTriggerMethod != null) {
                orderData.put("stop_trigger_method", stopTriggerMethod);
            }

            if (timeInForce != null) {
                orderData.put("time_in_force", timeInForce);
            }

            if (postOnly != null) {
                orderData.put("post_only", postOnly);
            }

            if (reduceOnly != null) {
                orderData.put("reduce_only", reduceOnly);
            }

            if (clientOrderId != null) {
                orderData.put("client_order_id", clientOrderId);
            }

            if (leverage != null) {
                orderData.put("leverage", leverage);
            }

            return orderData;
        }
    }

    /**
     * SELL Order Request Class
     */
    public static class SellOrderRequest {
        private String productSymbol;
        private String productId;
        private Integer size;
        private String orderType;
        private String limitPrice;
        private String stopOrderType;
        private String stopPrice;
        private String trailAmount;
        private String stopTriggerMethod = "mark_price";
        private String timeInForce = "gtc";
        private Boolean postOnly = false;
        private Boolean reduceOnly = false;
        private String clientOrderId;
        private String leverage;

        // Constructors
        public SellOrderRequest() {}

        public SellOrderRequest(String productSymbol, Integer size, String orderType) {
            this.productSymbol = productSymbol;
            this.size = size;
            this.orderType = orderType;
        }

        // Getters and Setters
        public String getProductSymbol() { return productSymbol; }
        public void setProductSymbol(String productSymbol) { this.productSymbol = productSymbol; }

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }

        public Integer getSize() { return size; }
        public void setSize(Integer size) { this.size = size; }

        public String getOrderType() { return orderType; }
        public void setOrderType(String orderType) { this.orderType = orderType; }

        public String getLimitPrice() { return limitPrice; }
        public void setLimitPrice(String limitPrice) { this.limitPrice = limitPrice; }

        public String getStopOrderType() { return stopOrderType; }
        public void setStopOrderType(String stopOrderType) { this.stopOrderType = stopOrderType; }

        public String getStopPrice() { return stopPrice; }
        public void setStopPrice(String stopPrice) { this.stopPrice = stopPrice; }

        public String getTrailAmount() { return trailAmount; }
        public void setTrailAmount(String trailAmount) { this.trailAmount = trailAmount; }

        public String getStopTriggerMethod() { return stopTriggerMethod; }
        public void setStopTriggerMethod(String stopTriggerMethod) { this.stopTriggerMethod = stopTriggerMethod; }

        public String getTimeInForce() { return timeInForce; }
        public void setTimeInForce(String timeInForce) { this.timeInForce = timeInForce; }

        public Boolean getPostOnly() { return postOnly; }
        public void setPostOnly(Boolean postOnly) { this.postOnly = postOnly; }

        public Boolean getReduceOnly() { return reduceOnly; }
        public void setReduceOnly(Boolean reduceOnly) { this.reduceOnly = reduceOnly; }

        public String getClientOrderId() { return clientOrderId; }
        public void setClientOrderId(String clientOrderId) { this.clientOrderId = clientOrderId; }

        public String getLeverage() { return leverage; }
        public void setLeverage(String leverage) { this.leverage = leverage; }

        public Map<String, Object> toOrderData(String side) {
            Map<String, Object> orderData = new HashMap<>();
            if (productId != null) {
                orderData.put("product_id", Integer.parseInt(productId));
            } else {
                orderData.put("product_symbol", productSymbol);
            }
            orderData.put("side", side);
            orderData.put("size", size);
            orderData.put("order_type", orderType);

            if (orderType.equals("limit_order") && limitPrice != null) {
                orderData.put("limit_price", limitPrice);
            }

            if (stopOrderType != null) {
                orderData.put("stop_order_type", stopOrderType);
            }

            if (stopPrice != null) {
                orderData.put("stop_price", stopPrice);
            }

            if (trailAmount != null) {
                orderData.put("trail_amount", trailAmount);
            }

            if (stopTriggerMethod != null) {
                orderData.put("stop_trigger_method", stopTriggerMethod);
            }

            if (timeInForce != null) {
                orderData.put("time_in_force", timeInForce);
            }

            if (postOnly != null) {
                orderData.put("post_only", postOnly);
            }

            if (reduceOnly != null) {
                orderData.put("reduce_only", reduceOnly);
            }

            if (clientOrderId != null) {
                orderData.put("client_order_id", clientOrderId);
            }

            if (leverage != null) {
                orderData.put("leverage", leverage);
            }

            return orderData;
        }
    }
}
