-- Seed default admin01 for dev
-- username: admin01rjs
-- password: rjs0010122 (bcrypt)

INSERT INTO users (username, password_hash, role, full_name, phone_e164, is_active)
VALUES (
  'admin01rjs',
  '$2a$10$0hV7fX8KZyqXvM3kK7c7Ue9mF0BvZP5uG9cXyJcXgGz8c9M3Y8v3W',
  'ADMIN',
  'Admin 01 RJS',
  '+620000000000',
  TRUE
)
ON CONFLICT (username) DO UPDATE
SET password_hash = EXCLUDED.password_hash,
    role = EXCLUDED.role,
    is_active = EXCLUDED.is_active;
