package com.englishlearning.features.reminder;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@ConditionalOnProperty(name="APP_SESSION_REMINDERS_ENABLED", havingValue="true")
public class SessionReminderWorker {
    private static final String SAME_TARGET = """
            t.session_id=r.session_id AND t.student_id=r.student_id
            AND t.starts_at=r.starts_at AND t.schedule_version=r.schedule_version
            """;
    private final JdbcTemplate db;
    private final JavaMailSender smtp;
    private final String from;
    private final URI schedule;

    public SessionReminderWorker(JdbcTemplate db, JavaMailSender smtp, @Value("${SMTP_FROM}") String from,
            @Value("${APP_SESSION_REMINDER_SITE_ORIGIN}") URI origin) {
        this.db = db;
        this.smtp = smtp;
        this.from = from;
        if (from.isBlank() || from.chars().anyMatch(Character::isISOControl)
                || origin.getHost() == null || origin.getUserInfo() != null || origin.getQuery() != null
                || origin.getFragment() != null || !List.of("", "/").contains(origin.getPath())
                || !("https".equals(origin.getScheme()) || ("http".equals(origin.getScheme())
                    && List.of("localhost","127.0.0.1").contains(origin.getHost())))) {
            throw new IllegalArgumentException("Invalid reminder mail configuration");
        }
        schedule = origin.resolve("/student/dashboard/schedule");
    }

    @Scheduled(fixedDelayString="${app.session-reminders.delay-ms:15000}")
    public void tick() {
        reconcile();
        for (int i = 0; i < 50 && deliverOne(); i++) {
            // Bound each pass so eligibility is reconciled regularly.
        }
    }

    public void reconcile() {
        db.update("""
                WITH stale AS (SELECT id FROM student_session_reminders
                  WHERE state='SENDING' AND attempted_at < now()-interval '5 minutes'
                  ORDER BY attempted_at,id LIMIT 250 FOR UPDATE SKIP LOCKED)
                UPDATE student_session_reminders r SET state='UNKNOWN', finished_at=now()
                FROM stale WHERE r.id=stale.id
                """);
        db.update("""
                WITH invalid AS (SELECT r.id FROM student_session_reminders r
                WHERE state='PENDING' AND (starts_at < now()+interval '10 minutes'
                OR NOT EXISTS (SELECT FROM student_reminder_targets t WHERE
                """ + SAME_TARGET + """
                )) ORDER BY r.next_attempt,r.id LIMIT 250 FOR UPDATE OF r SKIP LOCKED)
                UPDATE student_session_reminders r SET state='CANCELLED', finished_at=now()
                FROM invalid WHERE r.id=invalid.id
                """);
        db.update("""
                INSERT INTO student_session_reminders AS r(session_id,student_id,starts_at,schedule_version,due_at,next_attempt)
                SELECT t.session_id, t.student_id, t.starts_at, t.schedule_version, t.starts_at-interval '15 minutes',
                       t.starts_at-interval '15 minutes' FROM student_reminder_targets t
                WHERE t.starts_at BETWEEN now()+interval '10 minutes' AND now()+interval '1 day'
                AND NOT EXISTS (SELECT FROM student_session_reminders existing WHERE existing.session_id=t.session_id
                  AND existing.student_id=t.student_id AND existing.starts_at=t.starts_at
                  AND existing.schedule_version=t.schedule_version
                  AND (existing.state<>'CANCELLED' OR existing.attempts>=3))
                ORDER BY t.starts_at,t.session_id,t.student_id LIMIT 250
                ON CONFLICT (session_id,student_id,starts_at,schedule_version) DO UPDATE
                SET state='PENDING', finished_at=NULL WHERE r.state='CANCELLED' AND r.attempts<3
                """);
    }

    // Autocommit the claim before SMTP. A crashed sender must never be replayed automatically.
    public boolean deliverOne() {
        var ids = db.queryForList("""
                WITH candidate AS (SELECT id FROM student_session_reminders
                  WHERE state='PENDING' AND next_attempt<=now() AND starts_at>=now()+interval '10 minutes'
                  ORDER BY next_attempt,id LIMIT 1 FOR UPDATE SKIP LOCKED)
                UPDATE student_session_reminders r SET state='SENDING',attempts=attempts+1,attempted_at=now()
                FROM candidate WHERE r.id=candidate.id RETURNING r.id
                """, Long.class);
        if (ids.isEmpty()) return false;
        long id = ids.get(0);
        var targets = db.query("""
                SELECT t.email,t.full_name,t.course_name,t.session_number,t.timezone,t.starts_at
                FROM student_session_reminders r JOIN student_reminder_targets t ON
                """ + SAME_TARGET + " WHERE r.id=? AND r.state='SENDING' AND r.starts_at>=now()+interval '10 minutes'",
                (rs,n) -> new Recipient(rs.getString(1),rs.getString(2),rs.getString(3),
                        rs.getInt(4),rs.getString(5),rs.getTimestamp(6).toInstant()), id);
        if (targets.isEmpty()) {
            finish(id,"CANCELLED");
            return true;
        }
        var message = message(targets.get(0));
        try {
            smtp.send(message);
        } catch (MailAuthenticationException rejectedBeforeSubmission) {
            db.update("""
                    UPDATE student_session_reminders SET state=CASE WHEN attempts<3 THEN 'PENDING' ELSE 'FAILED' END,
                    next_attempt=now()+attempts*interval '30 seconds',
                    finished_at=CASE WHEN attempts=3 THEN now() END WHERE id=? AND state='SENDING'
                    """,id);
            return true;
        } catch (RuntimeException uncertainDelivery) {
            finish(id,"UNKNOWN");
            return true;
        }
        finish(id,"SENT");
        return true;
    }

    record Recipient(String email,String name,String course,int number,String timezone,Instant start) {}

    private static String field(String value) {
        return value.replaceAll("[\\p{Cntrl}\\p{Zl}\\p{Zp}]"," ").replaceAll("(?i)(?:https?://|www\\.|meet\\.google\\.com)\\S*", "[liên kết]");
    }

    private SimpleMailMessage message(Recipient recipient) {
        String course = field(recipient.course());
        String time = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy (VV)")
                .withZone(ZoneId.of(recipient.timezone())).format(recipient.start());
        var mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(recipient.email());
        mail.setSubject("Nhắc lịch học: " + course + " — Buổi " + recipient.number());
        mail.setText("""
                Xin chào %s,

                Buổi học của bạn sẽ bắt đầu sau khoảng 15 phút.
                Khóa học: %s
                Buổi học: %d
                Thời gian: %s

                Vào website để xem lịch và tham gia Google Meet:
                %s

                Chúc bạn học tốt!
                """.formatted(field(recipient.name()),course,recipient.number(),time,schedule));
        return mail;
    }

    private void finish(long id,String state) {
        db.update("UPDATE student_session_reminders SET state=?,finished_at=now() WHERE id=? AND state='SENDING'",state,id);
    }
}
