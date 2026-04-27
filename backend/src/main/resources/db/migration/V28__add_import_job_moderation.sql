ALTER TABLE import_jobs ADD COLUMN moderation_status VARCHAR(50) NOT NULL DEFAULT 'APPROVED';
ALTER TABLE import_jobs ADD COLUMN moderation_reason TEXT;
