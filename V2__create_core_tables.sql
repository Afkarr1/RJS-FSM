-- =========================================================
-- RJS-FSM V2 CREATE CORE TABLES (PostgreSQL 16) - UUID schema
-- =========================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_role') THEN
    CREATE TYPE user_role AS ENUM ('ADMIN', 'TECHNICIAN');
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'job_status') THEN
    CREATE TYPE job_status AS ENUM ('UPCOMING', 'IN_PROGRESS', 'DONE', 'BLOCKED');
  END IF;
END$$;

CREATE TABLE IF NOT EXISTS users (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  username        VARCHAR(80) NOT NULL UNIQUE,
  password_hash   VARCHAR(255) NOT NULL,
  role            user_role NOT NULL,
  full_name       VARCHAR(120) NOT NULL,
  phone_e164      VARCHAR(20),
  is_active       BOOLEAN NOT NULL DEFAULT TRUE,
  created_at      TIMESTAMP NOT NULL DEFAULT now(),
  updated_at      TIMESTAMP NOT NULL DEFAULT now(),
  last_login_at   TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

CREATE TABLE IF NOT EXISTS user_admin_actions (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  target_user_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  actor_admin_id  UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  action_type     VARCHAR(50) NOT NULL,
  note            TEXT,
  created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_user_admin_actions_target ON user_admin_actions(target_user_id);
CREATE INDEX IF NOT EXISTS idx_user_admin_actions_actor  ON user_admin_actions(actor_admin_id);

CREATE TABLE IF NOT EXISTS customers (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name            VARCHAR(150) NOT NULL,
  address         TEXT NOT NULL,
  phone_e164      VARCHAR(20) NOT NULL,
  notes           TEXT,
  created_at      TIMESTAMP NOT NULL DEFAULT now(),
  updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_customers_phone ON customers(phone_e164);

CREATE TABLE IF NOT EXISTS jobs (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  customer_id        UUID NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
  assigned_to        UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  created_by         UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  scheduled_at       TIMESTAMP NOT NULL,
  status             job_status NOT NULL DEFAULT 'UPCOMING',
  address_override   TEXT,
  customer_phone     VARCHAR(20),
  issue_summary      VARCHAR(255),
  issue_details      TEXT,
  unresolved_reason  TEXT,
  done_at            TIMESTAMP,
  requires_photo     BOOLEAN NOT NULL DEFAULT TRUE,
  photo_uploaded     BOOLEAN NOT NULL DEFAULT FALSE,
  last_photo_at      TIMESTAMP,
  last_reminder_at   TIMESTAMP,
  created_at         TIMESTAMP NOT NULL DEFAULT now(),
  updated_at         TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_jobs_assigned_to   ON jobs(assigned_to);
CREATE INDEX IF NOT EXISTS idx_jobs_customer_id   ON jobs(customer_id);
CREATE INDEX IF NOT EXISTS idx_jobs_scheduled_at  ON jobs(scheduled_at);
CREATE INDEX IF NOT EXISTS idx_jobs_status        ON jobs(status);

CREATE TABLE IF NOT EXISTS job_photos (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  job_id        UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
  file_url      TEXT NOT NULL,
  file_name     VARCHAR(255),
  mime_type     VARCHAR(100),
  size_bytes    BIGINT,
  uploaded_by   UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  uploaded_at   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_job_photos_job_id ON job_photos(job_id);

CREATE TABLE IF NOT EXISTS job_review_links (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  job_id          UUID NOT NULL UNIQUE REFERENCES jobs(id) ON DELETE CASCADE,
  token           VARCHAR(120) NOT NULL UNIQUE,
  expires_at      TIMESTAMP NOT NULL,
  used_at         TIMESTAMP,
  created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_job_review_links_token ON job_review_links(token);
CREATE INDEX IF NOT EXISTS idx_job_review_links_exp   ON job_review_links(expires_at);

CREATE TABLE IF NOT EXISTS job_reviews (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  job_id          UUID NOT NULL UNIQUE REFERENCES jobs(id) ON DELETE CASCADE,
  rating          INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
  note            TEXT,
  photo_url       TEXT,
  created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS audit_logs (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  actor_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  action        VARCHAR(80) NOT NULL,
  entity_type   VARCHAR(50) NOT NULL,
  entity_id     UUID,
  detail        JSONB,
  created_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_actor ON audit_logs(actor_user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs(entity_type, entity_id);

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_users_updated_at ON users;
CREATE TRIGGER trg_users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_customers_updated_at ON customers;
CREATE TRIGGER trg_customers_updated_at
BEFORE UPDATE ON customers
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_jobs_updated_at ON jobs;
CREATE TRIGGER trg_jobs_updated_at
BEFORE UPDATE ON jobs
FOR EACH ROW EXECUTE FUNCTION set_updated_at();
