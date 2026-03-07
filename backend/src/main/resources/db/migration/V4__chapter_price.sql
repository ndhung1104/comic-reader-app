-- =============================================
-- V4: Add price column to chapters
-- =============================================

ALTER TABLE chapters ADD COLUMN price INTEGER NOT NULL DEFAULT 0;
