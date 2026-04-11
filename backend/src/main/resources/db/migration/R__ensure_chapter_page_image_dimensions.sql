-- Keep legacy local/docker databases compatible after the image dimensions change
-- moved around during development. Fresh databases already get these columns from V12.
ALTER TABLE chapter_pages
    ADD COLUMN IF NOT EXISTS image_width INTEGER,
    ADD COLUMN IF NOT EXISTS image_height INTEGER;
