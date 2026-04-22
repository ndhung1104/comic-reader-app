-- Create table for users requesting Creator role
CREATE TABLE
IF NOT EXISTS creator_requests
(
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    message TEXT,
    status VARCHAR
(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    processed_by BIGINT,
    admin_message TEXT,
    CONSTRAINT fk_creator_requests_user FOREIGN KEY
(user_id) REFERENCES users
(id) ON
DELETE CASCADE,
    CONSTRAINT fk_creator_requests_processed_by FOREIGN KEY
(processed_by) REFERENCES users
(id) ON
DELETE
SET NULL
);

CREATE INDEX
IF NOT EXISTS idx_creator_requests_status ON creator_requests
(status);
