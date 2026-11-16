-- The legacy organizational mappings have no personal OAuth owner. Keep them separate.
CREATE TABLE public.personal_google_calendars (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id bigint NOT NULL REFERENCES public.google_calendar_connections(user_id),
    google_subject varchar(255) NOT NULL,
    calendar_id text UNIQUE,
    provisioning text NOT NULL DEFAULT 'NEW' CHECK (provisioning IN ('NEW','CREATING','READY','UNCERTAIN','FAILED')),
    attempts integer NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    next_attempt timestamptz NOT NULL DEFAULT now(),
    provision_attempt uuid,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE(user_id,google_subject)
);

-- One durable job per destination/session doubles as the remote event mapping.
-- Deliberately retain the session ID after draft regeneration so a deletion can finish.
CREATE TABLE public.user_session_calendar_events (
    destination uuid NOT NULL REFERENCES public.personal_google_calendars(id),
    session_id bigint NOT NULL,
    event_id text NOT NULL DEFAULT replace(gen_random_uuid()::text,'-',''),
    revision bigint NOT NULL DEFAULT 1,
    applied_revision bigint NOT NULL DEFAULT 0,
    wanted boolean NOT NULL,
    desired jsonb,
    attempts integer NOT NULL DEFAULT 0,
    next_attempt timestamptz NOT NULL DEFAULT now(),
    lease uuid,
    lease_until timestamptz,
    failure text,
    last_success timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    CHECK (revision >= applied_revision AND applied_revision >= 0 AND attempts >= 0),
    PRIMARY KEY(destination,session_id),
    UNIQUE(destination,event_id),
    CHECK(wanted = (desired IS NOT NULL))
);
CREATE INDEX ix_calendar_sync_due ON public.user_session_calendar_events(next_attempt)
    WHERE applied_revision < revision;

CREATE VIEW public.personal_calendar_schedule AS
SELECT d.id destination,s.id session_id,jsonb_build_object(
    'course',c.id,'name',c.name,'number',s.session_number,'title',s.title,
    'start',extract(epoch FROM s.scheduled_start_at),'end',extract(epoch FROM s.scheduled_end_at),
    'timezone',c.timezone,'role',u.role) desired
FROM public.personal_google_calendars d
JOIN public.google_calendar_connections g ON g.user_id=d.user_id AND g.google_subject=d.google_subject AND g.status='CONNECTED'
JOIN public.users u ON u.id=g.user_id AND NOT u.locked
JOIN public.courses c ON c.status='PUBLISHED'
JOIN public.sessions s ON s.course_id=c.id AND s.status='SCHEDULED'
WHERE (u.role='TEACHER' AND c.teacher_id=u.id AND EXISTS
    (SELECT FROM public.teacher_applications a WHERE a.teacher_id=u.id AND a.status='APPROVED'))
OR (u.role='STUDENT' AND (u.email_verified OR u.student_login_without_verification) AND EXISTS
    (SELECT FROM public.enrollments e WHERE e.course_id=c.id AND e.student_id=u.id AND e.status='ACTIVE'
      AND EXISTS (SELECT FROM public.payments p WHERE p.enrollment_id=e.id AND p.status='CONFIRMED')));

-- Record invalidations in the originating transaction. The projector consumes them after commit.
CREATE TABLE public.calendar_sync_changes (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    entity text NOT NULL,
    entity_id bigint NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE FUNCTION public.record_calendar_change() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE row_data jsonb;
BEGIN
    row_data := CASE WHEN TG_OP='DELETE' THEN to_jsonb(OLD) ELSE to_jsonb(NEW) END;
    INSERT INTO public.calendar_sync_changes(entity,entity_id) VALUES(TG_ARGV[0],(row_data->>TG_ARGV[1])::bigint);
    -- Organizational rows have no authorized personal OAuth owner. Retain a separate
    -- operator reconciliation item while allowing the authoritative session edit.
    IF TG_ARGV[0]='session' AND TG_OP='UPDATE' THEN
        INSERT INTO public.calendar_sync_changes(entity,entity_id)
        SELECT 'legacy-review',NEW.id WHERE EXISTS(SELECT FROM public.session_calendar_events WHERE session_id=NEW.id);
    END IF;
    RETURN NULL;
END $$;
CREATE TRIGGER calendar_session_change AFTER INSERT OR DELETE OR UPDATE OF scheduled_start_at,scheduled_end_at,status,title ON public.sessions
    FOR EACH ROW EXECUTE FUNCTION public.record_calendar_change('session','id');
CREATE TRIGGER calendar_course_change AFTER UPDATE OF status,name,timezone ON public.courses
    FOR EACH ROW EXECUTE FUNCTION public.record_calendar_change('course','id');
CREATE TRIGGER calendar_enrollment_change AFTER INSERT OR DELETE OR UPDATE OF status ON public.enrollments
    FOR EACH ROW EXECUTE FUNCTION public.record_calendar_change('user','student_id');
CREATE TRIGGER calendar_payment_change AFTER INSERT OR DELETE OR UPDATE OF status ON public.payments
    FOR EACH ROW EXECUTE FUNCTION public.record_calendar_change('payment-enrollment','enrollment_id');
CREATE TRIGGER calendar_teacher_change AFTER INSERT OR DELETE OR UPDATE OF status ON public.teacher_applications
    FOR EACH ROW EXECUTE FUNCTION public.record_calendar_change('user','teacher_id');
CREATE TRIGGER calendar_user_change AFTER UPDATE OF locked,email_verified,student_login_without_verification,role ON public.users
    FOR EACH ROW EXECUTE FUNCTION public.record_calendar_change('user','id');
CREATE TRIGGER calendar_connection_change AFTER INSERT OR UPDATE OF status,google_subject ON public.google_calendar_connections
    FOR EACH ROW EXECUTE FUNCTION public.record_calendar_change('connection','user_id');
INSERT INTO public.calendar_sync_changes(entity,entity_id)
    SELECT 'user',user_id FROM public.google_calendar_connections WHERE status='CONNECTED';
