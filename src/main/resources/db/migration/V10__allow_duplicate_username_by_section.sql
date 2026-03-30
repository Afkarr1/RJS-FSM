-- Allow same username to exist with different tech_section
-- (e.g. same technician can have both FIELD and INTERNAL accounts)
ALTER TABLE users DROP CONSTRAINT IF EXISTS uq_users_tenant_username;
ALTER TABLE users ADD CONSTRAINT uq_users_tenant_username_section
    UNIQUE (tenant_id, username, tech_section);
