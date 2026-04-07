CREATE TABLE IF NOT EXISTS chapter_page_tts_audios (
    id BIGSERIAL PRIMARY KEY,
    chapter_id BIGINT NOT NULL,
    page_number INTEGER NOT NULL,
    lang VARCHAR(32) NOT NULL,
    voice VARCHAR(128) NOT NULL,
    speed DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    audio_path TEXT NOT NULL,
    duration_ms INTEGER,
    source_text_hash VARCHAR(128),
    source_ocr_job_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chapter_page_tts_audios_chapter
        FOREIGN KEY (chapter_id) REFERENCES chapters(id) ON DELETE CASCADE,
    CONSTRAINT fk_chapter_page_tts_audios_ocr_job
        FOREIGN KEY (source_ocr_job_id) REFERENCES translation_jobs(id) ON DELETE SET NULL,
    CONSTRAINT uq_chapter_page_tts_audio
        UNIQUE (chapter_id, page_number, lang, voice, speed)
);

CREATE INDEX IF NOT EXISTS idx_chapter_page_tts_audios_chapter
    ON chapter_page_tts_audios(chapter_id);
CREATE INDEX IF NOT EXISTS idx_chapter_page_tts_audios_ocr_job
    ON chapter_page_tts_audios(source_ocr_job_id);
