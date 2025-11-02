package com.tradingbot.tools;

import com.tradingbot.service.DeltaApiClient;
import com.tradingbot.strategy.EMA200TrendlineStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/debug")
public class MarketDebugController {

    @Autowired
    private DeltaApiClient deltaApiClient;

    @Autowired
    private EMA200TrendlineStrategy ema200Strategy;

    @GetMapping("/ema200/{symbol}")
    public Map<String, Object> checkEma200(
            @PathVariable("symbol") String symbol,
            @RequestParam(value = "resolution", required = false, defaultValue = "1m") String resolution,
            @RequestParam(value = "candles", required = false, defaultValue = "500") int candlesCount
    ) {
        Map<String, Object> result = new HashMap<>();
        try {
            Double ema200;
            Double mark = deltaApiClient.getCurrentMarkPrice(symbol);
            
            // For non-1m resolutions, use the higher timeframe method directly
            if (!"1m".equals(resolution)) {
                // Use getHigherTimeframeEma200 for 15m, 5m, 1h, etc.
                ema200 = ema200Strategy.getHigherTimeframeEma200(symbol, resolution);
            } else {
                // For 1m, fetch candles and calculate normally
                long now = Instant.now().getEpochSecond();
                long secondsPerCandle = secondsForResolution(resolution);
                long start = now - (candlesCount * secondsPerCandle);
                
                List<Map<String, Object>> ohlcv = deltaApiClient.fetchOhlcv(symbol, resolution, start, now);
                if (ohlcv != null && !ohlcv.isEmpty()) {
                    ema200Strategy.addCandleData(symbol, ohlcv);
                }
                ema200 = ema200Strategy.getLastEma200(symbol);
            }

            result.put("symbol", symbol);
            result.put("resolution", resolution);
            result.put("ema200", ema200);
            result.put("markPrice", mark);

            if (ema200 == null || mark == null) {
                result.put("status", "unknown");
                result.put("message", "Unable to compute EMA200 or mark price");
                return result;
            }

            String relation = mark >= ema200 ? "above" : "below";
            result.put("relation", relation);
            result.put("aboveEma200", mark >= ema200);
            return result;
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return result;
        }
    }

    private long secondsForResolution(String resolution) {
        switch (resolution) {
            case "1m": return 60L;
            case "3m": return 180L;
            case "5m": return 300L;
            case "15m": return 900L;
            case "30m": return 1800L;
            case "1h": return 3600L;
            case "4h": return 14400L;
            case "1d": return 86400L;
            default: return 60L; // fallback to 1m
        }
    }
}


