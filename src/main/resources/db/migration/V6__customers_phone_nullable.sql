-- Phone is optional for customer contact database
ALTER TABLE customers ALTER COLUMN phone_e164 DROP NOT NULL;
