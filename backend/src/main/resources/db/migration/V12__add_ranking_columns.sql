-- =============================================
-- V12: Add ranking/popularity columns to comics
-- =============================================

ALTER TABLE comics ADD COLUMN IF NOT EXISTS view_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE comics ADD COLUMN IF NOT EXISTS average_rating DOUBLE PRECISION NOT NULL DEFAULT 0;
ALTER TABLE comics ADD COLUMN IF NOT EXISTS follower_count INTEGER NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_comics_view_count ON comics(view_count DESC);
CREATE INDEX IF NOT EXISTS idx_comics_average_rating ON comics(average_rating DESC);
