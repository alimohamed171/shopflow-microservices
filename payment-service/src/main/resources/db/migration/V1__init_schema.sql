-- V1__init_schema.sql
-- ShopFlow Payment Service — Initial Schema
-- Flyway will run this automatically on startup.

-- Example: uncomment when you are ready to build the wallet/payment module
-- CREATE TABLE wallets (
--     id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
--     user_id    BIGINT      UNIQUE NOT NULL,
--     balance    NUMERIC(12,2) NOT NULL DEFAULT 0.00,
--     currency   VARCHAR(3)  DEFAULT 'USD',
--     updated_at TIMESTAMPTZ DEFAULT NOW()
-- );

SELECT 1; -- keeps Flyway happy until real migrations are added
