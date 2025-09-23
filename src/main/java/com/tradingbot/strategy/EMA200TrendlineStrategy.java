package com.tradingbot.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

@Service
public class EMA200TrendlineStrategy {
    
    // Configuration
    private static final String BASE_URL = "https://api.india.delta.exchange";
    private static final String RESOLUTION = "15m";
    private static final int LOOKBACK_PERIOD = 50;
    private static final int EMA_PERIOD = 200;
    // removed unused POSITION_SIZE
    private static final double STOP_LOSS_PCT = 0.005; // 0.50%
    private static final double TAKE_PROFIT_PCT = 0.01; // 1.00%
    private static final int MIN_SWING_SEPARATION = 5;
    
    // Data structures
    public static class Candle {
        public long time;
        public double open, high, low, close, volume;
        public LocalDateTime datetime;
        public double ema200;
        public int signal;
        public int position;
        public double swingHigh = Double.NaN;
        public double swingLow = Double.NaN;
        public double resistanceLine = Double.NaN;
        public double supportLine = Double.NaN;
        
        public Candle(long time, double open, double high, double low, double close, double volume) {
            this.time = time;
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.volume = volume;
            this.datetime = LocalDateTime.ofInstant(Instant.ofEpochSecond(time), ZoneOffset.UTC);
        }
    }
    
    public static class SwingPoint {
        public int index;
        public double price;
        
        public SwingPoint(int index, double price) {
            this.index = index;
            this.price = price;
        }
    }
    
    public static class TradeSignal {
        private String type;
        private double entryPrice;
        private double stopLoss;
        private double takeProfit;
        private String reason;

        public TradeSignal(String type, double entryPrice, double stopLoss, double takeProfit, String reason) {
            this.type = type;
            this.entryPrice = entryPrice;
            this.stopLoss = stopLoss;
            this.takeProfit = takeProfit;
            this.reason = reason;
        }

        // Getters
        public String getType() { return type; }
        public double getEntryPrice() { return entryPrice; }
        public double getStopLoss() { return stopLoss; }
        public double getTakeProfit() { return takeProfit; }
        public String getReason() { return reason; }
    }
    
    public static class TrendLine {
        public double slope;
        public double intercept;
        
        public TrendLine(double slope, double intercept) {
            this.slope = slope;
            this.intercept = intercept;
        }
        
        public double getValue(int x) {
            return slope * x + intercept;
        }
    }
    
    private final Map<String, List<Candle>> candlesData;
    private HttpClient httpClient;
    private ObjectMapper objectMapper;
    @Autowired
    private com.tradingbot.service.DeltaApiClient deltaApiClient;
    
    public EMA200TrendlineStrategy() {
        this.candlesData = new HashMap<>();
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public void addCandleData(String symbol, List<Map<String, Object>> rawCandles) {
        List<Candle> candles = new ArrayList<>();
        for (Map<String, Object> c : rawCandles) {
            candles.add(new Candle(
                ((Number) c.get("time")).longValue(),
                ((Number) c.get("open")).doubleValue(),
                ((Number) c.get("high")).doubleValue(),
                ((Number) c.get("low")).doubleValue(),
                ((Number) c.get("close")).doubleValue(),
                ((Number) c.get("volume")).doubleValue()
            ));
        }
        candlesData.put(symbol, candles);
    }

    public List<TradeSignal> checkSignals(String symbol) {
        List<TradeSignal> signals = new ArrayList<>();
        List<Candle> candles = candlesData.get(symbol);

        if (candles == null || candles.size() < EMA_PERIOD + LOOKBACK_PERIOD) {
            return signals;
        }

        calculateEMA(candles, EMA_PERIOD);
        List<SwingPoint>[] swingPoints = findSwingPoints(candles);
        List<SwingPoint> highs = swingPoints[0];
        List<SwingPoint> lows = swingPoints[1];

        // Get current real-time price from candles (more reliable than tickers endpoint)
        double currentPrice = 0;
        try {
            // Use the getCurrentMarketPrice method which uses candles endpoint
            double marketPrice = getCurrentMarketPrice(symbol);
            if (marketPrice > 0) {
                currentPrice = marketPrice;
            }
        } catch (Exception ignored) {}
        if (currentPrice <= 0) {
            currentPrice = candles.get(candles.size() - 1).close; // Fallback to last candle close
        }

        Candle lastCandle = candles.get(candles.size() - 1);
        double emaValue = lastCandle.ema200; // Use EMA from last full candle

        // Get recent swing points (last 3) relative to the end of the historical data
        List<SwingPoint> recentHighs = highs.stream()
                .filter(h -> h.index >= candles.size() - LOOKBACK_PERIOD && h.index < candles.size())
                .collect(ArrayList::new, (list, item) -> {
                    list.add(item);
                    if (list.size() > 3) list.remove(0);
                }, ArrayList::addAll);
        
        List<SwingPoint> recentLows = lows.stream()
                .filter(l -> l.index >= candles.size() - LOOKBACK_PERIOD && l.index < candles.size())
                .collect(ArrayList::new, (list, item) -> {
                    list.add(item);
                    if (list.size() > 3) list.remove(0);
                }, ArrayList::addAll);

        // Fit trendlines
        TrendLine resistanceLine = fitTrendline(recentHighs);
        TrendLine supportLine = fitTrendline(recentLows);

        // Calculate trendline values at current position (index of the last candle)
        int currentIndex = candles.size() - 1;

        if (resistanceLine != null) {
            double resistanceValue = resistanceLine.getValue(currentIndex);
            
            // LONG signal: Price > EMA200 and breaks above descending resistance line
            if (currentPrice > emaValue && resistanceLine.slope < 0 && currentPrice > resistanceValue) {
                double stopLoss = currentPrice * (1 - STOP_LOSS_PCT);
                double takeProfit = currentPrice * (1 + TAKE_PROFIT_PCT);
                // Emit only if on correct EMA side (redundant safety)
                if (currentPrice >= emaValue) {
                    signals.add(new TradeSignal("BUY", currentPrice, stopLoss, takeProfit,
                        String.format("Bullish Pattern (Above EMA 200, Trendline Breakout, Trend: %.2f%%)", (currentPrice - emaValue) / emaValue * 100)));
                }
            }
        }
        
        if (supportLine != null) {
            double supportValue = supportLine.getValue(currentIndex);
            
            // SHORT signal: Price < EMA200 and breaks below ascending support line
            if (currentPrice < emaValue && supportLine.slope > 0 && currentPrice < supportValue) {
                double stopLoss = currentPrice * (1 + STOP_LOSS_PCT);
                double takeProfit = currentPrice * (1 - TAKE_PROFIT_PCT);
                // Emit only if on correct EMA side (redundant safety)
                if (currentPrice <= emaValue) {
                    signals.add(new TradeSignal("SELL", currentPrice, stopLoss, takeProfit,
                        String.format("Bearish Pattern (Below EMA 200, Trendline Breakout, Trend: %.2f%%)", (emaValue - currentPrice) / emaValue * 100)));
                }
            }
        }
        return signals;
    }

    private double getCurrentMarketPrice(String symbol) {
        try {
            long now = System.currentTimeMillis() / 1000;
            String url = "https://api.india.delta.exchange/v2/history/candles?resolution=1m&symbol=" + symbol + 
                        "&start=" + (now - 300) + 
                        "&end=" + now;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode rootNode = objectMapper.readTree(response.body());
                JsonNode resultNode = rootNode.get("result");
                
                if (resultNode != null && resultNode.isArray() && resultNode.size() > 0) {
                    JsonNode lastCandle = resultNode.get(resultNode.size() - 1);
                    JsonNode closeNode = lastCandle.get("close");
                    
                    if (closeNode != null && closeNode.isNumber()) {
                        return closeNode.asDouble();
                    }
                }
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    public void calculateEMA(List<Candle> candles, int period) {
        if (candles.size() < period) return;
        
        double multiplier = 2.0 / (period + 1);
        
        // Calculate initial SMA for first EMA value
        double sum = 0;
        for (int i = 0; i < period; i++) {
            sum += candles.get(i).close;
        }
        candles.get(period - 1).ema200 = sum / period;
        
        // Calculate EMA for remaining candles
        for (int i = period; i < candles.size(); i++) {
            double ema = (candles.get(i).close - candles.get(i - 1).ema200) * multiplier + candles.get(i - 1).ema200;
            candles.get(i).ema200 = ema;
        }
    }
    
    public List<SwingPoint>[] findSwingPoints(List<Candle> candles) {
        List<SwingPoint> highs = new ArrayList<>();
        List<SwingPoint> lows = new ArrayList<>();
        
        for (int i = MIN_SWING_SEPARATION; i < candles.size() - MIN_SWING_SEPARATION; i++) {
            // Check for swing high
            boolean isHigh = true;
            for (int j = 1; j <= MIN_SWING_SEPARATION; j++) {
                if (candles.get(i).high <= candles.get(i - j).high || 
                    candles.get(i).high <= candles.get(i + j).high) {
                    isHigh = false;
                    break;
                }
            }
            if (isHigh) {
                highs.add(new SwingPoint(i, candles.get(i).high));
                candles.get(i).swingHigh = candles.get(i).high;
            }
            
            // Check for swing low
            boolean isLow = true;
            for (int j = 1; j <= MIN_SWING_SEPARATION; j++) {
                if (candles.get(i).low >= candles.get(i - j).low || 
                    candles.get(i).low >= candles.get(i + j).low) {
                    isLow = false;
                    break;
                }
            }
            if (isLow) {
                lows.add(new SwingPoint(i, candles.get(i).low));
                candles.get(i).swingLow = candles.get(i).low;
            }
        }
        
        return new List[]{highs, lows};
    }
    
    public TrendLine fitTrendline(List<SwingPoint> points) {
        if (points.size() < 2) return null;
        
        SimpleRegression regression = new SimpleRegression();
        for (SwingPoint point : points) {
            regression.addData(point.index, point.price);
        }
        
        return new TrendLine(regression.getSlope(), regression.getIntercept());
    }

    /**
     * Return last EMA-200 value computed for the given symbol, if available.
     * Requires that candle data has been added; will compute EMA if missing.
     */
    public Double getLastEma200(String symbol) {
        List<Candle> candles = candlesData.get(symbol);
        if (candles == null || candles.size() < EMA_PERIOD) {
            return null;
        }
        // Ensure EMA is populated
        if (candles.get(candles.size() - 1).ema200 == 0.0) {
            calculateEMA(candles, EMA_PERIOD);
        }
        return candles.get(candles.size() - 1).ema200;
    }
}