# PositionChecker Service - Complete Guide

## Overview
The `PositionChecker` service is a reusable class that checks for existing open positions on Delta Exchange before placing new orders. This prevents duplicate orders and ensures proper risk management.

## Features
- ✅ Check for existing positions by symbol
- ✅ Get detailed position information
- ✅ Print position summaries
- ✅ Prevent duplicate orders
- ✅ Spring Service integration
- ✅ Proper API signature handling
- ✅ Error handling and logging

## Quick Start

### 1. Basic Usage
```java
@Autowired
private PositionChecker positionChecker;

// Check if symbol has open position
boolean hasPosition = positionChecker.hasOpenPosition("ETHUSD");

// Get detailed position info
PositionChecker.PositionInfo position = positionChecker.getPositionForSymbol("ETHUSD");

// Print all open positions
positionChecker.printOpenPositions();
```

### 2. In Trade Execution Logic
```java
public boolean executeTrade(Trade trade) {
    // Check for existing position before placing order
    if (positionChecker.hasOpenPosition(trade.getSymbol())) {
        logger.warn("⚠️ DUPLICATE ORDER PREVENTION: Open position exists for {}", trade.getSymbol());
        return false; // Block duplicate order
    }
    
    // Proceed with trade execution
    return placeOrder(trade);
}
```

## API Methods

### `hasOpenPosition(String symbol)`
**Purpose**: Check if a symbol has an open position
**Parameters**: 
- `symbol`: Trading symbol (e.g., "BTCUSD", "ETHUSD")
**Returns**: `boolean` - true if position exists, false otherwise

**Example**:
```java
boolean hasBtcPosition = positionChecker.hasOpenPosition("BTCUSD");
boolean hasEthPosition = positionChecker.hasOpenPosition("ETHUSD");
```

### `getPositionForSymbol(String symbol)`
**Purpose**: Get detailed position information for a symbol
**Parameters**:
- `symbol`: Trading symbol
**Returns**: `PositionInfo` object with position details, or null if no position

**Example**:
```java
PositionChecker.PositionInfo position = positionChecker.getPositionForSymbol("ETHUSD");
if (position != null) {
    logger.info("Position: {} {} at ${}", position.side, position.size, position.entryPrice);
}
```

### `printOpenPositions()`
**Purpose**: Print a summary of all open positions
**Returns**: void (logs to console)

**Example**:
```java
positionChecker.printOpenPositions();
```

### `getOpenPositions()`
**Purpose**: Get list of all open positions
**Returns**: `List<PositionInfo>` - List of all open positions

**Example**:
```java
List<PositionChecker.PositionInfo> positions = positionChecker.getOpenPositions();
for (PositionInfo position : positions) {
    logger.info("Position: {}", position.toString());
}
```

## PositionInfo Class
```java
public static class PositionInfo {
    public String symbol;        // Trading symbol
    public String side;          // "buy" or "sell"
    public double size;          // Position size
    public double entryPrice;    // Entry price
    public double markPrice;     // Current mark price
    public double unrealizedPnl; // Unrealized P&L
    public double realizedPnl;   // Realized P&L
    public int productId;        // Product ID
}
```

## Integration Examples

### 1. In LiveTradeExecutor
```java
@Service
public class LiveTradeExecutor {
    
    @Autowired
    private PositionChecker positionChecker;
    
    public boolean executeTrade(Trade trade) {
        // Check for existing position
        if (positionChecker.hasOpenPosition(trade.getSymbol())) {
            PositionChecker.PositionInfo existingPos = positionChecker.getPositionForSymbol(trade.getSymbol());
            logger.warn("⚠️ DUPLICATE ORDER PREVENTION: Open position already exists for {}", trade.getSymbol());
            logger.warn("   Existing Position: {} {} contracts at ${}", 
                       existingPos.side, existingPos.size, existingPos.entryPrice);
            return false;
        }
        
        // Execute trade...
        return true;
    }
}
```

### 2. In Strategy Service
```java
@Service
public class TradingStrategy {
    
    @Autowired
    private PositionChecker positionChecker;
    
    public void checkAndExecuteSignal(String symbol, String side) {
        if (positionChecker.hasOpenPosition(symbol) && side.equals("BUY")) {
            logger.info("Skipping {} signal for {} - position already exists", side, symbol);
            return;
        }
        
        // Execute signal...
    }
}
```

### 3. In Monitoring Service
```java
@Service
public class PositionMonitor {
    
    @Autowired
    private PositionChecker positionChecker;
    
    @Scheduled(fixedRate = 60000) // Every minute
    public void monitorPositions() {
        positionChecker.printOpenPositions();
    }
}
```

## Error Handling
The PositionChecker includes comprehensive error handling:
- API connection errors
- Signature validation errors
- Invalid symbol errors
- Network timeouts

All errors are logged and the service returns safe defaults (assumes no position on error).

## Configuration
The service uses the same API credentials as other Delta Exchange services:
- API Key: From application.properties
- API Secret: From application.properties
- Base URL: https://api.india.delta.exchange

## Testing
Run the example to test functionality:
```bash
java -cp "target/classes;target/dependency/*" com.tradingbot.example.PositionCheckerExample
```

## Best Practices

### 1. Always Check Before Trading
```java
if (!positionChecker.hasOpenPosition(symbol)) {
    // Safe to place new order
    placeOrder(trade);
}
```

### 2. Handle Different Order Types
```java
boolean hasPosition = positionChecker.hasOpenPosition(symbol);

if (hasPosition && side.equals("BUY")) {
    // Block duplicate BUY
    return false;
} else if (hasPosition && side.equals("SELL")) {
    // Allow SELL to close position
    return true;
} else {
    // No position, allow any order
    return true;
}
```

### 3. Log Position Details
```java
PositionChecker.PositionInfo position = positionChecker.getPositionForSymbol(symbol);
if (position != null) {
    logger.info("Existing position: {} {} at ${}", 
                position.side, position.size, position.entryPrice);
}
```

## Troubleshooting

### Common Issues

1. **"Unknown symbol" error**
   - Solution: Use "BTCUSD" or "ETHUSD" (case sensitive)

2. **API connection errors**
   - Solution: Check internet connection and API credentials

3. **Position not detected**
   - Solution: Verify the position exists on Delta Exchange dashboard

### Debug Mode
Enable debug logging to see API requests:
```properties
logging.level.com.tradingbot.service.PositionChecker=DEBUG
```

## Security Notes
- Uses HMAC-SHA256 signature authentication
- API credentials are stored securely in application.properties
- All API calls are logged for audit purposes

## Performance
- API calls are cached for 5 seconds (signature validity)
- Lightweight service with minimal memory footprint
- Designed for high-frequency trading scenarios

---

## Quick Reference Card

| Method | Purpose | Returns |
|--------|---------|---------|
| `hasOpenPosition(symbol)` | Check if position exists | boolean |
| `getPositionForSymbol(symbol)` | Get position details | PositionInfo |
| `printOpenPositions()` | Print all positions | void |
| `getOpenPositions()` | Get all positions | List<PositionInfo> |

**Supported Symbols**: BTCUSD, ETHUSD
**API Endpoint**: /v2/positions?underlying_asset_symbol={ASSET}
**Authentication**: HMAC-SHA256 with API key/secret
