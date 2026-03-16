-- 1) tenants table
CREATE TABLE IF NOT EXISTS tenants (
  id uuid PRIMARY KEY,
  name varchar(150) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

-- 2) seed default tenant (RJS)
-- id reserved & gampang diingat
INSERT INTO tenants (id, name)
VALUES ('00000000-0000-0000-0000-000000000001', 'Restu Jaya Sentosa')
ON CONFLICT (id) DO NOTHING;

-- 3) add tenant_id nullable first (existing rows)
ALTER TABLE users ADD COLUMN IF NOT EXISTS tenant_id uuid;
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS tenant_id uuid;
ALTER TABLE job_status_history ADD COLUMN IF NOT EXISTS tenant_id uuid;

-- 4) seed existing rows to default tenant
UPDATE users SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE jobs SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;
UPDATE job_status_history SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;

-- 5) set NOT NULL after safe
ALTER TABLE users ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE jobs ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE job_status_history ALTER COLUMN tenant_id SET NOT NULL;

-- 6) FK constraints
ALTER TABLE users
  ADD CONSTRAINT fk_users_tenant
  FOREIGN KEY (tenant_id) REFERENCES tenants(id)
  ON DELETE RESTRICT;

ALTER TABLE jobs
  ADD CONSTRAINT fk_jobs_tenant
  FOREIGN KEY (tenant_id) REFERENCES tenants(id)
  ON DELETE RESTRICT;

ALTER TABLE job_status_history
  ADD CONSTRAINT fk_job_status_history_tenant
  FOREIGN KEY (tenant_id) REFERENCES tenants(id)
  ON DELETE RESTRICT;

-- 7) indexes
CREATE INDEX IF NOT EXISTS idx_users_tenant_id ON users(tenant_id);

CREATE INDEX IF NOT EXISTS idx_jobs_tenant_created_at ON jobs(tenant_id, created_at);
CREATE INDEX IF NOT EXISTS idx_jobs_tenant_status ON jobs(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_jobs_tenant_assigned_to ON jobs(tenant_id, assigned_to_id);

CREATE INDEX IF NOT EXISTS idx_hist_tenant_job ON job_status_history(tenant_id, job_id);
CREATE INDEX IF NOT EXISTS idx_hist_tenant_changed_at ON job_status_history(tenant_id, changed_at);

-- 8) username unique must be per-tenant
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_username_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_tenant_username ON users(tenant_id, username);