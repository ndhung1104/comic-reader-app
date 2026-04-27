CREATE TABLE ai_summaries (
    id BIGSERIAL PRIMARY KEY,
    comic_id BIGINT NOT NULL,
    chapter_id BIGINT,
    content TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    moderation_reason TEXT,
    creator_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_summaries_comic FOREIGN KEY (comic_id) REFERENCES comics(id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_summaries_chapter FOREIGN KEY (chapter_id) REFERENCES chapters(id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_summaries_creator FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_ai_summaries_comic ON ai_summaries(comic_id);
CREATE INDEX idx_ai_summaries_chapter ON ai_summaries(chapter_id);
