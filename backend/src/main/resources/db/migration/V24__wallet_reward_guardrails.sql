ALTER TABLE wallet_transactions
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'SUCCEEDED';

CREATE UNIQUE INDEX IF NOT EXISTS uq_wallet_transaction_user_type_reference
    ON wallet_transactions(user_id, type, reference_id);

CREATE TABLE IF NOT EXISTS ad_reward_claims (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    reward_id VARCHAR(255) NOT NULL,
    placement VARCHAR(100) NOT NULL,
    reward_type VARCHAR(10) NOT NULL,
    reward_amount INTEGER NOT NULL,
    ad_provider VARCHAR(50),
    ad_unit_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ad_reward_claim_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_ad_reward_claim_user_reward UNIQUE (user_id, reward_id)
);

CREATE INDEX IF NOT EXISTS idx_ad_reward_claim_user_created
    ON ad_reward_claims(user_id, created_at);
