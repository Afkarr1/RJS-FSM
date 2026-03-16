-- Ubah type kolom role dari enum user_role ke varchar
ALTER TABLE users
  ALTER COLUMN role TYPE varchar(50)
  USING role::text;

-- (Opsional) kalau type enum user_role gak dipakai lagi, bisa drop
-- DROP TYPE IF EXISTS user_role;
