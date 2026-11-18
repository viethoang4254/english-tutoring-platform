package com.englishlearning.features.enrollment;

import com.englishlearning.EnglishLearningApplication;
import com.englishlearning.features.enrollment.service.*;
import com.englishlearning.features.course.service.CourseService;
import com.englishlearning.features.course.CourseInputTests;
import com.englishlearning.features.category.service.CategoryService;
import java.time.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="enrollmentPersistence",matches="true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnrollmentPersistenceTests {
    String url,user,password;ConfigurableApplicationContext context;EnrollmentService enrollments;
    long admin,teacher,otherTeacher,student,other,course;Instant first;
    Connection connect() throws Exception{return DriverManager.getConnection(url,user,password);}
    void sql(String value)throws Exception{try(var c=connect();var s=c.createStatement()){s.execute(value);}}
    long count(String value)throws Exception{try(var c=connect();var s=c.createStatement();var r=s.executeQuery(value)){r.next();return r.getLong(1);}}
    @BeforeAll void start()throws Exception {
        url=System.getenv("ENROLLMENT_TEST_URL");user=System.getenv("ENROLLMENT_TEST_USER");password=System.getenv("ENROLLMENT_TEST_PASSWORD");
        assertEquals("jdbc:postgresql://127.0.0.1:15440/task016_enrollments",url);assertEquals("task016_test",user);
        assertEquals(0,count("SELECT count(*) FROM pg_tables WHERE schemaname NOT IN ('pg_catalog','information_schema')"));
        var flyway=Flyway.configure().dataSource(url,user,password).cleanDisabled(true).load();flyway.migrate();flyway.validate();
        assertEquals(15,count("SELECT count(*) FROM flyway_schema_history WHERE success"));
        var props=new HashMap<String,Object>();props.put("spring.datasource.url",url);props.put("spring.datasource.username",user);props.put("spring.datasource.password",password);
        props.put("spring.flyway.enabled",false);props.put("spring.jpa.hibernate.ddl-auto","validate");props.put("spring.sql.init.mode","never");
        props.put("spring.datasource.hikari.maximum-pool-size",4);props.put("spring.datasource.hikari.connection-init-sql","SET lock_timeout TO '5s'");
        byte[] key=new byte[32];new java.security.SecureRandom().nextBytes(key);props.put("app.security.password-reset.hmac-secret",Base64.getEncoder().encodeToString(key));
        context=new SpringApplicationBuilder(EnglishLearningApplication.class).web(WebApplicationType.NONE)
            .initializers(c->c.getEnvironment().getPropertySources().addFirst(new MapPropertySource("isolated-enrollment-test",props))).run();
        enrollments=context.getBean(EnrollmentService.class);
        sql("INSERT INTO users(email,password_hash,full_name,role,email_verified,locked,created_at,updated_at) VALUES ('admin@example.invalid','unused','Admin','ADMIN',true,false,now(),now()),('teacher@example.invalid','unused','Teacher','TEACHER',true,false,now(),now()),('student@example.invalid','unused','Student','STUDENT',false,false,now(),now()),('other@example.invalid','unused','Other','STUDENT',true,false,now(),now())");
        admin=count("SELECT id FROM users WHERE role='ADMIN'");teacher=count("SELECT id FROM users WHERE email='teacher@example.invalid'");
        student=count("SELECT id FROM users WHERE email='student@example.invalid'");other=count("SELECT id FROM users WHERE email='other@example.invalid'");
        sql("INSERT INTO users(email,password_hash,full_name,role,email_verified,locked,created_at,updated_at) VALUES ('other-teacher@example.invalid','unused','Other Teacher','TEACHER',true,false,now(),now())");
        otherTeacher=count("SELECT id FROM users WHERE email='other-teacher@example.invalid'");
        sql("INSERT INTO teacher_applications(teacher_id,specialization,teaching_experience,introduction,status,submitted_at,reviewed_at,reviewed_by,created_at,updated_at) VALUES ("+teacher+",'Fixture','Fixture','Fixture','APPROVED',now(),now(),"+admin+",now(),now())");
        sql("INSERT INTO teacher_applications(teacher_id,specialization,teaching_experience,introduction,status,submitted_at,reviewed_at,reviewed_by,created_at,updated_at) VALUES ("+otherTeacher+",'Fixture','Fixture','Fixture','APPROVED',now(),now(),"+admin+",now(),now())");
    }
    @AfterAll void close(){if(context!=null)context.close();}
    @BeforeEach void fixture()throws Exception {
        sql("DELETE FROM enrollments");sql("DELETE FROM sessions");sql("DELETE FROM courses");sql("DELETE FROM course_categories");
        long category=context.getBean(CategoryService.class).create(admin,Map.of("name","Enrollment fixture")).id().value();
        var fields=CourseInputTests.fields();fields.put("categoryId",Long.toString(category));fields.put("maxStudents",1);fields.put("sessionCount",1);
        course=context.getBean(CourseService.class).create(teacher,fields).id().value();first=Instant.now().plusSeconds(600).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        sql("INSERT INTO sessions(course_id,session_number,scheduled_start_at,scheduled_end_at,status,created_at,updated_at) VALUES ("+course+",1,'"+first+"','"+first.plusSeconds(3600)+"','SCHEDULED',now(),now())");
        sql("UPDATE courses SET status='PUBLISHED' WHERE id="+course);
    }
    @Test void adminDirectoryFiltersLocksAndPreservesTeacherApproval() {
        var management=context.getBean(com.englishlearning.features.user.service.AdminUserService.class);
        new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).executeWithoutResult(tx->{
            tx.setRollbackOnly();
            var listed=management.list(admin,0,1,"example.invalid","STUDENT",false);
            assertEquals(1,listed.items().size());assertTrue(listed.hasNext());
            assertEquals(student,management.list(admin,0,20,"student@example.invalid",null,null).items().get(0).id().value());
            assertTrue(management.changeLock(admin,student,true).locked());
            assertEquals(1,management.list(admin,0,20,"student@example.invalid",null,true).items().size());
            assertFalse(management.changeLock(admin,student,false).locked());
            assertThrows(com.englishlearning.features.user.service.AdminUserService.Failure.class,()->management.changeLock(admin,admin,true));
            var reviews=context.getBean(com.englishlearning.features.teacher.service.TeacherApplicationReviewService.class);
            assertEquals(2,reviews.list(admin,0,20,"APPROVED").items().size());
            assertTrue(reviews.list(admin,0,20,"REJECTED").items().isEmpty());
        });
    }
    @Test void adminDeletionPreservesBusinessHistoryAndRemovesOnlyDisposableAccount()throws Exception {
        var management=context.getBean(com.englishlearning.features.user.service.AdminUserService.class);
        var current=context.getBean(com.englishlearning.features.user.service.CurrentAccountService.class);
        sql("INSERT INTO users(email,password_hash,full_name,role,email_verified,locked,created_at,updated_at) VALUES ('delete-only@example.invalid','unused','Disposable','STUDENT',false,false,now(),now())");
        long disposable=count("SELECT id FROM users WHERE email='delete-only@example.invalid'");
        assertEquals("Updated",management.updateName(admin,disposable,Map.of("fullName","Updated")).fullName());
        sql("INSERT INTO refresh_sessions(user_id,token_hash,expires_at,created_at) VALUES ("+disposable+",'disposable-refresh-fixture',now()+interval '1 hour',now())");
        sql("INSERT INTO email_verification_credentials(user_id,purpose,target_email,secret_hash,expires_at,created_at) VALUES ("+disposable+",'INITIAL_VERIFICATION','delete-only@example.invalid','disposable-verification-fixture',now()+interval '1 hour',now())");
        sql("INSERT INTO password_reset_credentials(user_id,secret_hash,expires_at,created_at) VALUES ("+disposable+",'disposable-reset-fixture',now()+interval '1 hour',now())");
        management.delete(admin,disposable);
        for(String table:List.of("refresh_sessions","email_verification_credentials","password_reset_credentials"))assertEquals(0,count("SELECT count(*) FROM "+table+" WHERE user_id="+disposable));
        var jwt=org.springframework.security.oauth2.jwt.Jwt.withTokenValue("synthetic").header("alg","RS256").subject(Long.toString(disposable)).build();
        assertThrows(org.springframework.security.authentication.BadCredentialsException.class,()->context.getBean(com.englishlearning.features.auth.security.CurrentAccountAuthenticationConverter.class).convert(jwt));
        assertEquals(0,count("SELECT count(*) FROM users WHERE id="+disposable));
        assertTrue(current.findById(disposable).isEmpty());
        assertEquals(403,assertThrows(com.englishlearning.features.user.service.AdminUserService.Failure.class,()->management.delete(admin,admin)).status);
        assertEquals(409,assertThrows(com.englishlearning.features.user.service.AdminUserService.Failure.class,()->management.delete(admin,teacher)).status);
        var enrolled=enrollments.create(student,course).enrollment();
        assertEquals(409,assertThrows(com.englishlearning.features.user.service.AdminUserService.Failure.class,()->management.delete(admin,student)).status);
        assertEquals(enrolled,enrollments.get(student,enrolled.id().value()));
        assertEquals(1,count("SELECT count(*) FROM courses WHERE id="+course));
    }
    @Test void adminDeletionRejectsCascadeReminderAndNonForeignKeyOutbox() {
        var management=context.getBean(com.englishlearning.features.user.service.AdminUserService.class);
        new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).executeWithoutResult(tx->{
            tx.setRollbackOnly();var em=java.util.Objects.requireNonNull(org.springframework.orm.jpa.EntityManagerFactoryUtils.getTransactionalEntityManager(context.getBean(jakarta.persistence.EntityManagerFactory.class)));
            em.createNativeQuery("INSERT INTO student_session_reminders(session_id,student_id,starts_at,schedule_version,due_at,next_attempt) SELECT id,:student,scheduled_start_at,0,now(),now() FROM sessions WHERE course_id=:course").setParameter("student",other).setParameter("course",course).executeUpdate();
            assertEquals(409,assertThrows(com.englishlearning.features.user.service.AdminUserService.Failure.class,()->management.delete(admin,other)).status);
            assertEquals(1,((Number)em.createNativeQuery("SELECT count(*) FROM student_session_reminders WHERE student_id=:student").setParameter("student",other).getSingleResult()).intValue());
            em.createNativeQuery("INSERT INTO calendar_sync_changes(entity,entity_id) VALUES ('user',:student)").setParameter("student",student).executeUpdate();
            assertEquals(409,assertThrows(com.englishlearning.features.user.service.AdminUserService.Failure.class,()->management.delete(admin,student)).status);
        });
    }
    @Test void pendingDuplicateCancellationAndPrivacy()throws Exception {
        var e=enrollments.create(student,course).enrollment();assertEquals("PENDING",e.status());assertEquals(first,e.seatReservedUntil());
        assertEquals(e,enrollments.create(student,course).enrollment());assertEquals(1,count("SELECT count(*) FROM enrollments"));
        assertThrows(EnrollmentFailure.class,()->enrollments.get(other,e.id().value()));
        assertThrows(EnrollmentFailure.class,()->enrollments.cancel(other,e.id().value()));
        assertEquals(1,enrollments.roster(teacher,course,0,20,null).items().size());
        var cancelled=enrollments.cancel(student,e.id().value());assertEquals("CANCELLED",cancelled.status());assertEquals(cancelled,enrollments.cancel(student,e.id().value()));
        assertThrows(EnrollmentFailure.class,()->enrollments.create(student,course));assertEquals(0,count("SELECT count(*) FROM payments"));
    }
    @Test void expiryReleasesCapacityAndRenewalRechecks()throws Exception {
        var e=enrollments.create(student,course).enrollment();sql("UPDATE enrollments SET seat_reserved_until=now()-interval '1 second' WHERE id="+e.id().value());
        assertTrue(enrollments.get(student,e.id().value()).reservationExpired());
        assertEquals(EnrollmentFailure.Reason.ENROLLMENT_RENEWAL_REQUIRED,assertThrows(EnrollmentFailure.class,()->enrollments.create(student,course)).reason());
        var second=enrollments.create(other,course).enrollment();
        assertEquals(EnrollmentFailure.Reason.COURSE_FULL,assertThrows(EnrollmentFailure.class,()->enrollments.renew(student,e.id().value())).reason());
        enrollments.cancel(other,second.id().value());assertFalse(enrollments.renew(student,e.id().value()).reservationExpired());
    }
    @Test void concurrentLastSeatAndDuplicateAreSerialized()throws Exception {
        var pool=Executors.newFixedThreadPool(2);
        try {
            var start=new CountDownLatch(1);var a=pool.submit(()->attempt(student,start));var b=pool.submit(()->attempt(other,start));start.countDown();
            assertEquals(1,(a.get(15,TimeUnit.SECONDS)?1:0)+(b.get(15,TimeUnit.SECONDS)?1:0));assertEquals(1,count("SELECT count(*) FROM enrollments"));
            sql("DELETE FROM enrollments");var again=new CountDownLatch(1);
            var x=pool.submit(()->{again.await();return enrollments.create(student,course).enrollment();});
            var y=pool.submit(()->{again.await();return enrollments.create(student,course).enrollment();});again.countDown();
            assertEquals(x.get(15,TimeUnit.SECONDS),y.get(15,TimeUnit.SECONDS));assertEquals(1,count("SELECT count(*) FROM enrollments"));
        } finally {pool.shutdownNow();}
    }
    boolean attempt(long actor,CountDownLatch ready)throws Exception {
        ready.await();try{enrollments.create(actor,course);return true;}catch(EnrollmentFailure e){assertEquals(EnrollmentFailure.Reason.COURSE_FULL,e.reason());return false;}
    }
    @Test void rollbackAndCutoffPreserveState()throws Exception {
        var tx=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        assertThrows(IllegalStateException.class,()->tx.execute(s->{enrollments.create(student,course);throw new IllegalStateException("Fixture rollback");}));
        assertEquals(0,count("SELECT count(*) FROM enrollments"));
        sql("UPDATE sessions SET scheduled_start_at=now()-interval '1 second' WHERE course_id="+course);
        assertEquals(EnrollmentFailure.Reason.COURSE_NOT_ENROLLABLE,assertThrows(EnrollmentFailure.class,()->enrollments.create(student,course)).reason());
    }
    @Test void paginationFilteringAndRosterIsolation()throws Exception {
        sql("UPDATE courses SET max_students=2 WHERE id="+course);
        var a=enrollments.create(student,course).enrollment();var b=enrollments.create(other,course).enrollment();
        var page=enrollments.roster(teacher,course,0,1,"PENDING");assertTrue(page.hasNext());assertEquals(b.id(),page.items().get(0).enrollmentId());
        assertEquals(a.id(),enrollments.roster(teacher,course,1,1,"PENDING").items().get(0).enrollmentId());
        assertEquals(1,enrollments.list(student,0,20,null).items().size());
        assertEquals(EnrollmentFailure.Reason.COURSE_NOT_FOUND,assertThrows(EnrollmentFailure.class,()->enrollments.roster(otherTeacher,course,0,20,null)).reason());
    }
    @Test void activeEnrollmentConsumesLastSeat() throws Exception {
        sql("INSERT INTO enrollments(student_id,course_id,status,enrolled_at,created_at,updated_at) VALUES ("+student+","+course+",'ACTIVE',now(),now(),now())");
        assertEquals(EnrollmentFailure.Reason.COURSE_FULL,
                assertThrows(EnrollmentFailure.class,()->enrollments.create(other,course)).reason());
        assertEquals(1,occupiedSeats());
        assertEquals(1,count("SELECT count(*) FROM enrollments WHERE course_id="+course));
    }
    long occupiedSeats() throws Exception {
        return count("SELECT count(*) FROM enrollments WHERE course_id="+course+
                " AND (status='ACTIVE' OR (status='PENDING' AND seat_reserved_until>now()))");
    }
    @Test void expiredRenewalAndNewReservationCompeteForOneSeat() throws Exception {
        var original=enrollments.create(student,course).enrollment();
        sql("UPDATE enrollments SET seat_reserved_until=now()-interval '1 second' WHERE id="+original.id().value());
        var ready=new CyclicBarrier(2);
        var connections=ConcurrentHashMap.<Integer>newKeySet();
        var pool=Executors.newFixedThreadPool(2);
        try {
            var renewal=pool.submit(()->competeForSeat(ready,connections,()->enrollments.renew(student,original.id().value())));
            var creation=pool.submit(()->competeForSeat(ready,connections,()->enrollments.create(other,course)));
            assertEquals(1,(renewal.get(15,TimeUnit.SECONDS)?1:0)+(creation.get(15,TimeUnit.SECONDS)?1:0));
            assertEquals(2,connections.size());
            assertEquals(1,occupiedSeats());
            assertEquals(original.id(),enrollments.get(student,original.id().value()).id());
            assertEquals(0,count("SELECT count(*) FROM (SELECT student_id,course_id FROM enrollments GROUP BY student_id,course_id HAVING count(*)>1) duplicates"));
        } finally {pool.shutdownNow();}
    }
    boolean competeForSeat(CyclicBarrier ready,Set<Integer> connections,Runnable operation) {
        var tx=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        tx.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
        try {
            tx.executeWithoutResult(state->{
                var em=org.springframework.orm.jpa.EntityManagerFactoryUtils.getTransactionalEntityManager(
                        context.getBean(jakarta.persistence.EntityManagerFactory.class));
                connections.add(((Number)em.createNativeQuery("SELECT pg_backend_pid()").getSingleResult()).intValue());
                try {ready.await(5,TimeUnit.SECONDS);} catch(Exception failure) {throw new IllegalStateException("Fixture rendezvous failed",failure);}
                operation.run();
                assertEquals(1,((Number)em.createNativeQuery("SELECT count(*) FROM enrollments WHERE course_id=:course AND (status='ACTIVE' OR (status='PENDING' AND seat_reserved_until>now()))")
                        .setParameter("course",course).getSingleResult()).longValue());
            });
            return true;
        } catch(EnrollmentFailure failure) {
            assertEquals(EnrollmentFailure.Reason.COURSE_FULL,failure.reason());
            return false;
        }
    }
    @Test void persistedTeacherApplicationStatesControlRosterAccess() throws Exception {
        enrollments.create(student,course);
        assertEquals(1,enrollments.roster(teacher,course,0,20,null).items().size());
        assertEquals(EnrollmentFailure.Reason.COURSE_NOT_FOUND,
                assertThrows(EnrollmentFailure.class,()->enrollments.roster(otherTeacher,course,0,20,null)).reason());
        try {
            for(String status:List.of("PENDING","REJECTED")) {
                sql("UPDATE teacher_applications SET status='"+status+"',reviewed_at="+
                        (status.equals("PENDING")?"NULL":"now()")+",reviewed_by="+
                        (status.equals("PENDING")?"NULL":Long.toString(admin))+",rejection_reason="+
                        (status.equals("REJECTED")?"'Fixture rejection'":"NULL")+" WHERE teacher_id="+teacher);
                assertEquals(EnrollmentFailure.Reason.ACCESS_DENIED,
                        assertThrows(EnrollmentFailure.class,()->enrollments.roster(teacher,course,0,20,null)).reason());
            }
        } finally {
            sql("UPDATE teacher_applications SET status='APPROVED',rejection_reason=NULL,reviewed_at=now(),reviewed_by="+admin+" WHERE teacher_id="+teacher);
        }
    }
}
