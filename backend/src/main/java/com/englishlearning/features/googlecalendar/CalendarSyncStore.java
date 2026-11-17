package com.englishlearning.features.googlecalendar;

import java.util.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class CalendarSyncStore {
    public CalendarSyncStore(org.springframework.beans.factory.ObjectProvider<JdbcTemplate> jdbc) { this.jdbc = jdbc; }
    private final org.springframework.beans.factory.ObjectProvider<JdbcTemplate> jdbc;
    private JdbcTemplate sql() { return jdbc.getObject(); }

    @Transactional
    public void project() {
        if (!Boolean.TRUE.equals(sql().queryForObject("SELECT pg_try_advisory_xact_lock(740034)", Boolean.class))) return;
        var ids = sql().queryForList("SELECT id FROM calendar_sync_changes WHERE entity<>'legacy-review' ORDER BY id LIMIT 512 FOR UPDATE", Long.class);
        // Recheck a bounded batch of delivered tombstones: an old HTTP insert can
        // finish after its worker died and another worker observed a missing event.
        sql().update("""
            UPDATE user_session_calendar_events m SET revision=revision+1,attempts=0,next_attempt=now()
            WHERE (destination,session_id) IN (
                SELECT x.destination,x.session_id FROM user_session_calendar_events x
                JOIN personal_google_calendars d ON d.id=x.destination
                JOIN google_calendar_connections g ON g.user_id=d.user_id AND g.google_subject=d.google_subject
                WHERE g.status='CONNECTED' AND d.provisioning='READY' AND NOT x.wanted
                AND x.revision=x.applied_revision AND x.last_success<now()-interval '1 day'
                ORDER BY x.last_success LIMIT 16 FOR UPDATE OF x SKIP LOCKED)
            """);
        if (ids.isEmpty()) {
            var due=sql().query("""
                SELECT destination,course_id FROM calendar_invitation_courses WHERE next_review<=now()
                ORDER BY next_review,destination,course_id LIMIT 8 FOR UPDATE SKIP LOCKED
                """,(r,n)->Map.entry(r.getObject(1,UUID.class),r.getLong(2)));
            for(var course:due){
                reconcile(course.getKey(),course.getValue());
                sql().update("UPDATE calendar_invitation_courses SET next_review=now()+interval '5 minutes' WHERE destination=? AND course_id=?",course.getKey(),course.getValue());
            }
            return;
        }
        sql().update("""
            INSERT INTO personal_google_calendars(user_id,google_subject)
            SELECT user_id,google_subject FROM google_calendar_connections WHERE status='CONNECTED'
            ON CONFLICT DO NOTHING
            """);
        reconcile(null,0);
        sql().update("""
            UPDATE user_session_calendar_events m SET attempts=0,next_attempt=now(),failure=NULL
            FROM personal_google_calendars d JOIN google_calendar_connections g
                ON g.user_id=d.user_id AND g.google_subject=d.google_subject AND g.status='CONNECTED'
            WHERE m.destination=d.id AND m.revision>m.applied_revision AND m.attempts>=8
                AND EXISTS (SELECT FROM calendar_sync_changes q WHERE q.entity='connection' AND q.entity_id=d.user_id)
            """);
        sql().batchUpdate("DELETE FROM calendar_sync_changes WHERE id=?", ids, ids.size(), (ps,id)->ps.setLong(1,id));
    }

    // The requesting Teacher is checked separately even for courses with no future sessions.
    private static final String COURSE_OWNER = """
        SELECT EXISTS(SELECT FROM courses c JOIN users u ON u.id=c.teacher_id
          JOIN google_calendar_connections g ON g.user_id=u.id AND g.status='CONNECTED'
          WHERE c.id=? AND u.id=? AND u.role='TEACHER' AND NOT u.locked AND c.status='PUBLISHED'
          AND EXISTS(SELECT FROM teacher_applications a WHERE a.teacher_id=u.id AND a.status='APPROVED'))
        """;
    private void reconcile(UUID destination,long course) {
        boolean scoped=destination!=null;
        Object[] args=scoped?new Object[]{destination,course}:new Object[0];
        sql().update("""
            INSERT INTO user_session_calendar_events AS m(destination,session_id,wanted,desired,invitation_course_id)
            SELECT v.destination,v.session_id,true,v.desired,v.invitation_course_id FROM calendar_event_target v
            WHERE ((v.desired->>'end')::numeric>extract(epoch FROM now()) OR EXISTS
              (SELECT FROM user_session_calendar_events h WHERE h.destination=v.destination AND h.session_id=v.session_id))
            """+(scoped?" AND v.destination=? AND v.invitation_course_id=? ":"")+"""
            ORDER BY v.destination,v.session_id
            ON CONFLICT(destination,session_id) DO UPDATE SET wanted=true,desired=excluded.desired,
              invitation_course_id=excluded.invitation_course_id,revision=m.revision+1,attempts=0,next_attempt=now(),failure=NULL
            WHERE m.desired IS DISTINCT FROM excluded.desired
            """,args);
        sql().update("""
            UPDATE user_session_calendar_events m SET wanted=false,desired=NULL,revision=revision+1,
              attempts=0,next_attempt=now(),failure=NULL
            WHERE wanted AND (m.invitation_course_id IS NULL OR (m.desired->>'end')::numeric>extract(epoch FROM now())
              OR EXISTS(SELECT FROM calendar_invitation_courses i WHERE i.destination=m.destination AND i.course_id=m.invitation_course_id AND i.cleanup))
              AND NOT EXISTS (SELECT FROM calendar_event_target v WHERE v.destination=m.destination AND v.session_id=m.session_id)
            """+(scoped?" AND m.destination=? AND m.invitation_course_id=?":""),args);
    }
    private void requireCourseOwner(long teacher,long course) {
        if (!Boolean.TRUE.equals(sql().queryForObject(COURSE_OWNER,Boolean.class,course,teacher)))
            throw new GoogleFailure("DENIED");
    }
    @Transactional
    public void requestInvitations(long teacher,long course) {
        sql().queryForObject("SELECT id FROM users WHERE id=? FOR UPDATE",Long.class,teacher);
        sql().execute("SELECT pg_advisory_xact_lock(740034)");
        requireCourseOwner(teacher,course);
        sql().update("""
            INSERT INTO personal_google_calendars(user_id,google_subject)
            SELECT user_id,google_subject FROM google_calendar_connections WHERE user_id=? AND status='CONNECTED'
            ON CONFLICT DO NOTHING
            """,teacher);
        UUID destination=sql().queryForObject("SELECT d.id FROM personal_google_calendars d "+OWNER+" WHERE d.user_id=?",UUID.class,teacher);
        if(Boolean.TRUE.equals(sql().queryForObject("SELECT EXISTS(SELECT FROM calendar_invitation_courses WHERE destination=? AND cleanup)",Boolean.class,destination)))
            throw new GoogleFailure("CLEANUP_PENDING");
        sql().update("INSERT INTO calendar_invitation_courses(destination,course_id) VALUES (?,?) ON CONFLICT DO NOTHING",destination,course);
        reconcile(destination,course);
        sql().update("UPDATE user_session_calendar_events SET attempts=0,failure=NULL,next_attempt=now() WHERE destination=? AND invitation_course_id=? AND revision>applied_revision AND attempts>=8 AND (lease_until IS NULL OR lease_until<now())",destination,course);
    }
    public record UpcomingSession(int number,java.time.Instant start,java.time.Instant end,String status,String syncState) {}
    public record CourseOverview(long eligibleStudents,List<UpcomingSession> sessions) {}
    @Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public CourseOverview courseOverview(long teacher,long course) {
        boolean owned=Boolean.TRUE.equals(sql().queryForObject("""
            SELECT EXISTS(SELECT FROM courses c JOIN users u ON u.id=c.teacher_id
            WHERE c.id=? AND u.id=? AND u.role='TEACHER' AND NOT u.locked AND EXISTS
            (SELECT FROM teacher_applications a WHERE a.teacher_id=u.id AND a.status='APPROVED'))
            """,Boolean.class,course,teacher));
        if(!owned)throw new GoogleFailure("DENIED");
        long eligible=sql().queryForObject("""
            SELECT count(*) FROM enrollments e JOIN users u ON u.id=e.student_id
            JOIN courses c ON c.id=e.course_id
            WHERE e.course_id=? AND c.status='PUBLISHED' AND e.status='ACTIVE' AND u.role='STUDENT'
            AND NOT u.locked AND (u.email_verified OR u.student_login_without_verification)
            AND length(trim(u.email))<=254 AND lower(trim(u.email)) ~ '^[^[:space:]@,;<>]+@[^[:space:]@,;<>]+[.][^[:space:]@,;<>]+$'
            AND EXISTS(SELECT FROM payments p WHERE p.enrollment_id=e.id AND p.status='CONFIRMED')
            """,Long.class,course);
        var sessions=sql().query("""
            SELECT s.session_number,s.scheduled_start_at,s.scheduled_end_at,s.status,progress.state
            FROM sessions s LEFT JOIN (
                SELECT m.session_id,m.invitation_course_id,
                    CASE WHEN m.revision=m.applied_revision THEN 'SYNCED'
                         WHEN m.attempts>=8 OR d.provisioning IN ('FAILED','UNCERTAIN') THEN 'FAILED'
                         ELSE 'PENDING' END state
                FROM user_session_calendar_events m JOIN personal_google_calendars d ON d.id=m.destination
                JOIN google_calendar_connections g ON g.user_id=d.user_id AND g.google_subject=d.google_subject
                WHERE d.user_id=? AND g.status='CONNECTED' AND m.invitation_course_id=?
            ) progress ON progress.session_id=s.id AND progress.invitation_course_id=s.course_id
            WHERE s.course_id=? AND s.scheduled_start_at>now() ORDER BY s.scheduled_start_at,s.id
            """,(r,n)->new UpcomingSession(r.getInt(1),r.getTimestamp(2).toInstant(),r.getTimestamp(3).toInstant(),
                    r.getString(4),r.getString(5)),teacher,course,course);
        return new CourseOverview(eligible,sessions);
    }
    public record InvitationStatus(long pending,long synchronizedSessions,long failed) {}
    public InvitationStatus invitationStatus(long teacher,long course) {
        requireCourseOwner(teacher,course);
        return sql().queryForObject("""
            SELECT count(*) FILTER(WHERE revision>applied_revision AND m.attempts<8
                AND provisioning NOT IN ('FAILED','UNCERTAIN')) pending,
                count(*) FILTER(WHERE revision=applied_revision) synced,
                count(*) FILTER(WHERE revision>applied_revision AND (m.attempts>=8 OR provisioning IN ('FAILED','UNCERTAIN'))) failed
            FROM user_session_calendar_events m JOIN personal_google_calendars d ON d.id=m.destination
            """ + OWNER + """
            WHERE d.user_id=? AND m.invitation_course_id=?
            """,(r,n)->new InvitationStatus(r.getLong(1),r.getLong(2),r.getLong(3)),teacher,course);
    }

    public record Status(String state,long pending,long failed,java.time.Instant lastSuccess) {}
    public Status status(long user) {
        // Projection is batched globally. Pending committed changes must not be
        // presented as a fully synchronized snapshot before the projector runs.
        boolean awaitingProjection=Boolean.TRUE.equals(sql().queryForObject(
            "SELECT EXISTS(SELECT FROM calendar_sync_changes WHERE entity<>'legacy-review')",Boolean.class));
        var states=sql().query("SELECT d.provisioning,d.calendar_id IS NOT NULL AS known_calendar,count(m.session_id) FILTER(WHERE revision>applied_revision AND m.attempts<8) pending,count(m.session_id) FILTER(WHERE revision>applied_revision AND m.attempts>=8) failed,max(m.last_success) latest FROM personal_google_calendars d "+OWNER+
            "LEFT JOIN user_session_calendar_events m ON m.destination=d.id WHERE d.user_id=? GROUP BY d.id",(r,n)->{
                var time=r.getTimestamp("latest");
                long pending=r.getLong("pending"),failed=r.getLong("failed");
                String provision=r.getString("provisioning");
                String state=switch(provision){
                    case "UNCERTAIN" -> r.getBoolean("known_calendar")?"FAILED":"REVIEW_REQUIRED";
                    case "FAILED" -> "FAILED";
                    case "READY" -> failed>0?"FAILED":pending>0||awaitingProjection?"PENDING":time==null?"EMPTY":"SYNCED";
                    default -> "PENDING";
                };
                return new Status(state,pending,failed,time==null?null:time.toInstant());
            },user);
        return states.isEmpty()?new Status("PENDING",0,0,null):states.get(0);
    }

    @Transactional
    public void retry(long user) {
        sql().update("UPDATE user_session_calendar_events m SET attempts=0,failure=NULL,next_attempt=now() FROM personal_google_calendars d "+OWNER+
            "WHERE d.user_id=? AND m.destination=d.id AND m.revision>m.applied_revision AND m.attempts>=8 AND (m.lease_until IS NULL OR m.lease_until<now())",user);
        sql().update("UPDATE personal_google_calendars d SET provisioning='READY' FROM google_calendar_connections g WHERE d.user_id=? AND g.user_id=d.user_id AND g.google_subject=d.google_subject AND g.status='CONNECTED' AND d.provisioning='UNCERTAIN' AND calendar_id IS NOT NULL",user);
        sql().update("UPDATE personal_google_calendars d SET attempts=0,provisioning='NEW',next_attempt=now() FROM google_calendar_connections g WHERE d.user_id=? AND g.user_id=d.user_id AND g.google_subject=d.google_subject AND g.status='CONNECTED' AND d.provisioning='FAILED'",user);
    }

    record Destination(UUID id,long user,String subject,long version,String calendar,UUID provisionAttempt) {
        Destination(UUID id,long user,String subject,long version,String calendar) {
            this(id,user,subject,version,calendar,null);
        }
    }
    private Destination destination(ResultSet r) throws SQLException {
        return new Destination(r.getObject("id",UUID.class),r.getLong("user_id"),r.getString("google_subject"),
            r.getLong("version"),r.getString("calendar_id"),r.getObject("provision_attempt",UUID.class));
    }
    private static final String OWNER = """
        JOIN google_calendar_connections g ON g.user_id=d.user_id AND g.google_subject=d.google_subject AND g.status='CONNECTED'
        """;

    @Transactional
    public Destination provision() {
        sql().update("UPDATE personal_google_calendars SET provisioning='UNCERTAIN' WHERE provisioning='CREATING' AND next_attempt<now()");
        var list=sql().query("SELECT d.*,g.version FROM personal_google_calendars d "+OWNER+"""
            JOIN users u ON u.id=d.user_id AND NOT u.locked
            WHERE d.provisioning='NEW' AND d.next_attempt<=now() AND
            ((u.role='STUDENT' AND (u.email_verified OR u.student_login_without_verification)) OR
             (u.role='TEACHER' AND EXISTS(SELECT FROM teacher_applications a WHERE a.teacher_id=u.id AND a.status='APPROVED')))
            ORDER BY d.next_attempt,d.id LIMIT 1 FOR UPDATE OF d SKIP LOCKED
            """,(r,n)->destination(r));
        if(list.isEmpty())return null;
        var found=list.get(0);
        var dest=new Destination(found.id(),found.user(),found.subject(),found.version(),found.calendar(),UUID.randomUUID());
        sql().update("UPDATE personal_google_calendars SET provision_attempt=?,provisioning='CREATING',next_attempt=now()+interval '2 minutes',attempts=attempts+1 WHERE id=?",dest.provisionAttempt(),dest.id());
        return dest;
    }
    public void provisioned(Destination d,String calendar,String state) {
        sql().update("UPDATE personal_google_calendars SET provisioning=CASE WHEN ?='NEW' AND attempts>=8 THEN 'FAILED' ELSE ? END,calendar_id=?,next_attempt=now()+(least(256,power(2,least(attempts,8))) * (1+random()) * interval '1 second') WHERE id=? AND provision_attempt=? AND provisioning IN ('CREATING','UNCERTAIN')",
            state,state,calendar,d.id(),d.provisionAttempt());
    }
    record Job(Destination dest,long session,String event,long revision,UUID lease,String desired,int attempts) {}
    @Transactional
    public Job claim() {
        var rows=sql().query("SELECT d.*,g.version,m.session_id,m.event_id,m.revision,m.desired,m.attempts AS job_attempts FROM user_session_calendar_events m JOIN personal_google_calendars d ON d.id=m.destination "+OWNER+"""
            WHERE d.provisioning='READY' AND m.revision>m.applied_revision AND m.attempts<8
            AND m.next_attempt<=now() AND (m.lease_until IS NULL OR m.lease_until<now())
            ORDER BY m.next_attempt,m.destination,m.session_id LIMIT 1 FOR UPDATE OF m SKIP LOCKED
            """,(r,n)->new Job(destination(r),r.getLong("session_id"),r.getString("event_id"),r.getLong("revision"),
                UUID.randomUUID(),r.getString("desired"),r.getInt("job_attempts")+1));
        if(rows.isEmpty())return null;
        var j=rows.get(0);
        sql().update("UPDATE user_session_calendar_events SET lease=?,lease_until=now()+interval '2 minutes',attempts=attempts+1 WHERE destination=? AND session_id=?",j.lease(),j.dest().id(),j.session());
        return j;
    }
    public boolean current(Job j) {
        return Boolean.TRUE.equals(sql().queryForObject("SELECT EXISTS(SELECT FROM user_session_calendar_events m JOIN personal_google_calendars d ON d.id=m.destination "+OWNER+"""
            LEFT JOIN calendar_event_target v ON v.destination=m.destination AND v.session_id=m.session_id
            WHERE m.destination=? AND m.session_id=? AND g.version=? AND m.lease=? AND m.lease_until>now()
              AND m.revision=? AND m.desired IS NOT DISTINCT FROM v.desired
              AND (NOT coalesce(jsonb_exists(m.desired,'attendees'),false) OR
                   EXISTS(SELECT FROM sessions s WHERE s.id=m.session_id AND s.status='SCHEDULED' AND s.scheduled_start_at>now())))
            """,Boolean.class,j.dest().id(),j.session(),j.dest().version(),j.lease(),j.revision()));
    }
    public boolean markInvitationInsert(Job j) {
        return sql().update("""
            UPDATE user_session_calendar_events m SET invitation_insert_uncertain=true FROM calendar_event_target v
            WHERE m.destination=? AND m.session_id=? AND m.lease=? AND m.revision=? AND m.lease_until>now()
              AND v.destination=m.destination AND v.session_id=m.session_id AND v.desired=m.desired
            """,j.dest().id(),j.session(),j.lease(),j.revision())==1;
    }
    public boolean uncertainInsert(Job j) {
        return Boolean.TRUE.equals(sql().queryForObject("""
            SELECT EXISTS(SELECT FROM user_session_calendar_events WHERE destination=? AND session_id=?
            AND invitation_insert_uncertain)
            """,Boolean.class,j.dest().id(),j.session()));
    }
    public void replaceRemovedEvent(Job j) {
        sql().update("""
            UPDATE user_session_calendar_events SET event_id=replace(gen_random_uuid()::text,'-',''),
                revision=revision+1,lease=NULL,lease_until=NULL,next_attempt=now(),failure=NULL
            WHERE destination=? AND session_id=? AND revision=? AND lease=? AND wanted AND attempts<8
            """,j.dest().id(),j.session(),j.revision(),j.lease());
    }

    public void missingCalendar(Job j) {
        sql().update("UPDATE personal_google_calendars SET provisioning='UNCERTAIN' WHERE id=? AND calendar_id=? AND provisioning='READY'",j.dest().id(),j.dest().calendar());
        finish(j,"CALENDAR_MISSING",false,0);
    }

    public void finish(Job j,String failure,boolean retry,long delay) {
        int updated=sql().update("""
            UPDATE user_session_calendar_events SET lease=NULL,lease_until=NULL,
                invitation_insert_uncertain=CASE WHEN ? THEN false ELSE invitation_insert_uncertain END,
                applied_revision=CASE WHEN ? THEN revision ELSE applied_revision END,
                last_success=CASE WHEN ? THEN now() ELSE last_success END,failure=?,
                attempts=CASE WHEN ? THEN 0 WHEN ? THEN attempts ELSE 8 END,
                next_attempt=now()+(? * interval '1 second')
            WHERE destination=? AND session_id=? AND lease=? AND revision=?
            """,failure==null,failure==null,failure==null,failure,failure==null,retry,delay,j.dest().id(),j.session(),j.lease(),j.revision());
        // A superseded request may already have reached Google. Reconcile again even if
        // the newer worker completed first, retaining the same remote event identity.
        if(updated==0)sql().update("""
            UPDATE user_session_calendar_events SET revision=revision+1,attempts=0,next_attempt=now()
            WHERE destination=? AND session_id=? AND revision=applied_revision
            """,j.dest().id(),j.session());
    }
}
