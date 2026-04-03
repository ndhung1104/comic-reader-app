CREATE TABLE IF NOT EXISTS translation_jobs (
    id BIGSERIAL PRIMARY KEY,
    chapter_id BIGINT NOT NULL,
    external_job_id VARCHAR(255),
    status VARCHAR(32) NOT NULL,
    source_lang VARCHAR(32) NOT NULL,
    target_lang VARCHAR(32),
    error_message TEXT,
    ocr_persisted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    requester_user_id BIGINT,
    CONSTRAINT fk_translation_jobs_chapter
        FOREIGN KEY (chapter_id) REFERENCES chapters(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_translation_jobs_chapter_id ON translation_jobs(chapter_id);
CREATE INDEX IF NOT EXISTS idx_translation_jobs_external_job_id ON translation_jobs(external_job_id);
CREATE INDEX IF NOT EXISTS idx_translation_jobs_status ON translation_jobs(status);

CREATE TABLE IF NOT EXISTS chapter_page_ocr_texts (
    id BIGSERIAL PRIMARY KEY,
    chapter_id BIGINT NOT NULL,
    page_number INTEGER NOT NULL,
    source_lang VARCHAR(32) NOT NULL,
    ocr_text TEXT NOT NULL,
    ocr_job_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chapter_page_ocr_texts_chapter
        FOREIGN KEY (chapter_id) REFERENCES chapters(id) ON DELETE CASCADE,
    CONSTRAINT fk_chapter_page_ocr_texts_job
        FOREIGN KEY (ocr_job_id) REFERENCES translation_jobs(id) ON DELETE SET NULL,
    CONSTRAINT uq_chapter_page_ocr_text UNIQUE (chapter_id, page_number, source_lang)
);

CREATE INDEX IF NOT EXISTS idx_chapter_page_ocr_texts_chapter_id ON chapter_page_ocr_texts(chapter_id);
CREATE INDEX IF NOT EXISTS idx_chapter_page_ocr_texts_job_id ON chapter_page_ocr_texts(ocr_job_id);
