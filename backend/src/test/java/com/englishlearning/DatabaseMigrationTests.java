package com.englishlearning;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "databaseMigration", matches = "true")
class DatabaseMigrationTests {

    private static final Set<String> TABLES = Set.of(
            "users", "refresh_sessions", "email_verification_credentials", "password_reset_credentials",
            "teacher_applications", "teacher_profiles", "course_categories", "courses",
            "course_schedule_rules", "sessions", "session_schedule_changes", "session_calendar_events",
            "enrollments", "assignments", "assignment_attachments", "submissions", "submission_attachments",
            "participant_feedback", "teacher_payment_accounts", "payments", "payment_transactions", "refunds", "teacher_certificates");

    @Test
    void migratesEmptyLocalDatabaseAndRestartsWithoutReapplyingMigrations() throws Exception {
        String url = System.getenv("DATABASE_URL");
        // Fail before connecting or starting Spring. Never run this against managed/shared databases.
        assertNotNull(url, "Supply an empty disposable local task004_* database");
        assertTrue(url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/task004_[a-z0-9_]+")
                        || url.equals("jdbc:postgresql://127.0.0.1:55432/task046_certificates_v2_rerun"),
                "Only allowlisted disposable loopback databases are permitted");
        String user = System.getenv("DATABASE_USERNAME");
        String password = System.getenv("DATABASE_PASSWORD");
        assertNotNull(user);
        assertNotNull(password);
        try (var connection = DriverManager.getConnection(url, user, password)) {
            assertTrue(strings(connection, """
                    SELECT tablename FROM pg_tables
                    WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
                    """).isEmpty(), "Database must be empty; no cleaning or baselining is permitted");
        }

        for (int run = 0; run < 2; run++) {
            try (var context = new SpringApplicationBuilder(EnglishLearningApplication.class)
                    .web(WebApplicationType.NONE)
                    .run("--spring.datasource.url=" + url, "--spring.datasource.username=" + user,
                            "--spring.datasource.password=" + password, "--spring.flyway.enabled=true",
                            "--spring.flyway.clean-disabled=true", "--spring.flyway.baseline-on-migrate=false",
                            "--spring.jpa.hibernate.ddl-auto=none", "--spring.sql.init.mode=never");
                 var connection = DriverManager.getConnection(url, user, password)) {
                var flyway = context.getBean(Flyway.class);
                flyway.validate();
                assertEquals("2", flyway.info().current().getVersion().getVersion());
                assertEquals(0, flyway.migrate().migrationsExecuted, "V1 and V2 must not run twice");
                assertEquals(TABLES, strings(connection, """
                        SELECT tablename FROM pg_tables
                        WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history'
                        """));
                assertEquals(Set.of("1:1:V1__initial_schema.sql:true", "2:2:V2__create_teacher_certificates.sql:true"), strings(connection,
                        "SELECT installed_rank || ':' || version || ':' || script || ':' || success FROM public.flyway_schema_history"));
                assertEquals(Set.of("23"), strings(connection, """
                        SELECT count(*)::text FROM pg_constraint
                        WHERE connamespace = 'public'::regnamespace AND contype = 'p'
                          AND conrelid <> 'public.flyway_schema_history'::regclass
                        """));
                assertEquals(Set.of("3"), strings(connection, """
                        SELECT count(*)::text FROM pg_constraint
                        WHERE connamespace = 'public'::regnamespace AND contype = 'f' AND confdeltype = 'c'
                        """));
                assertEquals(Set.of("26"), strings(connection, """
                        SELECT count(*)::text FROM pg_constraint
                        WHERE connamespace = 'public'::regnamespace AND contype = 'f' AND confdeltype = 'r'
                        """));
                assertTrue(strings(connection, """
                        SELECT indexname FROM pg_indexes WHERE schemaname = 'public'
                        """).containsAll(Set.of("uq_sessions_course_number", "uq_teacher_applications_one_pending",
                        "uq_teacher_payment_accounts_one_active", "uq_payments_one_pending_per_enrollment",
                        "uq_session_calendar_events_external")));
                verifyCertificateSchema(connection);
                if (run == 0) {
                    verifyConstraintsWithoutLeavingData(connection);
                    verifyCertificatesWithoutLeavingData(connection);
                }
                assertEquals(Set.of("0"), strings(connection, "SELECT count(*)::text FROM public.users"));
            }
        }
    }

    private static void verifyCertificateSchema(Connection c) throws SQLException {
        assertEquals(Set.of("id:bigint:NO:YES", "teacher_id:bigint:NO:NO", "name:text:NO:NO",
                "file_path:text:NO:NO", "status:text:NO:NO", "created_at:timestamp with time zone:NO:NO",
                "updated_at:timestamp with time zone:NO:NO"), strings(c, """
                SELECT column_name || ':' || data_type || ':' || is_nullable || ':' || is_identity
                FROM information_schema.columns
                WHERE table_schema='public' AND table_name='teacher_certificates'
                """));
        assertEquals(Set.of("id"), strings(c, """
                SELECT a.attname FROM pg_constraint k
                JOIN pg_attribute a ON a.attrelid=k.conrelid AND a.attnum=ANY(k.conkey)
                WHERE k.conrelid='public.teacher_certificates'::regclass AND k.contype='p'
                  AND k.conname='pk_teacher_certificates'
                """));
        assertEquals(Set.of("teacher_id:teacher_id:r"), strings(c, """
                SELECT a.attname || ':' || b.attname || ':' || k.confdeltype::text
                FROM pg_constraint k
                JOIN pg_attribute a ON a.attrelid=k.conrelid AND a.attnum=k.conkey[1]
                JOIN pg_attribute b ON b.attrelid=k.confrelid AND b.attnum=k.confkey[1]
                WHERE k.conrelid='public.teacher_certificates'::regclass AND k.contype='f'
                  AND k.confrelid='public.teacher_profiles'::regclass
                  AND cardinality(k.conkey)=1
                  AND k.conname='fk_teacher_certificates_teacher_profile'
                """));
        assertEquals(Set.of("ix_teacher_certificates_teacher:teacher_id",
                "ix_teacher_certificates_status:status"), strings(c, """
                SELECT n.relname || ':' || a.attname FROM pg_index i
                JOIN pg_class n ON n.oid=i.indexrelid
                JOIN pg_attribute a ON a.attrelid=i.indrelid AND a.attnum=i.indkey[0]
                WHERE i.indrelid='public.teacher_certificates'::regclass
                  AND NOT i.indisprimary AND i.indisvalid AND i.indisready AND i.indnatts=1
                """));
    }

    private static void verifyCertificatesWithoutLeavingData(Connection c) throws SQLException {
        c.setAutoCommit(false);
        try {
            execute(c, """
                    INSERT INTO public.users(id,email,password_hash,full_name,role,created_at,updated_at)
                    VALUES (10,'certificate@example.test','test-only','Teacher','TEACHER',now(),now());
                    INSERT INTO public.teacher_profiles(teacher_id,specialization,teaching_experience,
                        introduction,created_at,updated_at)
                    VALUES (10,'Test','Test','Test',now(),now());
                    INSERT INTO public.teacher_certificates(teacher_id,name,file_path,status,created_at,updated_at)
                    VALUES (10,'Test','test/evidence.pdf','PENDING',now(),now());
                    """);
            for (String status : Set.of("PENDING", "VERIFIED", "REJECTED")) {
                execute(c, "UPDATE public.teacher_certificates SET status='" + status + "'");
                assertEquals(Set.of(status), strings(c, "SELECT status FROM public.teacher_certificates"));
            }
            for (String column : Set.of("id", "teacher_id", "name", "file_path", "status", "created_at", "updated_at")) {
                reject(c, "UPDATE public.teacher_certificates SET " + column + "=NULL", "23502", column);
            }
            reject(c, "UPDATE public.teacher_certificates SET status='APPROVED'", "23514", "ck_teacher_certificates_status");
            reject(c, "UPDATE public.teacher_certificates SET teacher_id=999", "23503", "fk_teacher_certificates_teacher_profile");
            reject(c, "DELETE FROM public.teacher_profiles WHERE teacher_id=10", "23001", "fk_teacher_certificates_teacher_profile");
        } finally {
            c.rollback();
            c.setAutoCommit(true);
        }
        assertEquals(Set.of("0"), strings(c, "SELECT count(*)::text FROM public.teacher_certificates"));
    }

    private static void verifyConstraintsWithoutLeavingData(Connection c) throws SQLException {
        c.setAutoCommit(false);
        try {
            execute(c, """
                    INSERT INTO public.users(id,email,password_hash,full_name,role,created_at,updated_at)
                    VALUES (1,'teacher@example.test','test-only','Teacher','TEACHER',now(),now()),
                           (2,'student@example.test','test-only','Student','STUDENT',now(),now()),
                           (3,'admin@example.test','test-only','Admin','ADMIN',now(),now());
                    INSERT INTO public.course_categories(id,name,created_at,updated_at)
                    VALUES (1,'Test',now(),now());
                    INSERT INTO public.courses(id,teacher_id,category_id,name,description,tuition,currency,
                        min_students,max_students,planned_start_date,timezone,session_count,status,created_at,updated_at)
                    VALUES (1,1,1,'Test','Test',100,'VND',1,10,current_date,'Asia/Ho_Chi_Minh',1,'DRAFT',now(),now());
                    INSERT INTO public.sessions(id,course_id,session_number,scheduled_start_at,scheduled_end_at,
                        status,created_at,updated_at)
                    VALUES (1,1,1,now(),now()+interval '1 hour','SCHEDULED',now(),now());
                    INSERT INTO public.assignments(id,session_id,title,max_score,due_at,status,created_at,updated_at)
                    VALUES (1,1,'Test',150,now()+interval '1 day','ACTIVE',now(),now());
                    INSERT INTO public.submissions(id,assignment_id,student_id,status,created_at,updated_at)
                    VALUES (1,1,2,'DRAFT',now(),now());
                    INSERT INTO public.enrollments(id,student_id,course_id,status,created_at,updated_at)
                    VALUES (1,2,1,'PENDING',now(),now());
                    INSERT INTO public.teacher_payment_accounts(id,teacher_id,bank_code,account_number,
                        account_holder_name,provider,status,created_at,updated_at)
                    VALUES (1,1,'TEST','TEST','Test','VIETQR','ACTIVE',now(),now());
                    INSERT INTO public.payments(id,enrollment_id,teacher_payment_account_id,payment_code,
                        provider,original_amount,discount_amount,final_amount,currency,status,
                        expires_at,created_at,updated_at)
                    VALUES (1,1,1,'TEST','VIETQR',100,0,100,'VND','PENDING',now()+interval '1 hour',now(),now());
                    INSERT INTO public.refunds(id,payment_id,amount,reason,status,recipient_bank_code,
                        recipient_account_number,recipient_account_holder,requested_at,created_at,updated_at)
                    VALUES (1,1,100,'Test','PENDING','TEST','TEST','Test',now(),now(),now());
                    """);
            reject(c, "UPDATE public.users SET email=' Teacher@example.test ' WHERE id=1", "23514", "ck_users_email_canonical");
            reject(c, "UPDATE public.users SET email='student@example.test' WHERE id=1", "23505", "uq_users_email");
            reject(c, "UPDATE public.courses SET tuition='NaN' WHERE id=1", "23514", "ck_courses_tuition");
            reject(c, "UPDATE public.courses SET currency='USD' WHERE id=1", "23514", "ck_courses_currency");
            reject(c, "UPDATE public.courses SET min_students=11 WHERE id=1", "23514", "ck_courses_capacity");
            reject(c, "UPDATE public.courses SET discount_percent=10 WHERE id=1", "23514", "ck_courses_discount_fields");
            reject(c, "UPDATE public.assignments SET max_score='NaN' WHERE id=1", "23514", "ck_assignments_max_score");
            reject(c, "UPDATE public.submissions SET score='NaN' WHERE id=1", "23514", "ck_submissions_score");
            reject(c, "UPDATE public.submissions SET status='SUBMITTED' WHERE id=1", "23514", "ck_submissions_submission_evidence");
            reject(c, "UPDATE public.payments SET status='CONFIRMED' WHERE id=1", "23514", "ck_payments_confirmation_evidence");
            reject(c, "UPDATE public.refunds SET status='SUBMITTED' WHERE id=1", "23514", "ck_refunds_submission_evidence");
            reject(c, "UPDATE public.courses SET teacher_id=999 WHERE id=1", "23503", "fk_courses_teacher");
            reject(c, "DELETE FROM public.courses WHERE id=1", "23001", "fk_sessions_course");
            reject(c, """
                    INSERT INTO public.sessions(id,course_id,session_number,scheduled_start_at,scheduled_end_at,
                        status,created_at,updated_at)
                    VALUES (2,1,1,now(),now()+interval '1 hour','SCHEDULED',now(),now())
                    """, "23505", "uq_sessions_course_number");
            // GRADED does not force a numeric score; cross-row/business policies belong to later tasks.
            execute(c, """
                    UPDATE public.submissions SET status='GRADED', submitted_at=now(),
                        graded_at=now(), graded_by=1 WHERE id=1;
                    UPDATE public.sessions SET status='CANCELLED' WHERE id=1;
                    """);
            assertEquals(Set.of("1"), strings(c, "SELECT session_count::text FROM public.courses WHERE id=1"));
        } finally {
            c.rollback();
            c.setAutoCommit(true);
        }
    }

    private static void reject(Connection c, String sql, String state, String constraint) throws SQLException {
        var savepoint = c.setSavepoint();
        try {
            SQLException error = assertThrows(SQLException.class, () -> execute(c, sql));
            assertEquals(state, error.getSQLState());
            assertTrue(error.getMessage().contains(constraint), "Expected constraint: " + constraint);
        } finally {
            c.rollback(savepoint);
            c.releaseSavepoint(savepoint);
        }
    }

    private static void execute(Connection c, String sql) throws SQLException {
        try (var statement = c.createStatement()) {
            statement.setQueryTimeout(15);
            statement.execute(sql);
        }
    }

    private static Set<String> strings(Connection c, String sql) throws SQLException {
        try (var statement = c.createStatement()) {
            statement.setQueryTimeout(15);
            try (var rows = statement.executeQuery(sql)) {
                var values = new HashSet<String>();
                while (rows.next()) {
                    values.add(rows.getString(1));
                }
                return values;
            }
        }
    }
}
