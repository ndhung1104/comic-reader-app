-- =============================================
-- V11: Library schema
-- =============================================

CREATE TABLE IF NOT EXISTS followed_comics (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    comic_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_followed_comics_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_followed_comics_comic FOREIGN KEY (comic_id) REFERENCES comics(id) ON DELETE CASCADE,
    CONSTRAINT uq_followed_comics_user_comic UNIQUE (user_id, comic_id)
);

CREATE TABLE IF NOT EXISTS reading_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    comic_id BIGINT NOT NULL,
    chapter_id BIGINT NOT NULL,
    page_number INTEGER,
    last_read_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reading_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_reading_history_comic FOREIGN KEY (comic_id) REFERENCES comics(id) ON DELETE CASCADE,
    CONSTRAINT fk_reading_history_chapter FOREIGN KEY (chapter_id) REFERENCES chapters(id) ON DELETE CASCADE,
    CONSTRAINT uq_reading_history_user_comic UNIQUE (user_id, comic_id)
);

CREATE INDEX IF NOT EXISTS idx_followed_comics_user_id ON followed_comics(user_id);
CREATE INDEX IF NOT EXISTS idx_followed_comics_created_at ON followed_comics(created_at);
CREATE INDEX IF NOT EXISTS idx_reading_history_user_id ON reading_history(user_id);
CREATE INDEX IF NOT EXISTS idx_reading_history_last_read_at ON reading_history(last_read_at);
