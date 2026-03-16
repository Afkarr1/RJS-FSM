-- =========================================================
-- RJS-FSM V1 INIT (PostgreSQL 16) - UUID based schema
-- =========================================================

-- UUID generator (pgcrypto)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================================================
-- ENUMS
-- =========================================================
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_role') THEN
    CREATE TYPE user_role AS ENUM ('ADMIN', 'TECHNICIAN');
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'job_status') THEN
    CREATE TYPE job_status AS ENUM ('UPCOMING', 'IN_PROGRESS', 'DONE', 'BLOCKED');
  END IF;
END$$;

-- =========================================================
-- USERS (2 admin + 13 teknisi)
-- - Login credentials only managed by admin (app rule)
-- - We still store hashed password, never plaintext.
-- =========================================================
CREATE TABLE IF NOT EXISTS users (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  username        VARCHAR(80) NOT NULL UNIQUE,   -- nama admin/teknisi (unique)
  password_hash   VARCHAR(255) NOT NULL,         -- BCrypt recommended
  role            user_role NOT NULL,
  full_name       VARCHAR(120) NOT NULL,
  phone_e164      VARCHAR(20),                   -- format +62...
  is_active       BOOLEAN NOT NULL DEFAULT TRUE,

  created_at      TIMESTAMP NOT NULL DEFAULT now(),
  updated_at      TIMESTAMP NOT NULL DEFAULT now(),
  last_login_at   TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

-- Track last admin who changed internal user data / login setup (history requirement)
CREATE TABLE IF NOT EXISTS user_admin_actions (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  target_user_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  actor_admin_id  UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  action_type     VARCHAR(50) NOT NULL, -- e.g. CREATE_USER, RESET_PASSWORD, DISABLE_USER
  note            TEXT,

  created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_user_admin_actions_target ON user_admin_actions(target_user_id);
CREATE INDEX IF NOT EXISTS idx_user_admin_actions_actor  ON user_admin_actions(actor_admin_id);

-- =========================================================
-- CUSTOMERS (CRM master)
-- - Admin pilih customer dari master data
-- - Phone normalized to +62
-- =========================================================
CREATE TABLE IF NOT EXISTS customers (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name            VARCHAR(150) NOT NULL,
  address         TEXT NOT NULL,
  phone_e164      VARCHAR(20) NOT NULL,        -- +62...
  notes           TEXT,

  created_at      TIMESTAMP NOT NULL DEFAULT now(),
  updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_customers_phone ON customers(phone_e164);

-- =========================================================
-- JOBS (work order)
-- - assigned technician only can see their jobs (app rule)
-- - DONE only when photo exists (enforced by app + helper columns)
-- =========================================================
CREATE TABLE IF NOT EXISTS jobs (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  customer_id        UUID NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
  assigned_to        UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT, -- technician
  created_by         UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT, -- admin1/admin2

  scheduled_at       TIMESTAMP NOT NULL, -- jadwal kunjungan
  status             job_status NOT NULL DEFAULT 'UPCOMING',

  address_override   TEXT,      -- kalau ada alamat berbeda dari customer master
  customer_phone     VARCHAR(20),-- snapshot phone saat job dibuat (opsional)
  issue_summary      VARCHAR(255), -- ringkas
  issue_details      TEXT,        -- kendala/detail
  unresolved_reason  TEXT,        -- form kerjaan belum terselesaikan (BLOCKED)
  done_at            TIMESTAMP,   -- set saat DONE

  -- For reminder logic
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

-- =========================================================
-- JOB PHOTOS (wajib ada untuk DONE)
-- =========================================================
CREATE TABLE IF NOT EXISTS job_photos (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  job_id        UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,

  file_url      TEXT NOT NULL,         -- path/object storage url
  file_name     VARCHAR(255),
  mime_type     VARCHAR(100),
  size_bytes    BIGINT,

  uploaded_by   UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT, -- technician
  uploaded_at   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_job_photos_job_id ON job_photos(job_id);

-- =========================================================
-- CUSTOMER REVIEW
-- - customer hanya bisa review job yang selesai dan punya bukti foto (app rule)
-- - link review expire 1x24 jam sejak dibuat
-- =========================================================
CREATE TABLE IF NOT EXISTS job_review_links (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  job_id          UUID NOT NULL UNIQUE REFERENCES jobs(id) ON DELETE CASCADE,

  token           VARCHAR(120) NOT NULL UNIQUE,   -- token link review
  expires_at      TIMESTAMP NOT NULL,             -- created_at + interval '24 hours'
  used_at         TIMESTAMP,                      -- jika sudah diisi

  created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_job_review_links_token ON job_review_links(token);
CREATE INDEX IF NOT EXISTS idx_job_review_links_exp   ON job_review_links(expires_at);

CREATE TABLE IF NOT EXISTS job_reviews (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  job_id          UUID NOT NULL UNIQUE REFERENCES jobs(id) ON DELETE CASCADE,

  rating          INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
  note            TEXT,
  photo_url       TEXT,                -- opsional dari customer

  created_at      TIMESTAMP NOT NULL DEFAULT now()
);

-- =========================================================
-- AUDIT LOG (CRM + actions)
-- =========================================================
CREATE TABLE IF NOT EXISTS audit_logs (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  actor_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  action        VARCHAR(80) NOT NULL,       -- e.g. CREATE_JOB, UPDATE_JOB_STATUS, UPLOAD_PHOTO, SUBMIT_REVIEW
  entity_type   VARCHAR(50) NOT NULL,       -- USERS, JOBS, CUSTOMERS, REVIEWS
  entity_id     UUID,
  detail        JSONB,

  created_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_actor ON audit_logs(actor_user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs(entity_type, entity_id);

-- =========================================================
-- TRIGGERS: updated_at auto update
-- =========================================================
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
