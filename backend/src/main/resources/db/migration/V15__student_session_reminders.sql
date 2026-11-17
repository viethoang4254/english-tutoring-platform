CREATE TABLE public.student_session_reminders (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    session_id bigint NOT NULL REFERENCES public.sessions(id) ON DELETE CASCADE,
    student_id bigint NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    starts_at timestamptz NOT NULL,
    schedule_version bigint NOT NULL,
    due_at timestamptz NOT NULL,
    state text NOT NULL DEFAULT 'PENDING'
        CHECK (state IN ('PENDING','SENDING','SENT','CANCELLED','FAILED','UNKNOWN')),
    attempts integer NOT NULL DEFAULT 0 CHECK (attempts BETWEEN 0 AND 3),
    next_attempt timestamptz NOT NULL,
    attempted_at timestamptz,
    finished_at timestamptz,
    UNIQUE (session_id, student_id, starts_at, schedule_version)
);
CREATE INDEX ix_reminder_due ON public.student_session_reminders(next_attempt,id)
    WHERE state='PENDING';
CREATE INDEX ix_reminder_sending ON public.student_session_reminders(attempted_at,id)
    WHERE state='SENDING';

CREATE VIEW public.student_reminder_targets AS
SELECT s.id session_id, u.id student_id, s.scheduled_start_at starts_at,
       coalesce((SELECT max(h.id) FROM public.session_schedule_changes h WHERE h.session_id=s.id),0) schedule_version,
       u.email, u.full_name, c.name course_name, c.timezone, s.session_number
FROM public.sessions s JOIN public.courses c ON c.id=s.course_id
JOIN public.enrollments e ON e.course_id=c.id JOIN public.users u ON u.id=e.student_id
WHERE s.status='SCHEDULED' AND c.status='PUBLISHED' AND e.status='ACTIVE'
  AND u.role='STUDENT' AND NOT u.locked AND (u.email_verified OR u.student_login_without_verification)
  AND u.email ~ '^[^[:space:][:cntrl:]@,;<>]+@[^[:space:][:cntrl:]@,;<>]+[.][^[:space:][:cntrl:]@,;<>]+$'
  AND EXISTS (SELECT FROM public.payments p WHERE p.enrollment_id=e.id AND p.status='CONFIRMED');
