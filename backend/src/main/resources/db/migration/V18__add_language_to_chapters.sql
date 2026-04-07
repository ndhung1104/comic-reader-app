ALTER TABLE chapters
    ADD COLUMN IF NOT EXISTS language VARCHAR(32);

UPDATE chapters
SET language = 'vi'
WHERE language IS NULL OR btrim(language) = '';

ALTER TABLE chapters
    ALTER COLUMN language SET DEFAULT 'vi';

ALTER TABLE chapters
    ALTER COLUMN language SET NOT NULL;
