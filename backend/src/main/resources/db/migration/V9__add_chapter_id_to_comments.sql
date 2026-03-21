ALTER TABLE comments
ADD COLUMN IF NOT EXISTS chapter_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_comments_chapter_id ON comments(chapter_id);

DO $$
BEGIN
	IF NOT EXISTS (
		SELECT 1
		FROM pg_constraint
		WHERE conname = 'fk_comments_chapter'
	) THEN
		ALTER TABLE comments
			ADD CONSTRAINT fk_comments_chapter
			FOREIGN KEY (chapter_id)
			REFERENCES chapters(id)
			ON DELETE SET NULL;
	END IF;
END $$;
