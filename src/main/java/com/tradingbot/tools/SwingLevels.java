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

public class SwingLevels {
    private static final int SEPARATION = 5;

    public static void main(String[] args) throws Exception {
        String symbol = (args != null && args.length > 0 && args[0] != null && !args[0].isBlank()) ? args[0] : "BTCUSD";
        long now = System.currentTimeMillis() / 1000;
        int candlesToFetch = 300;
        long start = now - (candlesToFetch * 15L * 60L);

        String url = "https://api.india.delta.exchange/v2/history/candles?resolution=15m&symbol=" + symbol + "&price_type=mark&start=" + start + "&end=" + now;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(15)).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            System.out.println("❌ Failed to fetch candles: HTTP " + resp.statusCode());
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(resp.body());
        JsonNode result = root.get("result");
        if (result == null || !result.isArray() || result.size() < SEPARATION * 2 + 1) {
            System.out.println("❌ Not enough candles");
            return;
        }

        List<Double> highs = new ArrayList<>();
        List<Double> lows  = new ArrayList<>();
        List<Long> times   = new ArrayList<>();
        for (JsonNode c : result) {
            if (c.isArray() && c.size() >= 5) {
                times.add(c.get(0).asLong());
                highs.add(c.get(2).asDouble());
                lows.add(c.get(3).asDouble());
            } else if (!c.isArray()) {
                times.add(c.get("time").asLong());
                highs.add(c.get("high").asDouble());
                lows.add(c.get("low").asDouble());
            }
        }

        // find most recent swing high/low
        Double lastSwingHigh = null, lastSwingLow = null;
        for (int i = SEPARATION; i < highs.size() - SEPARATION; i++) {
            boolean isHigh = true, isLow = true;
            for (int j = 1; j <= SEPARATION; j++) {
                if (!(highs.get(i) > highs.get(i - j) && highs.get(i) > highs.get(i + j))) isHigh = false;
                if (!(lows.get(i) < lows.get(i - j) && lows.get(i) < lows.get(i + j))) isLow = false;
                if (!isHigh && !isLow) break;
            }
            if (isHigh) lastSwingHigh = highs.get(i);
            if (isLow)  lastSwingLow  = lows.get(i);
        }

        // fetch current mark
        String tUrl = "https://api.india.delta.exchange/v2/tickers?symbol=" + symbol;
        HttpRequest tReq = HttpRequest.newBuilder().uri(URI.create(tUrl)).timeout(Duration.ofSeconds(10)).GET().build();
        HttpResponse<String> tResp = client.send(tReq, HttpResponse.BodyHandlers.ofString());
        double mark = Double.NaN;
        if (tResp.statusCode() == 200) {
            JsonNode tr = mapper.readTree(tResp.body());
            JsonNode arr = tr.get("result");
            if (arr != null && arr.isArray() && arr.size() > 0) {
                JsonNode item = arr.get(0);
                mark = item.get("mark_price").asDouble();
            }
        }

        System.out.println("Symbol: " + symbol);
        System.out.println("Current mark: " + String.format("%.2f", mark));
        if (lastSwingHigh != null) System.out.println("Swing High: " + String.format("%.2f", lastSwingHigh));
        else System.out.println("Swing High: -");
        if (lastSwingLow != null) System.out.println("Swing Low:  " + String.format("%.2f", lastSwingLow));
        else System.out.println("Swing Low:  -");
    }
}


