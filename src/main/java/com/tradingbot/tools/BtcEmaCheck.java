package com.tradingbot.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class BtcEmaCheck {

    public static void main(String[] args) throws Exception {
        String symbol = (args != null && args.length > 0 && args[0] != null && !args[0].isBlank()) ? args[0] : "BTCUSD";
        long now = System.currentTimeMillis() / 1000;
        int candlesToFetch = 500; // enough to compute EMA-200 robustly
        long start = now - (candlesToFetch * 15L * 60L);

        // Use india host with symbol and MARK price to mirror chart
        String url = "https://api.india.delta.exchange/v2/history/candles?resolution=15m&symbol=" + symbol + "&price_type=mark&start=" + start + "&end=" + now;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            System.out.println("❌ Failed to fetch candles: HTTP " + response.statusCode());
            return;
        }

        List<Double> closes = extractCloses(response.body());
        if (closes.size() < 200) {
            System.out.println("❌ Not enough candles to compute EMA-200. Got: " + closes.size());
            return;
        }

        // Use last FULLY CLOSED candle: exclude the most recent in-progress candle
        if (closes.size() < 201) {
            System.out.println("❌ Not enough closed candles to compute EMA-200 reliably.");
            return;
        }
        List<Double> closedCloses = closes.subList(0, closes.size() - 1);
        double ema200 = computeEma(closedCloses, 200);
        double lastClose = closedCloses.get(closedCloses.size() - 1);

        String side = lastClose > ema200 ? "ABOVE" : (lastClose < ema200 ? "BELOW" : "AT");
        System.out.println(symbol + " 15m: " + side + " 200 EMA");
        System.out.println("Last Close: " + String.format("%.2f", lastClose));
        System.out.println("EMA-200 : " + String.format("%.2f", ema200));
        // Print last three closed candle closes for verification
        int n = closedCloses.size();
        if (n >= 3) {
            System.out.println("Closed Closes (last 3): "
                    + String.format("%.2f", closedCloses.get(n-3)) + ", "
                    + String.format("%.2f", closedCloses.get(n-2)) + ", "
                    + String.format("%.2f", closedCloses.get(n-1)));
        }

        // Also print live ticker (traded price and mark price) for BTCUSD from Delta India endpoint
        try {
            String tickerUrl = "https://api.india.delta.exchange/v2/tickers?symbol=" + symbol;
            HttpRequest tickerReq = HttpRequest.newBuilder()
                    .uri(URI.create(tickerUrl))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> tickerResp = client.send(tickerReq, HttpResponse.BodyHandlers.ofString());
            if (tickerResp.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode tickRoot = mapper.readTree(tickerResp.body());
                JsonNode arr = tickRoot.get("result");
                if (arr != null && arr.isArray()) {
                    for (JsonNode item : arr) {
                        JsonNode sym = item.get("symbol");
                        if (sym != null && symbol.equals(sym.asText())) {
                            double last = parseDouble(item.get("last_price"));
                            double mark = parseDouble(item.get("mark_price"));
                            System.out.println("Ticker last: " + String.format("%.2f", last) + " | mark: " + String.format("%.2f", mark));
                            break;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private static List<Double> extractCloses(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        JsonNode result = root.get("result");
        List<Double> closes = new ArrayList<>();
        if (result != null && result.isArray()) {
            for (JsonNode candle : result) {
                if (candle.isArray()) {
                    // Array form: [time, open, high, low, close, volume]
                    if (candle.size() >= 5 && candle.get(4).isNumber()) {
                        closes.add(candle.get(4).asDouble());
                    }
                } else {
                    // Object form: { time, open, high, low, close, volume }
                    JsonNode close = candle.get("close");
                    if (close != null && close.isNumber()) {
                        closes.add(close.asDouble());
                    }
                }
            }
        }
        return closes;
    }

    private static double computeEma(List<Double> closes, int period) {
        // Initial SMA for first EMA point
        double sum = 0.0;
        for (int i = 0; i < period; i++) {
            sum += closes.get(i);
        }
        double ema = sum / period;

        double multiplier = 2.0 / (period + 1);
        for (int i = period; i < closes.size(); i++) {
            ema = (closes.get(i) - ema) * multiplier + ema;
        }
        return ema;
    }

    private static double parseDouble(JsonNode node) {
        if (node == null || node.isNull()) return Double.NaN;
        if (node.isNumber()) return node.asDouble();
        if (node.isTextual()) {
            try { return Double.parseDouble(node.asText()); } catch (Exception ignored) { return Double.NaN; }
        }
        return Double.NaN;
    }
}


