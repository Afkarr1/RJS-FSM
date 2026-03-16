-- =========================================================
-- RJS-FSM V1: Complete Schema (PostgreSQL 16+)
-- =========================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================================================
-- TENANTS
-- =========================================================
CREATE TABLE tenants (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(150) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO tenants (id, name) VALUES
    ('00000000-0000-0000-0000-000000000001', 'Restu Jaya Sentosa');

-- =========================================================
-- USERS
-- =========================================================
CREATE TABLE users (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID        NOT NULL REFERENCES tenants(id),
    username      VARCHAR(80) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(50) NOT NULL,
    full_name     VARCHAR(150) NOT NULL,
    phone_e164    VARCHAR(20),
    is_active     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_login_at TIMESTAMPTZ,
    CONSTRAINT uq_users_tenant_username UNIQUE (tenant_id, username)
);

CREATE INDEX idx_users_tenant_id ON users(tenant_id);
CREATE INDEX idx_users_role ON users(role);

-- =========================================================
-- CUSTOMERS
-- =========================================================
CREATE TABLE customers (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID        NOT NULL REFERENCES tenants(id),
    name       VARCHAR(150) NOT NULL,
    address    TEXT,
    phone_e164 VARCHAR(20) NOT NULL,
    notes      TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_customers_tenant ON customers(tenant_id);
CREATE INDEX idx_customers_phone ON customers(phone_e164);

-- =========================================================
-- JOBS
-- =========================================================
CREATE TABLE jobs (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID        NOT NULL REFERENCES tenants(id),
    title            VARCHAR(255) NOT NULL,
    description      TEXT,
    customer_name    VARCHAR(150),
    customer_phone   VARCHAR(20),
    address          TEXT,
    status           VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    assigned_to_id   UUID        REFERENCES users(id),
    created_by_id    UUID        REFERENCES users(id),
    scheduled_date   DATE,
    assigned_at      TIMESTAMPTZ,
    started_at       TIMESTAMPTZ,
    finished_at      TIMESTAMPTZ,
    closed_at        TIMESTAMPTZ,
    requires_photo   BOOLEAN     NOT NULL DEFAULT TRUE,
    photo_uploaded   BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_jobs_tenant_status ON jobs(tenant_id, status);
CREATE INDEX idx_jobs_tenant_assigned ON jobs(tenant_id, assigned_to_id);
CREATE INDEX idx_jobs_tenant_date ON jobs(tenant_id, scheduled_date);

-- =========================================================
-- JOB STATUS HISTORY (FSM audit trail)
-- =========================================================
CREATE TABLE job_status_history (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID        NOT NULL REFERENCES tenants(id),
    job_id      UUID        NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    from_status VARCHAR(50),
    to_status   VARCHAR(50) NOT NULL,
    changed_by  UUID        NOT NULL REFERENCES users(id),
    changed_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_hist_job ON job_status_history(job_id);
CREATE INDEX idx_hist_tenant ON job_status_history(tenant_id, changed_at);

-- =========================================================
-- JOB PHOTOS
-- =========================================================
CREATE TABLE job_photos (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID        NOT NULL REFERENCES tenants(id),
    job_id      UUID        NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    file_path   VARCHAR(500) NOT NULL,
    file_name   VARCHAR(255) NOT NULL,
    mime_type   VARCHAR(100),
    size_bytes  BIGINT,
    uploaded_by UUID        NOT NULL REFERENCES users(id),
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_photos_job ON job_photos(job_id);

-- =========================================================
-- JOB REVIEW LINKS (token-based, 24h expiry)
-- =========================================================
CREATE TABLE job_review_links (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID        NOT NULL REFERENCES tenants(id),
    job_id     UUID        NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    token      VARCHAR(120) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_review_links_token ON job_review_links(token);

-- =========================================================
-- JOB REVIEWS
-- =========================================================
CREATE TABLE job_reviews (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID NOT NULL REFERENCES tenants(id),
    job_id     UUID NOT NULL UNIQUE REFERENCES jobs(id) ON DELETE CASCADE,
    rating     INT  NOT NULL CHECK (rating >= 1 AND rating <= 5),
    note       TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- =========================================================
-- AUDIT LOGS
-- =========================================================
CREATE TABLE audit_logs (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID        NOT NULL REFERENCES tenants(id),
    actor_user_id UUID        REFERENCES users(id),
    action        VARCHAR(100) NOT NULL,
    entity_type   VARCHAR(50) NOT NULL,
    entity_id     UUID,
    detail        JSONB,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_actor ON audit_logs(actor_user_id);
CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);

-- =========================================================
-- USER LOGIN AUDIT
-- =========================================================
CREATE TABLE user_login_audit (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID        NOT NULL REFERENCES tenants(id),
    user_id     UUID        REFERENCES users(id),
    username    VARCHAR(80) NOT NULL,
    role        VARCHAR(50),
    logged_in_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ip_address  VARCHAR(45),
    user_agent  TEXT
);

CREATE INDEX idx_login_audit_user ON user_login_audit(user_id);
CREATE INDEX idx_login_audit_time ON user_login_audit(logged_in_at);

-- =========================================================
-- USER ADMIN ACTIONS
-- =========================================================
CREATE TABLE user_admin_actions (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID        NOT NULL REFERENCES tenants(id),
    target_user_id  UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    actor_admin_id  UUID        NOT NULL REFERENCES users(id),
    action_type     VARCHAR(100) NOT NULL,
    note            TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_admin_actions_target ON user_admin_actions(target_user_id);

-- =========================================================
-- TRIGGERS: auto-update updated_at
-- =========================================================
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_customers_updated_at
    BEFORE UPDATE ON customers FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_jobs_updated_at
    BEFORE UPDATE ON jobs FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =========================================================
-- SEED: Default admin accounts
-- =========================================================
INSERT INTO users (tenant_id, username, password_hash, role, full_name, phone_e164) VALUES
    ('00000000-0000-0000-0000-000000000001', 'admin01',
     '$2a$12$LJ3a4VQnJFh2qGSMzs9KQuDgX0nXVQjOqBPFg/k.xFRrW9j5KFYG6',
     'ADMIN', 'Admin 01 RJS', '+6200000000001'),
    ('00000000-0000-0000-0000-000000000001', 'admin02',
     '$2a$12$LJ3a4VQnJFh2qGSMzs9KQuDgX0nXVQjOqBPFg/k.xFRrW9j5KFYG6',
     'ADMIN', 'Admin 02 RJS', '+6200000000002');
-- Default password for both: rjs@Admin2024 (CHANGE IN PRODUCTION!)
