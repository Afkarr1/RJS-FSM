CREATE TABLE IF NOT EXISTS jobs (
  id UUID PRIMARY KEY,
  title VARCHAR(150) NOT NULL,
  description TEXT NULL,

  customer_name VARCHAR(150) NOT NULL,
  customer_phone VARCHAR(50) NOT NULL,
  address TEXT NULL,

  status VARCHAR(30) NOT NULL,

  assigned_to_id UUID NULL,
  assigned_at TIMESTAMPTZ NULL,

  started_at TIMESTAMPTZ NULL,
  finished_at TIMESTAMPTZ NULL,
  closed_at TIMESTAMPTZ NULL,

  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT fk_jobs_assigned_to FOREIGN KEY (assigned_to_id) REFERENCES users(id)
);

CREATE INDEX idx_jobs_status ON jobs(status);
CREATE INDEX idx_jobs_assigned_to ON jobs(assigned_to_id);
