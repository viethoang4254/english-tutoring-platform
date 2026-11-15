-- Stage C3: personal OAuth connections only; no Calendar event mappings.
CREATE TABLE public.google_calendar_connections (
    user_id BIGINT PRIMARY KEY REFERENCES public.users(id),
    google_subject VARCHAR(255),
    google_email VARCHAR(320),
    granted_scopes TEXT,
    refresh_ciphertext TEXT,
    key_id VARCHAR(40),
    status VARCHAR(24) NOT NULL DEFAULT 'DISCONNECTED'
        CHECK (status IN ('DISCONNECTED','CONNECTED','RECONNECT_REQUIRED','REVOCATION_PENDING')),
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    connected_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (status <> 'CONNECTED' OR (google_subject IS NOT NULL AND google_email IS NOT NULL
        AND granted_scopes IS NOT NULL AND refresh_ciphertext IS NOT NULL AND key_id IS NOT NULL AND connected_at IS NOT NULL)),
    CHECK (status <> 'DISCONNECTED' OR refresh_ciphertext IS NULL)
);
CREATE UNIQUE INDEX uq_google_calendar_active_subject ON public.google_calendar_connections(google_subject) WHERE google_subject IS NOT NULL;
-- Retain a release boundary so consent begun before revocation cannot be reused.
CREATE SEQUENCE public.google_calendar_consent_order;
CREATE TABLE public.google_calendar_subject_releases (
    google_subject VARCHAR(255) PRIMARY KEY,
    release_order BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE public.google_calendar_oauth_intents (
    state_hash VARCHAR(43) PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES public.users(id),
    browser_hash VARCHAR(43) NOT NULL,
    verifier_ciphertext TEXT,
    key_id VARCHAR(40) NOT NULL,
    nonce_hash VARCHAR(43) NOT NULL,
    consent_order BIGINT NOT NULL DEFAULT nextval('public.google_calendar_consent_order'),
    connection_version BIGINT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (expires_at > created_at),
    CHECK (consumed_at IS NOT NULL OR verifier_ciphertext IS NOT NULL)
);
CREATE INDEX ix_google_calendar_intent_expiry ON public.google_calendar_oauth_intents(expires_at);
