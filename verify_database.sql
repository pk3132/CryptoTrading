-- Database Verification Script
-- Run this script in your MySQL database to verify the crypto_strategy2 table

USE tradingbot;

-- Check if the table exists
SHOW TABLES LIKE 'crypto_strategy2';

-- Show table structure
DESCRIBE crypto_strategy2;

-- Check current data in the table
SELECT * FROM crypto_strategy2;

-- Count records
SELECT COUNT(*) as total_records FROM crypto_strategy2;

-- Check for any existing BTCUSD trades
SELECT * FROM crypto_strategy2 WHERE symbol = 'BTCUSD';

-- If you want to clean up test data, uncomment the line below:
-- DELETE FROM crypto_strategy2 WHERE entry_reason LIKE '%test%' OR entry_reason LIKE '%Test%';
