-- Preserve the policy of every existing account; only new Student registration opts in.
ALTER TABLE public.users ADD COLUMN student_login_without_verification BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE public.users ADD CONSTRAINT ck_users_student_login_policy
    CHECK (NOT student_login_without_verification OR role = 'STUDENT');
