-- Opt-in belongs to this Google subject/destination and course, including when it has no sessions.
CREATE TABLE public.calendar_invitation_courses (
    destination uuid NOT NULL REFERENCES public.personal_google_calendars(id) ON DELETE CASCADE,
    course_id bigint NOT NULL REFERENCES public.courses(id),
    cleanup boolean NOT NULL DEFAULT false,
    next_review timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY(destination,course_id)
);
CREATE INDEX ix_invitation_review ON public.calendar_invitation_courses(next_review,destination,course_id);
ALTER TABLE public.user_session_calendar_events ADD COLUMN invitation_course_id bigint,
 ADD COLUMN invitation_insert_uncertain boolean NOT NULL DEFAULT false;
UPDATE public.user_session_calendar_events SET invitation_course_id=(desired->>'course')::bigint,
 invitation_insert_uncertain=(revision>applied_revision AND attempts>0)
 WHERE jsonb_exists(desired,'attendees');
INSERT INTO public.calendar_invitation_courses(destination,course_id)
 SELECT DISTINCT destination,invitation_course_id FROM public.user_session_calendar_events
 WHERE invitation_course_id IS NOT NULL;

CREATE VIEW public.calendar_event_target AS
SELECT v.destination,v.session_id,
 CASE WHEN i.destination IS NOT NULL AND (v.desired->>'start')::numeric<=extract(epoch FROM now()) THEN coalesce(h.desired,v.desired)
 WHEN i.destination IS NOT NULL THEN v.desired || jsonb_build_object('attendees',(SELECT coalesce(jsonb_agg(email ORDER BY email),'[]'::jsonb) FROM (
          SELECT DISTINCT lower(trim(u.email)) email FROM users u JOIN enrollments e ON e.student_id=u.id
          WHERE e.course_id=(v.desired->>'course')::bigint AND e.status='ACTIVE'
          AND u.role='STUDENT' AND NOT u.locked AND (u.email_verified OR u.student_login_without_verification)
          AND length(trim(u.email))<=254 AND lower(trim(u.email)) ~ '^[^[:space:]@,;<>]+@[^[:space:]@,;<>]+[.][^[:space:]@,;<>]+$'
          AND EXISTS(SELECT FROM payments p WHERE p.enrollment_id=e.id AND p.status='CONFIRMED')
        ) eligible))
 ELSE v.desired END desired, i.course_id invitation_course_id
FROM public.personal_calendar_schedule v
LEFT JOIN public.user_session_calendar_events h ON h.destination=v.destination AND h.session_id=v.session_id
LEFT JOIN public.calendar_invitation_courses i ON i.destination=v.destination
 AND i.course_id=(v.desired->>'course')::bigint AND v.desired->>'role'='TEACHER'
WHERE i.destination IS NULL OR (NOT i.cleanup AND ((v.desired->>'start')::numeric>extract(epoch FROM now()) OR h.wanted));

CREATE TRIGGER calendar_student_email_change AFTER UPDATE OF email ON public.users
 FOR EACH ROW EXECUTE FUNCTION public.record_calendar_change('user','id');
INSERT INTO public.calendar_sync_changes(entity,entity_id) SELECT 'course',course_id FROM public.calendar_invitation_courses;
