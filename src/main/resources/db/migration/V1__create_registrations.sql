CREATE TABLE registrations (
                               id          UUID PRIMARY KEY,
                               full_name   VARCHAR(100) NOT NULL,
                               phone       VARCHAR(16)  NOT NULL,
                               email       VARCHAR(254),
                               city        VARCHAR(80),
                               consent_at  TIMESTAMPTZ  NOT NULL,
                               created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
                               CONSTRAINT uq_registrations_phone UNIQUE (phone)
);