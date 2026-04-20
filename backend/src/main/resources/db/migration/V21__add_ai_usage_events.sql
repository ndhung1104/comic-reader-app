CREATE TABLE IF NOT EXISTS ai_usage_events (
    id BIGSERIAL PRIMARY KEY,
    requester_user_id BIGINT,
    actor_key VARCHAR(100) NOT NULL,
    feature VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    provider VARCHAR(50),
    model VARCHAR(100),
    reference_id VARCHAR(255),
    request_units INTEGER,
    response_units INTEGER,
    details VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_usage_event_user FOREIGN KEY (requester_user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_ai_usage_actor_feature_created
    ON ai_usage_events(actor_key, feature, created_at);

CREATE INDEX IF NOT EXISTS idx_ai_usage_feature_created
    ON ai_usage_events(feature, created_at);
