-- Every registration is a USER unless it is promoted to ADMIN by the backend (see RegistrationService.changeRole).
ALTER TABLE registrations ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';
ALTER TABLE registrations ADD CONSTRAINT ck_registrations_role CHECK (role IN ('USER', 'ADMIN'));
