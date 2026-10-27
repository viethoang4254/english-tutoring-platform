package com.englishlearning.features.course;

import com.englishlearning.EnglishLearningApplication;
import com.englishlearning.features.course.service.*;
import com.englishlearning.features.category.service.CategoryService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="coursePersistence", matches="true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CoursePersistenceTests {
    String url, user, password;
    ConfigurableApplicationContext context;
    CourseService courses; CategoryService categories; long teacher, other, admin, category;
    Connection connect() throws Exception { return DriverManager.getConnection(url,user,password); }
    void sql(String sql) throws Exception { try(var c=connect();var s=c.createStatement()){s.execute(sql);} }
    long count(String sql) throws Exception { try(var c=connect();var s=c.createStatement();var r=s.executeQuery(sql)){r.next();return r.getLong(1);} }
    @BeforeAll void start() throws Exception {
        url=System.getenv("COURSE_TEST_URL"); user=System.getenv("COURSE_TEST_USER"); password=System.getenv("COURSE_TEST_PASSWORD");
        assertEquals("jdbc:postgresql://127.0.0.1:55437/task010_courses",url); assertEquals("task010_test",user);
        assertEquals(0,count("SELECT count(*) FROM pg_tables WHERE schemaname NOT IN ('pg_catalog','information_schema')"));
        var flyway=Flyway.configure().dataSource(url,user,password).cleanDisabled(true).load();
        flyway.migrate(); flyway.validate(); assertEquals(6,count("SELECT count(*) FROM flyway_schema_history WHERE success"));
        var props=new HashMap<String,Object>();
        props.put("spring.datasource.url",url);props.put("spring.datasource.username",user);props.put("spring.datasource.password",password);
        props.put("spring.flyway.enabled",false);props.put("spring.jpa.hibernate.ddl-auto","validate");props.put("spring.sql.init.mode","never");
        props.put("spring.datasource.hikari.maximum-pool-size",4);props.put("spring.datasource.hikari.connection-init-sql","SET lock_timeout TO '5s'");
        byte[] key=new byte[32];new java.security.SecureRandom().nextBytes(key);
        props.put("app.security.password-reset.hmac-secret",Base64.getEncoder().encodeToString(key));
        context=new SpringApplicationBuilder(EnglishLearningApplication.class).web(WebApplicationType.NONE)
                .initializers(c->c.getEnvironment().getPropertySources().addFirst(new MapPropertySource("isolated-course-test",props))).run();
        courses=context.getBean(CourseService.class); categories=context.getBean(CategoryService.class);
        sql("INSERT INTO users(email,password_hash,full_name,role,email_verified,locked,created_at,updated_at) VALUES ('admin@example.invalid','unused','Fixture','ADMIN',true,false,now(),now()),('teacher@example.invalid','unused','Fixture','TEACHER',true,false,now(),now()),('other@example.invalid','unused','Fixture','TEACHER',true,false,now(),now())");
        admin=count("SELECT id FROM users WHERE role='ADMIN'");teacher=count("SELECT id FROM users WHERE email='teacher@example.invalid'");other=count("SELECT id FROM users WHERE email='other@example.invalid'");
        sql("INSERT INTO teacher_applications(teacher_id,specialization,teaching_experience,introduction,status,submitted_at,reviewed_at,reviewed_by,created_at,updated_at) SELECT id,'Fixture','Fixture','Fixture','APPROVED',now(),now(),"+admin+",now(),now() FROM users WHERE role='TEACHER'");
    }
    @AfterAll void close() { if(context!=null)context.close(); }
    @BeforeEach void fixture() throws Exception {
        sql("DELETE FROM session_calendar_events");sql("DELETE FROM session_schedule_changes");
        sql("DELETE FROM sessions");
        sql("DELETE FROM courses");sql("DELETE FROM course_categories");
        category=categories.create(admin,Map.of("name","Category")).id().value();
    }
    Map<String,Object> fields() { var f=CourseInputTests.fields(); f.put("categoryId",Long.toString(category));return f; }
    @Test void roundTripPaginationOwnershipAndNoSideEffects() throws Exception {
        var f=fields();f.put("discount",Map.of("percent","12.34","startAt","2026-01-01T00:00:00.123456Z","endAt","2027-01-01T00:00:00Z"));
        var row=courses.create(teacher,f);courses.create(teacher,fields());courses.create(other,fields());
        assertEquals("DRAFT",row.status());assertEquals("12.34",row.discount().percent());
        assertTrue(courses.list(teacher,0,1,null).hasNext());assertFalse(courses.list(teacher,1,1,"DRAFT").hasNext());
        assertEquals(1,courses.list(other,0,20,null).items().size());
        assertEquals(CourseFailure.Reason.MISSING,assertThrows(CourseFailure.class,()->courses.get(other,row.id().value())).reason());
        assertEquals(CourseFailure.Reason.MISSING,assertThrows(CourseFailure.class,()->courses.update(other,row.id().value(),Map.of("name","Wrong"))).reason());
        assertEquals(0,count("SELECT count(*) FROM sessions"));assertEquals(0,count("SELECT count(*) FROM course_schedule_rules"));
        assertEquals(0,count("SELECT count(*) FROM courses WHERE meet_url IS NOT NULL OR thumbnail_path IS NOT NULL"));
    }
    @Test void inactiveRetentionReselectionAndDiscountClear() {
        var row=courses.create(teacher,fields());categories.update(admin,category,Map.of("active",false));
        var changed=courses.update(teacher,row.id().value(),Map.of("name","Renamed","categoryId",Long.toString(category)));
        assertFalse(changed.category().active());assertEquals("Renamed",changed.name());
        assertEquals(CourseFailure.Reason.CATEGORY_INACTIVE,assertThrows(CourseFailure.class,()->courses.create(teacher,fields())).reason());
        var inactive=categories.create(admin,Map.of("name","Inactive","active",false));
        assertThrows(CourseFailure.class,()->courses.update(teacher,row.id().value(),Map.of("categoryId",inactive.id().decimal())));
        assertNull(courses.update(teacher,row.id().value(),Collections.singletonMap("discount",null)).discount());
    }
    @Test void nonDraftStatesReadableButNotEditable() throws Exception {
        var row=courses.create(teacher,fields());
        for(String state:List.of("PUBLISHED","COMPLETED","CANCELLED","ARCHIVED")) {
            sql("UPDATE courses SET status='"+state+"' WHERE id="+row.id().value());
            assertEquals(state,courses.get(teacher,row.id().value()).status());
            assertEquals(1,courses.list(teacher,0,20,state).items().size());
            assertEquals(CourseFailure.Reason.NOT_DRAFT,assertThrows(CourseFailure.class,()->courses.update(teacher,row.id().value(),Map.of("name","No"))).reason());
        }
    }
    @Test void invalidMergedStateAndRollbackPreserveData() {
        var row=courses.create(teacher,fields());
        assertThrows(CourseFailure.class,()->courses.update(teacher,row.id().value(),Map.of("name","Wrong","minStudents",151)));
        var tx=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        assertThrows(IllegalStateException.class,()->tx.execute(s->{courses.update(teacher,row.id().value(),Map.of("name","Rollback"));throw new IllegalStateException("Fixture");}));
        assertEquals(row,courses.get(teacher,row.id().value()));
    }
    @Test void simultaneousPatchesMergeAndSameFieldLastCommitWins() throws Exception {
        var row=courses.create(teacher,fields()); var pool=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(1);
        try {
            var a=pool.submit(()->{ready.await();return courses.update(teacher,row.id().value(),Map.of("name","Name"));});
            var b=pool.submit(()->{ready.await();return courses.update(teacher,row.id().value(),Map.of("description","Text"));});
            ready.countDown();a.get(15,TimeUnit.SECONDS);b.get(15,TimeUnit.SECONDS);
            var merged=courses.get(teacher,row.id().value());assertEquals("Name",merged.name());assertEquals("Text",merged.description());
            var locked=new CountDownLatch(1);var release=new CountDownLatch(1);
            var tx=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
            var first=pool.submit(()->tx.execute(s->{courses.update(teacher,row.id().value(),Map.of("name","First"));locked.countDown();await(release);return null;}));
            assertTrue(locked.await(5,TimeUnit.SECONDS));
            var second=pool.submit(()->courses.update(teacher,row.id().value(),Map.of("name","Last")));
            release.countDown();first.get(15,TimeUnit.SECONDS);second.get(15,TimeUnit.SECONDS);
            assertEquals("Last",courses.get(teacher,row.id().value()).name());
        } finally { pool.shutdownNow(); }
    }
    @Test void deactivationFirstRejectsCreateAndCategoryChange() throws Exception {
        var existing=courses.create(teacher,fields());
        var target=categories.create(admin,Map.of("name","Target")).id().value();
        for(boolean update:List.of(false,true)) {
            categories.update(admin,target,Map.of("active",true));
            var pool=Executors.newSingleThreadExecutor();
            try(var c=connect()) {
                c.setAutoCommit(false);try(var s=c.createStatement()){s.executeUpdate("UPDATE course_categories SET active=false WHERE id="+target);}
                var future=pool.submit(()->{if(update)return courses.update(teacher,existing.id().value(),Map.of("categoryId",Long.toString(target)));var f=fields();f.put("categoryId",Long.toString(target));return courses.create(teacher,f);});
                assertThrows(TimeoutException.class,()->future.get(200,TimeUnit.MILLISECONDS));c.commit();
                assertEquals(CourseFailure.Reason.CATEGORY_INACTIVE,((CourseFailure)assertThrows(ExecutionException.class,()->future.get(10,TimeUnit.SECONDS)).getCause()).reason());
            } finally {pool.shutdownNow();}
        }
        assertEquals(category,courses.get(teacher,existing.id().value()).category().id().value());
    }
    @Test void courseWriteFirstBlocksDeactivationAndRetainsReference() throws Exception {
        var pool=Executors.newFixedThreadPool(2);var locked=new CountDownLatch(1);var release=new CountDownLatch(1);
        var tx=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        try {
            var create=pool.submit(()->tx.execute(s->{var row=courses.create(teacher,fields());locked.countDown();await(release);return row;}));
            assertTrue(locked.await(5,TimeUnit.SECONDS));
            var deactivate=pool.submit(()->categories.update(admin,category,Map.of("active",false)));
            assertThrows(TimeoutException.class,()->deactivate.get(200,TimeUnit.MILLISECONDS));release.countDown();
            var row=create.get(10,TimeUnit.SECONDS);deactivate.get(10,TimeUnit.SECONDS);
            assertFalse(courses.get(teacher,row.id().value()).category().active());
        } finally {release.countDown();pool.shutdownNow();}
    }
    @Test void currentAccountAndOnboardingEligibilityIsEnforced() throws Exception {
        for(String change:List.of("locked=true", "email_verified=false", "role='STUDENT'")) {
            sql("UPDATE users SET "+change+" WHERE id="+teacher);
            try { assertThrows(CourseFailure.class,()->courses.create(teacher,fields())); }
            finally { sql("UPDATE users SET locked=false,email_verified=true,role='TEACHER' WHERE id="+teacher); }
        }
        for(String state:List.of("PENDING","REJECTED")) {
            // No approval remains for this Teacher; the current policy must deny both states.
            sql("UPDATE teacher_applications SET status='"+state+"',reviewed_at="+(state.equals("PENDING")?"null":"now()")+",reviewed_by="+(state.equals("PENDING")?"null":Long.toString(admin))+",rejection_reason="+(state.equals("REJECTED")?"'Fixture'":"null")+" WHERE teacher_id="+teacher);
            try { assertThrows(CourseFailure.class,()->courses.list(teacher,0,20,null)); }
            finally { sql("UPDATE teacher_applications SET status='APPROVED',reviewed_at=now(),reviewed_by="+admin+",rejection_reason=null WHERE teacher_id="+teacher); }
        }
        assertEquals(0,count("SELECT count(*) FROM courses"));
    }
    long scheduledCourse(int count) {
        var f=fields();f.put("plannedStartDate","2030-01-01");f.put("sessionCount",count);
        return courses.create(teacher,f).id().value();
    }
    Map<String,Object> weekly(int day,String start,String end) {return Map.of("rules",List.of(SchedulePlanTests.rule(day,start,end)));}
    @Test void scheduleGenerationRoundTripAndPlanningProtection() throws Exception {
        long id=scheduledCourse(12);
        var schedule=courses.replaceSchedule(teacher,id,weekly(2,"19:00","20:30"));
        assertEquals(schedule,courses.replaceSchedule(teacher,id,weekly(2,"19:00","20:30")));
        assertEquals(12,courses.generate(teacher,id).generatedSessionCount());
        assertEquals(12,courses.schedule(teacher,id).generatedSessionCount());
        assertEquals(12,count("SELECT count(*) FROM sessions WHERE course_id="+id+" AND status='SCHEDULED' AND title IS NULL AND content IS NULL AND cancellation_reason IS NULL"));
        assertEquals(12,count("SELECT max(session_number) FROM sessions WHERE course_id="+id));
        assertEquals(0,count("SELECT count(*) FROM (SELECT scheduled_start_at,lag(scheduled_end_at) OVER (ORDER BY session_number) previous_end FROM sessions WHERE course_id="+id+") s WHERE scheduled_start_at < previous_end"));
        assertEquals(CourseFailure.Reason.ALREADY_GENERATED,assertThrows(CourseFailure.class,()->courses.generate(teacher,id)).reason());
        assertEquals(CourseFailure.Reason.SCHEDULE_LOCKED,assertThrows(CourseFailure.class,()->courses.replaceSchedule(teacher,id,weekly(3,"19:00","20:30"))).reason());
        for(var patch:List.<Map<String,Object>>of(Map.of("plannedStartDate","2031-01-01"),Map.of("timezone","UTC"),Map.of("sessionCount",13)))
            assertEquals(CourseFailure.Reason.SCHEDULE_LOCKED,assertThrows(CourseFailure.class,()->courses.update(teacher,id,patch)).reason());
        assertEquals("Renamed",courses.update(teacher,id,Map.of("name","Renamed","sessionCount",12,"plannedStartDate","2030-01-01","timezone","Asia/Ho_Chi_Minh")).name());
        assertEquals(1,count("SELECT count(*) FROM course_schedule_rules WHERE course_id="+id));
    }
    @Test void lateDstFailureAndDatabaseInsertFailureRollbackEverything() throws Exception {
        long id=scheduledCourse(2);
        courses.update(teacher,id,Map.of("plannedStartDate","2030-03-03","timezone","America/New_York"));
        courses.replaceSchedule(teacher,id,weekly(7,"02:15","03:15"));
        assertEquals(CourseFailure.Reason.SCHEDULE_TIME,assertThrows(CourseFailure.class,()->courses.generate(teacher,id)).reason());
        assertEquals(0,count("SELECT count(*) FROM sessions"));
        courses.update(teacher,id,Map.of("timezone","UTC"));
        // Test-only trigger proves SQL failures after an earlier insert roll the entire batch back.
        sql("CREATE FUNCTION reject_second_session() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.session_number=2 THEN RAISE EXCEPTION 'Fixture'; END IF; RETURN NEW; END $$");
        sql("CREATE TRIGGER reject_second_session BEFORE INSERT ON sessions FOR EACH ROW EXECUTE FUNCTION reject_second_session()");
        try {assertThrows(RuntimeException.class,()->courses.generate(teacher,id));assertEquals(0,count("SELECT count(*) FROM sessions"));}
        finally {sql("DROP TRIGGER reject_second_session ON sessions");sql("DROP FUNCTION reject_second_session()");}
        assertEquals(2,courses.generate(teacher,id).generatedSessionCount());
    }
    @Test void concurrentGenerationCreatesExactlyOneBatch() throws Exception {
        long id=scheduledCourse(12);courses.replaceSchedule(teacher,id,weekly(2,"19:00","20:00"));
        var pool=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(1);
        try {
            Callable<Object> generate=()->{ready.await();try{return courses.generate(teacher,id);}catch(CourseFailure e){return e.reason();}};
            var a=pool.submit(generate);var b=pool.submit(generate);ready.countDown();
            var results=List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));
            assertEquals(1,results.stream().filter(r->r==CourseFailure.Reason.ALREADY_GENERATED).count());
            assertEquals(12,count("SELECT count(*) FROM sessions WHERE course_id="+id));
        } finally {pool.shutdownNow();}
    }
    @Test void replacementAndGenerationSerializeInBothOrders() throws Exception {
        for(boolean generateFirst:List.of(false,true)) {
            long id=scheduledCourse(2);courses.replaceSchedule(teacher,id,weekly(2,"19:00","20:00"));
            var pool=Executors.newFixedThreadPool(2);var held=new CountDownLatch(1);var release=new CountDownLatch(1);
            var tx=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
            try {
                var first=pool.submit(()->tx.execute(s->{if(generateFirst)courses.generate(teacher,id);else courses.replaceSchedule(teacher,id,weekly(5,"18:00","19:00"));held.countDown();await(release);return null;}));
                assertTrue(held.await(5,TimeUnit.SECONDS));
                var second=pool.submit(()->{if(generateFirst)return courses.replaceSchedule(teacher,id,weekly(5,"18:00","19:00"));return courses.generate(teacher,id);});
                assertThrows(TimeoutException.class,()->second.get(200,TimeUnit.MILLISECONDS));release.countDown();first.get(10,TimeUnit.SECONDS);
                if(generateFirst) assertEquals(CourseFailure.Reason.SCHEDULE_LOCKED,((CourseFailure)assertThrows(ExecutionException.class,()->second.get(10,TimeUnit.SECONDS)).getCause()).reason());
                else {second.get(10,TimeUnit.SECONDS);assertEquals(2,count("SELECT count(*) FROM sessions WHERE course_id="+id+" AND extract(isodow FROM scheduled_start_at AT TIME ZONE 'Asia/Ho_Chi_Minh')=5"));}
            } finally {release.countDown();pool.shutdownNow();}
        }
    }
    @Test void scheduleOwnershipEligibilityAndLifecycleAreEnforced() throws Exception {
        long id=scheduledCourse(2);courses.replaceSchedule(teacher,id,weekly(2,"19:00","20:00"));
        assertEquals(CourseFailure.Reason.MISSING,assertThrows(CourseFailure.class,()->courses.schedule(other,id)).reason());
        assertEquals(CourseFailure.Reason.MISSING,assertThrows(CourseFailure.class,()->courses.generate(other,id)).reason());
        assertEquals(CourseFailure.Reason.MISSING,assertThrows(CourseFailure.class,()->courses.replaceSchedule(other,id,weekly(2,"19:00","20:00"))).reason());
        sql("UPDATE users SET locked=true WHERE id="+teacher);
        try {assertThrows(CourseFailure.class,()->courses.generate(teacher,id));assertThrows(CourseFailure.class,()->courses.schedule(teacher,id));}
        finally {sql("UPDATE users SET locked=false WHERE id="+teacher);}
        sql("UPDATE courses SET status='PUBLISHED' WHERE id="+id);
        assertEquals(0,courses.schedule(teacher,id).generatedSessionCount());
        assertEquals(CourseFailure.Reason.NOT_DRAFT,assertThrows(CourseFailure.class,()->courses.generate(teacher,id)).reason());
        assertEquals(CourseFailure.Reason.NOT_DRAFT,assertThrows(CourseFailure.class,()->courses.replaceSchedule(teacher,id,weekly(2,"19:00","20:00"))).reason());
        assertEquals(0,count("SELECT count(*) FROM sessions"));
    }
    @Test void generationLimitDoesNotAlterPersistedCourseConstraints() throws Exception {
        long id=scheduledCourse(1001);courses.replaceSchedule(teacher,id,weekly(2,"19:00","20:00"));
        assertEquals(CourseFailure.Reason.GENERATION_LIMIT,assertThrows(CourseFailure.class,()->courses.generate(teacher,id)).reason());
        assertEquals(1001,courses.get(teacher,id).sessionCount());assertEquals(0,count("SELECT count(*) FROM sessions"));
    }
    long contentCourse() {long id=scheduledCourse(3);courses.replaceSchedule(teacher,id,weekly(2,"19:00","20:00"));courses.generate(teacher,id);return id;}
    @Test void sessionListDetailFilteringAndNestedOwnership() throws Exception {
        long id=contentCourse();var page=courses.sessions(teacher,id,0,2,null);
        assertTrue(page.hasNext());assertEquals(List.of(1,2),page.items().stream().map(s->s.sessionNumber()).toList());
        assertEquals(3,courses.sessions(teacher,id,1,2,null).items().get(0).sessionNumber());
        long session=page.items().get(0).id().value();
        sql("UPDATE sessions SET status='CANCELLED',cancellation_reason='Fixture' WHERE id="+session);
        assertEquals(1,courses.sessions(teacher,id,0,20,"CANCELLED").items().size());
        assertEquals(2,courses.sessions(teacher,id,0,100,"SCHEDULED").items().size());
        assertEquals("Fixture",courses.session(teacher,id,session).cancellationReason());
        long different=contentCourse();
        assertEquals(CourseFailure.Reason.SESSION_MISSING,assertThrows(CourseFailure.class,()->courses.session(teacher,different,session)).reason());
        assertEquals(CourseFailure.Reason.SESSION_MISSING,assertThrows(CourseFailure.class,()->courses.updateSession(teacher,different,session,Map.of("title","No"))).reason());
        assertEquals(CourseFailure.Reason.MISSING,assertThrows(CourseFailure.class,()->courses.sessions(other,id,0,20,null)).reason());
        assertEquals(CourseFailure.Reason.MISSING,assertThrows(CourseFailure.class,()->courses.updateSession(other,id,session,Map.of("title","No"))).reason());
    }
    @Test void sessionContentPersistenceNullAndStatePreservation() throws Exception {
        long id=contentCourse(),session=courses.sessions(teacher,id,0,20,null).items().get(0).id().value();
        var original=courses.session(teacher,id,session);
        var edited=courses.updateSession(teacher,id,session,Map.of("title","  Title  ","content","<b>Plain</b>\ntext"));
        assertEquals("Title",edited.title());assertEquals("<b>Plain</b>\ntext",edited.content());
        assertEquals(original.scheduledStartAt(),edited.scheduledStartAt());assertEquals(original.scheduledEndAt(),edited.scheduledEndAt());
        assertEquals(original.sessionNumber(),edited.sessionNumber());assertEquals(original.status(),edited.status());assertEquals(original.createdAt(),edited.createdAt());
        assertNull(courses.updateSession(teacher,id,session,Collections.singletonMap("title",null)).title());
        assertEquals(edited.content(),courses.session(teacher,id,session).content());
        sql("UPDATE courses SET status='PUBLISHED' WHERE id="+id);
        assertNull(courses.updateSession(teacher,id,session,Collections.singletonMap("content",null)).content());
        for(String state:List.of("COMPLETED","CANCELLED","ARCHIVED")) {
            sql("UPDATE courses SET status='"+state+"' WHERE id="+id);
            assertEquals(CourseFailure.Reason.SESSION_NOT_EDITABLE,assertThrows(CourseFailure.class,()->courses.updateSession(teacher,id,session,Map.of("title","No"))).reason());
        }
        sql("UPDATE courses SET status='DRAFT' WHERE id="+id);sql("UPDATE sessions SET status='CANCELLED' WHERE id="+session);
        assertEquals(CourseFailure.Reason.SESSION_NOT_EDITABLE,assertThrows(CourseFailure.class,()->courses.updateSession(teacher,id,session,Map.of("title","No"))).reason());
        assertNull(courses.session(teacher,id,session).title());assertEquals(3,count("SELECT count(*) FROM sessions WHERE course_id="+id));
    }
    @Test void sessionContentRollbackPreservesOriginal() {
        long id=contentCourse(),session=courses.sessions(teacher,id,0,20,null).items().get(0).id().value();
        var original=courses.session(teacher,id,session);var tx=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        assertThrows(IllegalStateException.class,()->tx.execute(s->{courses.updateSession(teacher,id,session,Map.of("title","Rollback"));throw new IllegalStateException("Fixture");}));
        assertEquals(original,courses.session(teacher,id,session));
        assertThrows(CourseFailure.class,()->courses.updateSession(teacher,id,session,Map.of("title","Good","content","\u0000")));
        assertEquals(original,courses.session(teacher,id,session));
    }
    @Test void concurrentSessionPatchesMergeAndLastCommittedFieldWins() throws Exception {
        long id=contentCourse(),session=courses.sessions(teacher,id,0,20,null).items().get(0).id().value();
        var pool=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(1);
        try {
            var a=pool.submit(()->{ready.await();return courses.updateSession(teacher,id,session,Map.of("title","Title"));});
            var b=pool.submit(()->{ready.await();return courses.updateSession(teacher,id,session,Map.of("content","Content"));});
            ready.countDown();a.get(15,TimeUnit.SECONDS);b.get(15,TimeUnit.SECONDS);
            assertEquals("Title",courses.session(teacher,id,session).title());assertEquals("Content",courses.session(teacher,id,session).content());
            var held=new CountDownLatch(1);var release=new CountDownLatch(1);var tx=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
            try {
                var first=pool.submit(()->tx.execute(s->{courses.updateSession(teacher,id,session,Map.of("title","First"));held.countDown();await(release);return null;}));
                assertTrue(held.await(5,TimeUnit.SECONDS));var second=pool.submit(()->courses.updateSession(teacher,id,session,Map.of("title","Last")));
                assertThrows(TimeoutException.class,()->second.get(200,TimeUnit.MILLISECONDS));release.countDown();first.get(10,TimeUnit.SECONDS);second.get(10,TimeUnit.SECONDS);
                assertEquals("Last",courses.session(teacher,id,session).title());
            } finally {release.countDown();}
        } finally {pool.shutdownNow();}
    }
    Map<String,Object> move(com.englishlearning.features.course.dto.SessionDto current,String start,String end) {
        return Map.of("expectedStartAt",current.scheduledStartAt().toString(),"expectedEndAt",current.scheduledEndAt().toString(),"scheduledStartAt",start,"scheduledEndAt",end);
    }
    @Test void rescheduleHistoryNoopStaleAndPreservation() throws Exception {
        long id=contentCourse(),sid=courses.sessions(teacher,id,0,20,null).items().get(0).id().value();
        var original=courses.updateSession(teacher,id,sid,Map.of("title","Title","content","Content"));
        var input=move(original,"2030-02-01T12:00:00Z","2030-02-01T13:00:00Z");
        var changed=courses.reschedule(teacher,id,sid,input);
        assertEquals(original.id(),changed.id());assertEquals(original.sessionNumber(),changed.sessionNumber());assertEquals(original.title(),changed.title());assertEquals(original.content(),changed.content());assertEquals(original.status(),changed.status());
        assertEquals(CourseFailure.Reason.SESSION_CHANGED,assertThrows(CourseFailure.class,()->courses.reschedule(teacher,id,sid,input)).reason());
        assertEquals(changed,courses.reschedule(teacher,id,sid,move(changed,changed.scheduledStartAt().toString(),changed.scheduledEndAt().toString())));
        var second=courses.reschedule(teacher,id,sid,move(changed,"2030-02-02T12:00:00Z","2030-02-02T13:00:00Z"));
        var page=courses.history(teacher,id,sid,0,1);assertTrue(page.hasNext());assertEquals(original.scheduledStartAt(),page.items().get(0).oldStartAt());assertEquals(changed.scheduledStartAt(),page.items().get(0).newStartAt());
        var next=courses.history(teacher,id,sid,1,1);assertFalse(next.hasNext());assertTrue(next.items().get(0).id().value()>page.items().get(0).id().value());assertEquals(second.scheduledStartAt(),next.items().get(0).newStartAt());
        assertEquals(CourseFailure.Reason.MISSING,assertThrows(CourseFailure.class,()->courses.history(other,id,sid,0,20)).reason());
        long another=contentCourse();assertEquals(CourseFailure.Reason.SESSION_MISSING,assertThrows(CourseFailure.class,()->courses.reschedule(teacher,another,sid,input)).reason());
    }
    @Test void cancellationIsIdempotentAndReleasesIntervalWithoutDeletingHistory() throws Exception {
        long id=contentCourse();var list=courses.sessions(teacher,id,0,20,null).items();long sid=list.get(0).id().value(),otherSid=list.get(1).id().value();
        var original=courses.session(teacher,id,sid);var moved=courses.reschedule(teacher,id,sid,move(original,"2030-02-01T12:00:00Z","2030-02-01T13:00:00Z"));
        var cancelled=courses.cancel(teacher,id,sid,Map.of("reason","  Unavailable  "));
        assertEquals("CANCELLED",cancelled.status());assertEquals("Unavailable",cancelled.cancellationReason());assertEquals(moved.scheduledStartAt(),cancelled.scheduledStartAt());assertEquals(moved.sessionNumber(),cancelled.sessionNumber());
        assertEquals(cancelled,courses.cancel(teacher,id,sid,Map.of("reason","Different")));assertEquals(cancelled,courses.cancel(teacher,id,sid,Map.of()));
        assertEquals(1,courses.history(teacher,id,sid,0,20).items().size());assertEquals(3,courses.get(teacher,id).sessionCount());assertEquals("DRAFT",courses.get(teacher,id).status());
        var otherSession=courses.session(teacher,id,otherSid);courses.reschedule(teacher,id,otherSid,move(otherSession,moved.scheduledStartAt().toString(),moved.scheduledEndAt().toString()));
        assertEquals(CourseFailure.Reason.SESSION_NOT_EDITABLE,assertThrows(CourseFailure.class,()->courses.reschedule(teacher,id,sid,move(moved,"2030-02-02T12:00:00Z","2030-02-02T13:00:00Z"))).reason());
        assertNull(courses.cancel(teacher,id,otherSid,Collections.singletonMap("reason",null)).cancellationReason());
        assertNull(courses.cancel(teacher,id,list.get(2).id().value(),Map.of()).cancellationReason());
    }
    @Test void overlapAdjacencyCalendarGuardAndHistoryFailureRollback() throws Exception {
        long id=contentCourse();var list=courses.sessions(teacher,id,0,20,null).items();long sid=list.get(0).id().value(),otherSid=list.get(1).id().value();
        var current=courses.session(teacher,id,sid);var otherSession=courses.session(teacher,id,otherSid);
        assertEquals(CourseFailure.Reason.SCHEDULE_OVERLAP,assertThrows(CourseFailure.class,()->courses.reschedule(teacher,id,sid,move(current,otherSession.scheduledStartAt().toString(),otherSession.scheduledEndAt().toString()))).reason());
        var adjacent=courses.reschedule(teacher,id,sid,move(current,otherSession.scheduledEndAt().toString(),otherSession.scheduledEndAt().plusSeconds(3600).toString()));
        sql("INSERT INTO session_calendar_events(session_id,provider,sync_status,created_at,updated_at) VALUES ("+sid+",'GOOGLE_CALENDAR','PENDING',now(),now())");
        assertEquals(CourseFailure.Reason.CALENDAR_SYNC_REQUIRED,assertThrows(CourseFailure.class,()->courses.cancel(teacher,id,sid,Map.of())).reason());
        assertEquals(CourseFailure.Reason.CALENDAR_SYNC_REQUIRED,assertThrows(CourseFailure.class,()->courses.reschedule(teacher,id,sid,move(adjacent,"2030-02-01T12:00:00Z","2030-02-01T13:00:00Z"))).reason());
        assertEquals(1,count("SELECT count(*) FROM session_calendar_events"));sql("DELETE FROM session_calendar_events");
        sql("CREATE FUNCTION reject_history() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'Fixture'; END $$");
        sql("CREATE TRIGGER reject_history BEFORE INSERT ON session_schedule_changes FOR EACH ROW EXECUTE FUNCTION reject_history()");
        try {assertThrows(RuntimeException.class,()->courses.reschedule(teacher,id,sid,move(adjacent,"2030-02-01T12:00:00Z","2030-02-01T13:00:00Z")));assertEquals(adjacent,courses.session(teacher,id,sid));assertEquals(1,courses.history(teacher,id,sid,0,20).items().size());}
        finally {sql("DROP TRIGGER reject_history ON session_schedule_changes");sql("DROP FUNCTION reject_history()");}
    }
    @Test void phaseThreeLifecycleFutureAndEligibility() throws Exception {
        long id=contentCourse(),sid=courses.sessions(teacher,id,0,20,null).items().get(0).id().value();
        var current=courses.session(teacher,id,sid);var input=move(current,"2030-02-01T12:00:00Z","2030-02-01T13:00:00Z");
        for(String state:List.of("COMPLETED","ARCHIVED","CANCELLED")) {
            sql("UPDATE courses SET status='"+state+"' WHERE id="+id);
            assertEquals(CourseFailure.Reason.SESSION_NOT_EDITABLE,assertThrows(CourseFailure.class,()->courses.reschedule(teacher,id,sid,input)).reason());
            assertEquals(CourseFailure.Reason.SESSION_NOT_EDITABLE,assertThrows(CourseFailure.class,()->courses.cancel(teacher,id,sid,Map.of())).reason());
        }
        sql("UPDATE courses SET status='PUBLISHED' WHERE id="+id);var moved=courses.reschedule(teacher,id,sid,input);assertEquals("PUBLISHED",courses.get(teacher,id).status());
        sql("UPDATE users SET locked=true WHERE id="+teacher);
        try {assertThrows(CourseFailure.class,()->courses.cancel(teacher,id,sid,Map.of()));}finally{sql("UPDATE users SET locked=false WHERE id="+teacher);}
        sql("UPDATE sessions SET scheduled_start_at=now()-interval '1 hour',scheduled_end_at=now()+interval '1 hour' WHERE id="+sid);
        assertEquals(CourseFailure.Reason.SCHEDULE_TIME,assertThrows(CourseFailure.class,()->courses.cancel(teacher,id,sid,Map.of())).reason());
        assertEquals(CourseFailure.Reason.SCHEDULE_TIME,assertThrows(CourseFailure.class,()->courses.reschedule(teacher,id,sid,move(moved,"2030-02-02T12:00:00Z","2030-02-02T13:00:00Z"))).reason());
    }
    @Test void concurrentRescheduleSameSessionDetectsStaleExpectedTimes() throws Exception {
        long id=contentCourse(),sid=courses.sessions(teacher,id,0,20,null).items().get(0).id().value();var current=courses.session(teacher,id,sid);
        var pool=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(1);
        try {
            Callable<Object> change=()->{ready.await();try{return courses.reschedule(teacher,id,sid,move(current,"2030-02-01T12:00:00Z","2030-02-01T13:00:00Z"));}catch(CourseFailure e){return e.reason();}};
            var a=pool.submit(change);var b=pool.submit(change);ready.countDown();var results=List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));
            assertEquals(1,results.stream().filter(r->r==CourseFailure.Reason.SESSION_CHANGED).count());assertEquals(1,courses.history(teacher,id,sid,0,20).items().size());
        }finally{pool.shutdownNow();}
    }
    @Test void concurrentDifferentSessionsCannotClaimSameInterval() throws Exception {
        long id=contentCourse();var rows=courses.sessions(teacher,id,0,20,null).items();var first=courses.session(teacher,id,rows.get(0).id().value());var second=courses.session(teacher,id,rows.get(1).id().value());
        var pool=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(1);
        try {
            var futures=new ArrayList<Future<Object>>();
            for(var current:List.of(first,second))futures.add(pool.submit(()->{ready.await();try{return courses.reschedule(teacher,id,current.id().value(),move(current,"2030-02-01T12:00:00Z","2030-02-01T13:00:00Z"));}catch(CourseFailure e){return e.reason();}}));
            ready.countDown();var results=List.of(futures.get(0).get(15,TimeUnit.SECONDS),futures.get(1).get(15,TimeUnit.SECONDS));
            assertEquals(1,results.stream().filter(r->r==CourseFailure.Reason.SCHEDULE_OVERLAP).count());assertEquals(1,count("SELECT count(*) FROM session_schedule_changes"));
        }finally{pool.shutdownNow();}
    }
    static void await(CountDownLatch latch) {try{if(!latch.await(10,TimeUnit.SECONDS))throw new IllegalStateException("Fixture timeout");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}}
}
