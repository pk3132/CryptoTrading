-- Test SQL script to verify Strategy 2 data insertion
-- Run this script in your MySQL database to test the crypto_strategy2 table

USE tradingbot;

-- First, let's check the table structure
DESCRIBE crypto_strategy2;

-- Insert test data for Strategy 2
INSERT INTO crypto_strategy2 (
    symbol, 
    trade_type, 
    entry_price, 
    stop_loss, 
    take_profit, 
    quantity, 
    leverage, 
    risk_reward_ratio, 
    timeframe, 
    strategy_name, 
    entry_reason, 
    status, 
    entry_time, 
    ema9_value, 
    ema20_value, 
    trend_direction, 
    market_condition, 
    created_at
) VALUES (
    'BTCUSD',
    'BUY',
    65000.00,
    62000.00,
    71000.00,
    0.1,
    25.0,
    3.0,
    '1h',
    'Strategy 2 - 9/20 EMA Crossover',
    'Manual test insertion - EMA 9 crossed above EMA 20',
    'OPEN',
    NOW(),
    64800.00,
    64200.00,
    'BULLISH',
    'TRENDING',
    NOW()
);

-- Insert another test record for ETHUSD
INSERT INTO crypto_strategy2 (
    symbol, 
    trade_type, 
    entry_price, 
    stop_loss, 
    take_profit, 
    quantity, 
    leverage, 
    risk_reward_ratio, 
    timeframe, 
    strategy_name, 
    entry_reason, 
    status, 
    entry_time, 
    ema9_value, 
    ema20_value, 
    trend_direction, 
    market_condition, 
    created_at
) VALUES (
    'ETHUSD',
    'SELL',
    3200.00,
    3350.00,
    2900.00,
    1.0,
    25.0,
    3.0,
    '1h',
    'Strategy 2 - 9/20 EMA Crossover',
    'Manual test insertion - EMA 9 crossed below EMA 20',
    'OPEN',
    NOW(),
    3180.00,
    3250.00,
    'BEARISH',
    'TRENDING',
    NOW()
);

-- Check the inserted data
SELECT * FROM crypto_strategy2;

-- Count total records
SELECT COUNT(*) as total_records FROM crypto_strategy2;

-- Check records by status
SELECT status, COUNT(*) as count FROM crypto_strategy2 GROUP BY status;

-- Check records by symbol
SELECT symbol, COUNT(*) as count FROM crypto_strategy2 GROUP BY symbol;
