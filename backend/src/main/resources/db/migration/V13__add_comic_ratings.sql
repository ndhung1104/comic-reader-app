-- =============================================
-- V13: User rating table for comics
-- =============================================

CREATE TABLE IF NOT EXISTS comic_ratings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    comic_id BIGINT NOT NULL,
    score INTEGER NOT NULL CHECK (score >= 1 AND score <= 5),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comic_ratings_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_comic_ratings_comic FOREIGN KEY (comic_id) REFERENCES comics(id) ON DELETE CASCADE,
    CONSTRAINT uq_comic_ratings_user_comic UNIQUE (user_id, comic_id)
);

CREATE INDEX IF NOT EXISTS idx_comic_ratings_comic_id ON comic_ratings(comic_id);
