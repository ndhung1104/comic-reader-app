-- =============================================
-- V3: Wallet & Finance Schema
-- =============================================

-- User wallets (one per user)
CREATE TABLE IF NOT EXISTS user_wallets (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL UNIQUE,
    coin_balance INTEGER        NOT NULL DEFAULT 0,
    point_balance INTEGER       NOT NULL DEFAULT 0,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Transaction ledger
CREATE TABLE IF NOT EXISTS wallet_transactions (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    type            VARCHAR(50)     NOT NULL,
    amount          INTEGER         NOT NULL,
    currency        VARCHAR(10)     NOT NULL DEFAULT 'COIN',
    balance_after   INTEGER         NOT NULL,
    description     VARCHAR(500),
    reference_id    VARCHAR(255),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transaction_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Chapter purchase records
CREATE TABLE IF NOT EXISTS chapter_purchases (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    chapter_id      BIGINT          NOT NULL,
    price_paid      INTEGER         NOT NULL,
    currency        VARCHAR(10)     NOT NULL DEFAULT 'COIN',
    purchased_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_purchase_user    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_purchase_chapter FOREIGN KEY (chapter_id) REFERENCES chapters(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_chapter     UNIQUE (user_id, chapter_id)
);

-- VIP subscriptions
CREATE TABLE IF NOT EXISTS vip_subscriptions (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL,
    plan        VARCHAR(50)     NOT NULL DEFAULT 'MONTHLY',
    start_date  TIMESTAMP       NOT NULL,
    end_date    TIMESTAMP       NOT NULL,
    status      VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vip_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_wallet_user_id       ON user_wallets(user_id);
CREATE INDEX IF NOT EXISTS idx_transaction_user_id   ON wallet_transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_transaction_created   ON wallet_transactions(created_at);
CREATE INDEX IF NOT EXISTS idx_purchase_user_id      ON chapter_purchases(user_id);
CREATE INDEX IF NOT EXISTS idx_purchase_chapter_id   ON chapter_purchases(chapter_id);
CREATE INDEX IF NOT EXISTS idx_vip_user_id           ON vip_subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_vip_end_date          ON vip_subscriptions(end_date);
