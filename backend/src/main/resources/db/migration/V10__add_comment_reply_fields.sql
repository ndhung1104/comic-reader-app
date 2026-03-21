ALTER TABLE comments
ADD COLUMN IF NOT EXISTS parent_comment_id BIGINT;

ALTER TABLE comments
ADD COLUMN IF NOT EXISTS root_comment_id BIGINT;

ALTER TABLE comments
ADD COLUMN IF NOT EXISTS depth INT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_comments_parent_comment_id ON comments(parent_comment_id);
CREATE INDEX IF NOT EXISTS idx_comments_root_comment_id ON comments(root_comment_id);
CREATE INDEX IF NOT EXISTS idx_comments_comic_created_at ON comments(comic_id, created_at DESC);

DO $$
BEGIN
	IF NOT EXISTS (
		SELECT 1 FROM pg_constraint WHERE conname = 'fk_comments_parent_comment'
	) THEN
		ALTER TABLE comments
			ADD CONSTRAINT fk_comments_parent_comment
			FOREIGN KEY (parent_comment_id)
			REFERENCES comments(id)
			ON DELETE CASCADE;
	END IF;

	IF NOT EXISTS (
		SELECT 1 FROM pg_constraint WHERE conname = 'fk_comments_root_comment'
	) THEN
		ALTER TABLE comments
			ADD CONSTRAINT fk_comments_root_comment
			FOREIGN KEY (root_comment_id)
			REFERENCES comments(id)
			ON DELETE CASCADE;
	END IF;
END $$;
