-- ─────────────────────────────────────────────────────────────────────────────
-- V1__init_schema.sql
-- Payment Service — full initial schema
-- PostgreSQL 15
-- ─────────────────────────────────────────────────────────────────────────────


-- ── 1. WALLETS ────────────────────────────────────────────────────────────────
-- One wallet per user. user_id mirrors users.id from Core Service.
-- We do NOT use a foreign key here — services don't share databases.
CREATE TABLE wallets (
                         id         UUID           NOT NULL DEFAULT gen_random_uuid(),
                         user_id    BIGINT         NOT NULL,
                         balance    NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
                         currency   VARCHAR(3)     NOT NULL DEFAULT 'USD',
                         updated_at TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

                         PRIMARY KEY (id),
                         UNIQUE (user_id),
                         CONSTRAINT chk_wallets_balance CHECK (balance >= 0)
);


-- ── 2. PAYMENTS ───────────────────────────────────────────────────────────────
-- One payment record per order payment attempt.
-- order_id mirrors orders.id from Core Service.
CREATE TABLE payments (
                          id             UUID           NOT NULL DEFAULT gen_random_uuid(),
                          order_id       BIGINT         NOT NULL,    -- mirrors orders.id in Core Service
                          user_id        BIGINT         NOT NULL,    -- mirrors users.id  in Core Service
                          amount         NUMERIC(10, 2) NOT NULL,
                          currency       VARCHAR(3)     NOT NULL DEFAULT 'USD',
                          method         VARCHAR(50)    NOT NULL,    -- 'WALLET', 'CARD_SIM', 'COD'
                          status         VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
                          failure_reason TEXT,
                          created_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
                          updated_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

                          PRIMARY KEY (id),
                          UNIQUE (order_id),                         -- one payment attempt per order
                          CONSTRAINT chk_payments_amount CHECK (amount > 0),
                          CONSTRAINT chk_payments_status CHECK (
                              status IN ('PENDING','COMPLETED','FAILED','REFUNDED')
                              )
);

CREATE INDEX idx_payments_user_id  ON payments (user_id);
CREATE INDEX idx_payments_status   ON payments (status);
CREATE INDEX idx_payments_order_id ON payments (order_id);


-- ── 3. TRANSACTIONS ───────────────────────────────────────────────────────────
-- Immutable ledger. Every money movement (debit, credit, refund) creates a row.
-- balance_after is a snapshot so you can audit the wallet history.
CREATE TABLE transactions (
                              id            UUID           NOT NULL DEFAULT gen_random_uuid(),
                              wallet_id     UUID           NOT NULL,
                              payment_id    UUID,                        -- NULL for manual top-ups
                              type          VARCHAR(20)    NOT NULL,     -- 'CREDIT', 'DEBIT', 'REFUND'
                              amount        NUMERIC(10, 2) NOT NULL,
                              balance_after NUMERIC(12, 2) NOT NULL,
                              description   TEXT,
                              created_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

                              PRIMARY KEY (id),
                              CONSTRAINT chk_transactions_amount CHECK (amount > 0),
                              CONSTRAINT chk_transactions_type   CHECK (type IN ('CREDIT','DEBIT','REFUND')),
                              CONSTRAINT fk_transactions_wallet  FOREIGN KEY (wallet_id)  REFERENCES wallets  (id),
                              CONSTRAINT fk_transactions_payment FOREIGN KEY (payment_id) REFERENCES payments (id)
);

CREATE INDEX idx_transactions_wallet_id  ON transactions (wallet_id);
CREATE INDEX idx_transactions_payment_id ON transactions (payment_id);
CREATE INDEX idx_transactions_created_at ON transactions (created_at DESC);


-- ── 4. REFUNDS ────────────────────────────────────────────────────────────────
CREATE TABLE refunds (
                         id         UUID           NOT NULL DEFAULT gen_random_uuid(),
                         payment_id UUID           NOT NULL,
                         amount     NUMERIC(10, 2) NOT NULL,
                         reason     TEXT,
                         status     VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
                         created_at TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
                         updated_at TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

                         PRIMARY KEY (id),
                         CONSTRAINT chk_refunds_amount CHECK (amount > 0),
                         CONSTRAINT chk_refunds_status CHECK (status IN ('PENDING','COMPLETED','REJECTED')),
                         CONSTRAINT fk_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments (id)
);

CREATE INDEX idx_refunds_payment_id ON refunds (payment_id);
