-- Add CREATOR role, comics.creator_id FK, and import_jobs table
INSERT INTO roles
    (name)
VALUES
    ('CREATOR')
ON CONFLICT
(name) DO NOTHING;

-- Add creator_id to comics
ALTER TABLE comics ADD COLUMN
IF NOT EXISTS creator_id BIGINT;

-- Add FK constraint only when it does not already exist (Postgres does not support
-- `ALTER TABLE ... ADD CONSTRAINT IF NOT EXISTS`). Use a DO block to check
-- information_schema and add the constraint conditionally.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
    FROM information_schema.table_constraints
    WHERE constraint_name = 'fk_comics_creator' AND table_name = 'comics'
    ) THEN
    ALTER TABLE comics
            ADD CONSTRAINT fk_comics_creator
            FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE SET NULL;
END
IF;
END
$$;

-- Import jobs table to track manual imports started by users
CREATE TABLE
IF NOT EXISTS import_jobs
(
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    source VARCHAR
(255) NOT NULL,
    source_url TEXT,
    status VARCHAR
(50) NOT NULL,
    result_comic_id BIGINT,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_import_jobs_user FOREIGN KEY
(user_id) REFERENCES users
(id) ON
DELETE CASCADE,
    CONSTRAINT fk_import_jobs_result_comic FOREIGN KEY
(result_comic_id) REFERENCES comics
(id) ON
DELETE
SET NULL
);

CREATE INDEX
IF NOT EXISTS idx_import_jobs_user_id ON import_jobs
(user_id);
