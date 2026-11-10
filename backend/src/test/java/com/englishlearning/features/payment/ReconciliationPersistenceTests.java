package com.englishlearning.features.payment;

import com.englishlearning.EnglishLearningApplication;
import com.englishlearning.features.payment.dto.*;
import com.englishlearning.features.payment.service.*;
import com.englishlearning.features.enrollment.service.EnrollmentService;
import com.englishlearning.features.enrollment.service.EnrollmentFailure;
import com.englishlearning.features.paymentaccount.service.ReceivingAccountReviewService;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
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

@EnabledIfSystemProperty(named="paymentPersistence",matches="true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReconciliationPersistenceTests {
    static final Instant START=Instant.parse("2026-11-09T00:00:00Z");
    static final TestClock CLOCK=new TestClock();
    static class TestClock extends Clock {
        volatile Instant now=START;
        public ZoneId getZone(){return ZoneOffset.UTC;}
        public Clock withZone(ZoneId zone){return Clock.fixed(now,zone);}
        public Instant instant(){return now;}
    }
    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods=false)
    static class TimeFixture {
        @org.springframework.context.annotation.Bean @org.springframework.context.annotation.Primary
        @org.springframework.beans.factory.annotation.Qualifier("refreshSessionClock")
        Clock reconciliationClock(){return CLOCK;}
    }
    String url,user,password;long course=300;
    ConfigurableApplicationContext context;PaymentService payments;EnrollmentService enrollments;ReconciliationService service;
    void sql(String value)throws Exception{try(var c=DriverManager.getConnection(url,user,password);var s=c.createStatement()){s.execute(value);}}
    String text(String value)throws Exception{try(var c=DriverManager.getConnection(url,user,password);var s=c.createStatement();var r=s.executeQuery(value)){assertTrue(r.next());return r.getString(1);}}
    long count(String query)throws Exception{return Long.parseLong(text(query));}
    @BeforeAll void start()throws Exception {
        String base=System.getenv("PAYMENT_TEST_URL");assertEquals("jdbc:postgresql://127.0.0.1:15442/task022c_payments",base);
        url=base.replace("/task022c_payments","/task022d_reconciliation");user=System.getenv("PAYMENT_TEST_USER");password=System.getenv("PAYMENT_TEST_PASSWORD");assertEquals("task022c_test",user);
        assertEquals(0,count("SELECT count(*) FROM pg_tables WHERE schemaname NOT IN ('pg_catalog','information_schema')"));
        assertEquals(10,Flyway.configure().dataSource(url,user,password).cleanDisabled(true).load().migrate().migrationsExecuted);
        sql("INSERT INTO users(id,email,password_hash,full_name,role,email_verified,locked,created_at,updated_at) VALUES (1,'teacher@example.invalid','unused','Teacher','TEACHER',true,false,now(),now()),(2,'admin@example.invalid','unused','Admin','ADMIN',true,false,now(),now()),(3,'student@example.invalid','unused','Student','STUDENT',false,false,now(),now()),(4,'other@example.invalid','unused','Other','STUDENT',true,false,now(),now()),(5,'teacher2@example.invalid','unused','Other Teacher','TEACHER',true,false,now(),now())");
        sql("INSERT INTO teacher_applications(teacher_id,specialization,teaching_experience,introduction,status,submitted_at,reviewed_at,reviewed_by,created_at,updated_at) VALUES (1,'Test','Test','Test','APPROVED',now(),now(),2,now(),now()),(5,'Test','Test','Test','APPROVED',now(),now(),2,now(),now())");
        sql("INSERT INTO course_categories(id,name,created_at,updated_at) VALUES (100,'Reconciliation',now(),now())");
        var props=new HashMap<String,Object>();props.put("spring.datasource.url",url);props.put("spring.datasource.username",user);props.put("spring.datasource.password",password);
        props.put("spring.flyway.enabled",false);props.put("spring.jpa.hibernate.ddl-auto","validate");props.put("spring.sql.init.mode","never");
        props.put("spring.datasource.hikari.maximum-pool-size",4);props.put("spring.datasource.hikari.connection-init-sql","SET lock_timeout TO '5s'");
        byte[] key=new byte[32];new java.security.SecureRandom().nextBytes(key);props.put("app.security.password-reset.hmac-secret",Base64.getEncoder().encodeToString(key));
        context=new SpringApplicationBuilder(EnglishLearningApplication.class,TimeFixture.class).web(WebApplicationType.NONE)
            .initializers(c->c.getEnvironment().getPropertySources().addFirst(new MapPropertySource("isolated-reconciliation",props))).run();
        payments=context.getBean(PaymentService.class);enrollments=context.getBean(EnrollmentService.class);service=context.getBean(ReconciliationService.class);
        account(200,"00123456");
    }
    void account(long id,String number)throws Exception { account(id,number,1); }
    void account(long id,String number,long teacher)throws Exception {
        String key=UUID.randomUUID().toString();
        sql("INSERT INTO teacher_payment_accounts(id,teacher_id,bank_code,bank_bin,bank_name,account_number,account_holder_name,provider,status,review_status,qr_object,qr_sha256,qr_size,submission_key,request_sha256,created_at,updated_at) VALUES ("+id+","+teacher+",'VCB','970436','Bank','"+number+"','Teacher','VIETQR','INACTIVE','PENDING','"+key+".png',repeat('a',64),100,'"+key+"',repeat('b',64),now(),now())");
        context.getBean(ReceivingAccountReviewService.class).review(2,id,true,null);
    }
    @AfterAll void close(){if(context!=null)context.close();}
    @BeforeEach void fixture()throws Exception {
        CLOCK.now=START;createCourse(1);
    }
    void createCourse(long owner)throws Exception {
        course++;
        sql("UPDATE users SET locked=false,email_verified=true WHERE id IN (1,5)");sql("UPDATE users SET locked=false WHERE id IN (3,4)");
        sql("INSERT INTO courses(id,teacher_id,category_id,name,description,tuition,currency,min_students,max_students,planned_start_date,timezone,session_count,status,created_at,updated_at) VALUES ("+course+","+owner+",100,'Reconcile','Fixture',101,'VND',1,1,current_date,'Asia/Ho_Chi_Minh',1,'PUBLISHED',now(),now())");
        sql("INSERT INTO sessions(course_id,session_number,scheduled_start_at,scheduled_end_at,status,created_at,updated_at) VALUES ("+course+",1,'"+START.plusSeconds(3600)+"','"+START.plusSeconds(7200)+"','SCHEDULED',now(),now())");
    }
    PaymentDto create(long student){return payments.create(student,enrollments.create(student,course).enrollment().id().value()).payment();}
    ReconciliationEvidence evidence(PaymentDto p){return new ReconciliationEvidence(p.bankBin(),p.accountNumber(),UUID.randomUUID().toString(),START,new BigDecimal(p.finalAmount()),"VND",p.transferReference(),p.transferReference(),true);}
    ReconciliationAttempt decide(PaymentDto p,ReconciliationEvidence e,boolean late){return service.decide(1,course,p.id().value(),e,late);}
    long audits(PaymentDto p)throws Exception{return count("SELECT count(*) FROM payment_reconciliation_attempts WHERE payment_id="+p.id().value());}
    String state(PaymentDto p)throws Exception{return text("SELECT p.status||'/'||e.status FROM payments p JOIN enrollments e ON e.id=p.enrollment_id WHERE p.id="+p.id().value());}
    String snapshot(PaymentDto p)throws Exception{return text("SELECT to_jsonb(p)-'status'-'updated_at'-'confirmed_at' FROM payments p WHERE id="+p.id().value());}
    @Test void onTimeActivationSnapshotsHistoryAndNoProviderEvidence()throws Exception {
        var p=create(3);String snapshot=snapshot(p);var a=decide(p,evidence(p),false);
        assertEquals("SUCCESS",a.outcome());assertTrue(a.allocated());assertEquals("CONFIRMED/ACTIVE",state(p));assertEquals(snapshot,snapshot(p));
        assertEquals(1,audits(p));assertEquals(1,service.history(1,course,p.id().value(),0,20).items().size());
        assertEquals(0,count("SELECT count(*) FROM payment_transactions"));
        assertEquals("23514",assertThrows(SQLException.class,()->sql("UPDATE payment_reconciliation_attempts SET outcome='FAILED' WHERE id="+a.id().value())).getSQLState());
        assertEquals("23514",assertThrows(SQLException.class,()->sql("DELETE FROM payment_reconciliation_attempts WHERE id="+a.id().value())).getSQLState());
        assertEquals("23514",assertThrows(SQLException.class,()->sql("UPDATE payments SET final_amount=999 WHERE id="+p.id().value())).getSQLState());
    }
    @Test void mismatchesAndFalseAttestationPersistWithoutActivation()throws Exception {
        var p=create(3);var e=evidence(p);
        var bad=List.of(
            new ReconciliationEvidence(e.bankBin(),e.accountNumber(),e.bankTransactionReference(),START,new BigDecimal("100"),"VND",e.transferDescription(),e.transferReference(),true),
            new ReconciliationEvidence(e.bankBin(),e.accountNumber(),e.bankTransactionReference(),START,e.observedAmount(),"USD",e.transferDescription(),e.transferReference(),true),
            new ReconciliationEvidence(e.bankBin(),"999999",e.bankTransactionReference(),START,e.observedAmount(),"VND",e.transferDescription(),e.transferReference(),true),
            new ReconciliationEvidence(e.bankBin(),e.accountNumber(),e.bankTransactionReference(),START,e.observedAmount(),"VND","wrong",e.transferReference(),true),
            new ReconciliationEvidence(e.bankBin(),e.accountNumber(),e.bankTransactionReference(),START.plusSeconds(1),e.observedAmount(),"VND",e.transferDescription(),e.transferReference(),true),
            new ReconciliationEvidence(e.bankBin(),e.accountNumber(),e.bankTransactionReference(),START,e.observedAmount(),"VND",e.transferDescription(),e.transferReference(),false));
        for(var input:bad)assertEquals("FAILED",decide(p,input,false).outcome());
        assertEquals(6,audits(p));assertEquals("PENDING/PENDING",state(p));assertEquals("SUCCESS",decide(p,e,false).outcome());assertEquals(7,audits(p));
    }
    @Test void duplicateConfirmationIsAuditedReplay()throws Exception {
        var p=create(3);var e=evidence(p);var first=decide(p,e,false);var second=decide(p,e,false);
        assertEquals("SUCCESS",second.outcome());assertFalse(second.allocated());assertEquals(first.id(),second.replayOf());assertEquals(2,audits(p));
        assertEquals(1,count("SELECT count(*) FROM payment_reconciliation_attempts WHERE payment_id="+p.id().value()+" AND allocated"));
    }
    @Test void bankTransactionCannotConfirmTwoPayments()throws Exception {
        sql("UPDATE courses SET max_students=2 WHERE id="+course);var p=create(3);var q=create(4);var e=evidence(p);decide(p,e,false);
        var reused=new ReconciliationEvidence(q.bankBin(),q.accountNumber(),e.bankTransactionReference(),START,new BigDecimal(q.finalAmount()),"VND",q.transferReference(),q.transferReference(),true);
        assertEquals("BANK_TRANSACTION_ALREADY_ALLOCATED",decide(q,reused,false).failureReason());assertEquals("PENDING/PENDING",state(q));
    }
    @Test void lateReconciliationReacquiresSeatWithoutRenewing()throws Exception {
        var p=create(3);CLOCK.now=p.expiresAt();
        assertEquals("LATE_RECONCILIATION_REQUIRED",decide(p,evidence(p),false).failureReason());
        CLOCK.now=p.expiresAt().plusSeconds(1);var e=evidence(p);
        var lateTransfer=new ReconciliationEvidence(e.bankBin(),e.accountNumber(),e.bankTransactionReference(),CLOCK.now,e.observedAmount(),e.currency(),e.transferDescription(),e.transferReference(),true);
        assertEquals("SUCCESS",decide(p,lateTransfer,true).outcome());assertEquals("CONFIRMED/ACTIVE",state(p));
        assertEquals(p.expiresAt().toString(),payments.get(3,p.id().value()).expiresAt().toString());
    }
    @Test void noSeatFailureIsDurableAndSameEvidenceCanRetry()throws Exception {
        var p=create(3);var e=evidence(p);CLOCK.now=p.expiresAt();var other=enrollments.create(4,course).enrollment();
        assertEquals("MANUAL_RESOLUTION_REQUIRED_NO_CAPACITY",decide(p,e,true).failureReason());assertEquals(1,audits(p));
        assertFalse(state(p).contains("ACTIVE"));enrollments.cancel(4,other.id().value());
        assertEquals("SUCCESS",decide(p,e,true).outcome());assertEquals(2,audits(p));
    }
    @Test void newerPendingObligationIsUntouchedThenExpiredConflictCanRetry()throws Exception {
        var old=create(3);var e=evidence(old);CLOCK.now=old.expiresAt();enrollments.renew(3,old.enrollmentId().value());var newer=payments.create(3,old.enrollmentId().value()).payment();
        String before=snapshot(newer);assertEquals("CONFLICTING_PAYMENT",decide(old,e,true).failureReason());assertEquals(before,snapshot(newer));assertEquals("PENDING/PENDING",state(newer));
        CLOCK.now=newer.expiresAt();payments.get(3,newer.id().value());
        assertEquals("SUCCESS",decide(old,e,true).outcome());assertEquals("EXPIRED/ACTIVE",state(newer));
    }
    @Test void newerConfirmedObligationPreventsOldConfirmation()throws Exception {
        var old=create(3);CLOCK.now=old.expiresAt();enrollments.renew(3,old.enrollmentId().value());var newer=payments.create(3,old.enrollmentId().value()).payment();
        var e=evidence(newer);e=new ReconciliationEvidence(e.bankBin(),e.accountNumber(),e.bankTransactionReference(),CLOCK.now,e.observedAmount(),e.currency(),e.transferDescription(),e.transferReference(),true);
        assertEquals("SUCCESS",decide(newer,e,false).outcome());assertEquals("CONFLICTING_PAYMENT",decide(old,evidence(old),true).failureReason());assertEquals("EXPIRED/ACTIVE",state(old));
    }
    @Test void courseCutoffAndCancelledEnrollmentCannotActivate()throws Exception {
        var p=create(3);CLOCK.now=START.plusSeconds(3600);assertEquals("COURSE_NOT_ELIGIBLE",decide(p,evidence(p),true).failureReason());
        enrollments.cancel(3,p.enrollmentId().value());assertEquals("ENROLLMENT_NOT_PENDING",decide(p,evidence(p),true).failureReason());
        assertEquals("EXPIRED/CANCELLED",state(p));
    }
    @Test void ownershipAndEligibilityAreEnforced()throws Exception {
        var p=create(3);
        for(long actor:new long[]{2,3,5})assertThrows(PaymentFailure.class,()->service.decide(actor,course,p.id().value(),evidence(p),false));
        assertThrows(PaymentFailure.class,()->service.detail(5,course,p.id().value()));assertEquals(0,audits(p));
        sql("UPDATE users SET locked=true WHERE id=3");assertEquals("STUDENT_INELIGIBLE",decide(p,evidence(p),false).failureReason());
        sql("UPDATE users SET locked=true WHERE id=1");assertThrows(PaymentFailure.class,()->decide(p,evidence(p),false));assertEquals(1,audits(p));
    }
    @Test void accountReplacementDoesNotRedirectHistoricalPayment()throws Exception {
        var p=create(3);String before=snapshot(p);account(900,"00987654");
        assertEquals("SUCCESS",decide(p,evidence(p),false).outcome());assertEquals(before,snapshot(p));
        assertEquals(p.accountNumber(),service.detail(1,course,p.id().value()).accountNumber());
    }
    @Test void failureAfterPaymentUpdateRollsBackBothTransitions()throws Exception {
        var p=create(3);
        sql("CREATE FUNCTION test_reconciliation_abort() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.status='ACTIVE' THEN RAISE EXCEPTION 'fixture rollback'; END IF; RETURN NEW; END; $$");
        sql("CREATE TRIGGER test_reconciliation_abort BEFORE UPDATE ON enrollments FOR EACH ROW EXECUTE FUNCTION test_reconciliation_abort()");
        try {assertThrows(RuntimeException.class,()->decide(p,evidence(p),false));assertEquals("PENDING/PENDING",state(p));assertEquals(0,audits(p));}
        finally {sql("DROP TRIGGER test_reconciliation_abort ON enrollments");sql("DROP FUNCTION test_reconciliation_abort()");}
    }
    @Test void concurrentConfirmationsAllocateExactlyOnce()throws Exception {
        var p=create(3);var e=evidence(p);var results=race(()->decide(p,e,false),()->decide(p,e,false));
        assertTrue(results.stream().allMatch(a->a.outcome().equals("SUCCESS")));assertEquals(1,results.stream().filter(ReconciliationAttempt::allocated).count());
        assertEquals(2,audits(p));assertEquals("CONFIRMED/ACTIVE",state(p));
    }
    @Test void failedAuditInsertRollsBackSuccessfulStateChanges()throws Exception {
        var p=create(3);
        sql("CREATE FUNCTION test_audit_abort() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'fixture audit rollback'; END; $$");
        sql("CREATE TRIGGER test_audit_abort BEFORE INSERT ON payment_reconciliation_attempts FOR EACH ROW EXECUTE FUNCTION test_audit_abort()");
        try {assertThrows(RuntimeException.class,()->decide(p,evidence(p),false));assertEquals("PENDING/PENDING",state(p));assertEquals(0,audits(p));}
        finally {sql("DROP TRIGGER test_audit_abort ON payment_reconciliation_attempts");sql("DROP FUNCTION test_audit_abort()");}
        assertEquals("SUCCESS",decide(p,evidence(p),false).outcome());
    }
    @Test void databaseEnforcesSuccessfulAllocationUniqueness()throws Exception {
        var p=create(3);decide(p,evidence(p),false);
        String duplicate="INSERT INTO payment_reconciliation_attempts(payment_id,teacher_id,bank_bin,account_number,bank_transaction_reference,received_at,observed_amount,currency,transfer_description,transfer_reference,attested,late,attempted_at,outcome,failure_reason,allocated,replay_of) SELECT payment_id,teacher_id,bank_bin,account_number,bank_transaction_reference,received_at,observed_amount,currency,transfer_description,transfer_reference,attested,late,attempted_at,outcome,failure_reason,allocated,replay_of FROM payment_reconciliation_attempts WHERE payment_id="+p.id().value();
        assertEquals("23505",assertThrows(SQLException.class,()->sql(duplicate)).getSQLState());assertEquals(1,audits(p));
    }
    @Test void normalizationIsConservativeAndQueuePaginationIsStable()throws Exception {
        sql("UPDATE courses SET max_students=2 WHERE id="+course);var p=create(3);var q=create(4);var e=evidence(p);
        var padded=new ReconciliationEvidence(e.bankBin(),e.accountNumber(),"  "+e.bankTransactionReference()+"  ",e.receivedAt(),e.observedAmount(),e.currency(),e.transferDescription(),e.transferReference(),true);
        var first=decide(p,padded,false);assertEquals(e.bankTransactionReference(),first.evidence().bankTransactionReference());
        assertEquals(first.id(),decide(p,e,false).replayOf());
        var page=service.queue(1,course,0,1);assertTrue(page.hasNext());assertEquals(q.id(),page.items().get(0).id());
        var next=service.queue(1,course,1,1);assertFalse(next.hasNext());assertEquals(p.id(),next.items().get(0).id());
        assertThrows(PaymentFailure.class,()->service.queue(1,course,Integer.MAX_VALUE,100));
    }
    @Test void concurrentLatePaymentsCannotOverbookLastSeat()throws Exception {
        sql("UPDATE courses SET max_students=2 WHERE id="+course);var p=create(3);var q=create(4);CLOCK.now=p.expiresAt();sql("UPDATE courses SET max_students=1 WHERE id="+course);
        var results=race(()->decide(p,evidence(p),true),()->decide(q,evidence(q),true));
        assertEquals(1,results.stream().filter(a->a.outcome().equals("SUCCESS")).count());
        assertEquals(1,results.stream().filter(a->"MANUAL_RESOLUTION_REQUIRED_NO_CAPACITY".equals(a.failureReason())).count());
        assertEquals(1,count("SELECT count(*) FROM enrollments WHERE course_id="+course+" AND status='ACTIVE'"));
    }
    @Test void foreignBankProbesHaveIdenticalFailuresAndPreserveAllocations()throws Exception {
        var own=create(3);
        long ownCourse=course;
        createCourse(5);
        account(901,"00444444",5);
        var foreign=create(4);
        var bank=evidence(foreign);
        assertTrue(service.decide(5,course,foreign.id().value(),bank,false).allocated());
        for(String reference:List.of(bank.bankTransactionReference(),"unused-foreign-reference")) {
            var probe=new ReconciliationEvidence(foreign.bankBin(),foreign.accountNumber(),reference,START,
                new BigDecimal(own.finalAmount()),"VND",own.transferReference(),own.transferReference(),true);
            var failed=service.decide(1,ownCourse,own.id().value(),probe,false);
            assertEquals("RECEIVER_MISMATCH",failed.failureReason());
            assertFalse(failed.allocated());
        }
        assertEquals(2,audits(own));assertEquals("PENDING/PENDING",state(own));
        assertEquals(1,audits(foreign));assertEquals("CONFIRMED/ACTIVE",state(foreign));
        assertTrue(service.decide(1,ownCourse,own.id().value(),evidence(own),false).allocated());
        assertEquals(3,audits(own));
    }
    @Test void truncateAloneOrWithAnotherTableCannotEraseHistory()throws Exception {
        var p=create(3);var e=evidence(p);var allocated=decide(p,e,false);
        String history="SELECT string_agg(to_jsonb(a)::text,'|' ORDER BY id) FROM payment_reconciliation_attempts a";
        String before=text(history);
        sql("CREATE TABLE truncate_companion(id integer PRIMARY KEY)");
        sql("INSERT INTO truncate_companion VALUES (1)");
        for(String tables:List.of("payment_reconciliation_attempts","truncate_companion,payment_reconciliation_attempts")) {
            var ex=assertThrows(SQLException.class,()->sql("TRUNCATE "+tables));
            assertEquals("23514",ex.getSQLState());assertEquals(before,text(history));assertEquals(1,count("SELECT count(*) FROM truncate_companion"));
        }
        var replay=decide(p,e,false);assertEquals(allocated.id(),replay.replayOf());assertEquals(2,audits(p));
    }
    @Test void lateConfirmationAndRenewalSerializeOnSameEnrollment()throws Exception {
        var p=create(3);var e=evidence(p);long id=p.enrollmentId().value();
        CLOCK.now=p.expiresAt();String original=snapshot(p);
        var results=lifecycleRace(()->decide(p,e,true).outcome(),()->{
            var renewed=enrollments.renew(3,id);
            assertEquals(CLOCK.now.plusSeconds(900),renewed.seatReservedUntil());
            return "RENEWED";
        });
        assertEquals("SUCCESS",results.get(0));
        assertTrue(Set.of("RENEWED","ENROLLMENT_STATE_CONFLICT").contains(results.get(1)));
        assertCommittedSeat(p,original);
    }
    @Test void cancellationAndConfirmationSerializeOnSameEnrollment()throws Exception {
        var p=create(3);String original=snapshot(p);var e=evidence(p);
        var results=lifecycleRace(()->decide(p,e,false).outcome(),()->enrollments.cancel(3,p.enrollmentId().value()).status());
        assertEquals("SUCCESS",results.get(0));
        // Cancellation first hits the pending Payment; cancellation second hits the ACTIVE Enrollment.
        assertTrue(Set.of("PAYMENT_COORDINATION_REQUIRED","ENROLLMENT_STATE_CONFLICT").contains(results.get(1)));
        assertCommittedSeat(p,original);
    }
    void assertCommittedSeat(PaymentDto p,String original)throws Exception {
        assertEquals(original,snapshot(p));assertEquals("CONFIRMED/ACTIVE",state(p));
        assertEquals(1,count("SELECT count(*) FROM enrollments WHERE course_id="+course+" AND status='ACTIVE'"));
        assertEquals(1,count("SELECT count(*) FROM enrollments WHERE id="+p.enrollmentId().value()+" AND seat_reserved_until IS NULL AND cancelled_at IS NULL"));
        assertEquals(0,count("SELECT count(*) FROM payments WHERE enrollment_id="+p.enrollmentId().value()+" AND status='PENDING'"));
        assertEquals(1,audits(p));
        assertEquals(1,count("SELECT count(*) FROM payment_reconciliation_attempts WHERE payment_id="+p.id().value()+" AND outcome='SUCCESS' AND allocated"));
    }
    List<String> lifecycleRace(Callable<String> confirmation,Callable<String> student)throws Exception {
        return race(confirmation,student,ex->ex.reason().name());
    }
    List<ReconciliationAttempt> race(Callable<ReconciliationAttempt> a,Callable<ReconciliationAttempt> b)throws Exception {
        return race(a,b,ex->{throw ex;});
    }
    <T> List<T> race(Callable<T> a,Callable<T> b,java.util.function.Function<EnrollmentFailure,T> studentFailure)throws Exception {
        var pool=Executors.newFixedThreadPool(2);var barrier=new CyclicBarrier(2);var connections=ConcurrentHashMap.<Integer>newKeySet();
        try {
            var x=pool.submit(()->concurrent(barrier,connections,a));var y=pool.submit(()->{
                try{return concurrent(barrier,connections,b);}
                catch(EnrollmentFailure ex){return studentFailure.apply(ex);} // Domain exception has already rolled back.
            });
            var result=List.of(x.get(15,TimeUnit.SECONDS),y.get(15,TimeUnit.SECONDS));assertEquals(2,connections.size());return result;
        }finally{pool.shutdownNow();}
    }
    <T> T concurrent(CyclicBarrier barrier,Set<Integer> connections,Callable<T> operation) {
        var tx=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));tx.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
        return tx.execute(state->{
            var em=org.springframework.orm.jpa.EntityManagerFactoryUtils.getTransactionalEntityManager(context.getBean(jakarta.persistence.EntityManagerFactory.class));
            connections.add(((Number)em.createNativeQuery("SELECT pg_backend_pid()").getSingleResult()).intValue());
            try {barrier.await(5,TimeUnit.SECONDS);return operation.call();}catch(RuntimeException ex){throw ex;}catch(Exception ex){throw new IllegalStateException(ex);}
        });
    }
}
