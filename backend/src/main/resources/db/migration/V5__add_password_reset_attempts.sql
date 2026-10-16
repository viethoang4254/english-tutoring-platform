ALTER TABLE public.password_reset_credentials
    ADD COLUMN failed_attempts INTEGER NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_password_reset_credentials_attempts CHECK (failed_attempts BETWEEN 0 AND 5);
