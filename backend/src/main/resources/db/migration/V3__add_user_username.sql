-- Existing accounts and Teacher/Admin creation may retain NULL usernames.
ALTER TABLE public.users ADD COLUMN username TEXT;
ALTER TABLE public.users ADD CONSTRAINT uq_users_username UNIQUE (username);
ALTER TABLE public.users ADD CONSTRAINT ck_users_username_format
    CHECK (username IS NULL OR username ~ '^[a-z][a-z0-9_]{2,29}$');
