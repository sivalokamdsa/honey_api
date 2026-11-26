-- BCrypt hashes only, never plaintext. Nullable because rows registered before this migration have no password;
-- login rejects those rows.
ALTER TABLE registrations ADD COLUMN password_hash VARCHAR(100);
