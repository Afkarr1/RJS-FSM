-- =========================================================
-- V2: Add review photos table for customer review uploads
-- =========================================================

CREATE TABLE review_photos (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID        NOT NULL REFERENCES tenants(id),
    review_id   UUID        NOT NULL REFERENCES job_reviews(id) ON DELETE CASCADE,
    job_id      UUID        NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    file_path   VARCHAR(500) NOT NULL,
    file_name   VARCHAR(255) NOT NULL,
    mime_type   VARCHAR(100),
    size_bytes  BIGINT,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_review_photos_review ON review_photos(review_id);
CREATE INDEX idx_review_photos_job ON review_photos(job_id);

-- Enforce max 1000 characters on review note
ALTER TABLE job_reviews ADD CONSTRAINT chk_review_note_length CHECK (length(note) <= 1000);
