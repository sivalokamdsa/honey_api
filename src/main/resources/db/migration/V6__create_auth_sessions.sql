-- token_hash is the SHA-256 (hex) of the bearer token. The raw token is never stored.
CREATE TABLE auth_sessions (
                               token_hash       VARCHAR(64)  PRIMARY KEY,
                               registration_id  UUID         NOT NULL REFERENCES registrations (id) ON DELETE CASCADE,
                               created_at       TIMESTAMPTZ  NOT NULL,
                               expires_at       TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_auth_sessions_registration_id ON auth_sessions (registration_id);
