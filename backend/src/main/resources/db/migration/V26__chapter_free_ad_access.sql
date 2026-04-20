CREATE TABLE IF NOT EXISTS chapter_free_ad_accesses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    chapter_id BIGINT NOT NULL,
    source VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chapter_free_ad_access_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_chapter_free_ad_access_chapter FOREIGN KEY (chapter_id) REFERENCES chapters(id) ON DELETE CASCADE,
    CONSTRAINT uq_chapter_free_ad_access_user_chapter UNIQUE (user_id, chapter_id)
);

CREATE INDEX IF NOT EXISTS idx_chapter_free_ad_access_user_created
    ON chapter_free_ad_accesses(user_id, created_at);
