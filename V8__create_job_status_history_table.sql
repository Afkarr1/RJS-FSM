-- Enable UUID generator (aman di Postgres)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE job_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL,
    from_status VARCHAR(30) NOT NULL,
    to_status VARCHAR(30) NOT NULL,
    changed_by UUID NULL,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_job_status_history_job
        FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE,

    CONSTRAINT fk_job_status_history_user
        FOREIGN KEY (changed_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_job_status_history_job_id ON job_status_history(job_id);
CREATE INDEX idx_job_status_history_changed_at ON job_status_history(changed_at);