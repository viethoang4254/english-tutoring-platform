package com.englishlearning.features.payment;

import com.englishlearning.EnglishLearningApplication;
import com.englishlearning.features.payment.service.*;
import com.englishlearning.features.enrollment.service.*;
import com.englishlearning.features.paymentaccount.service.ReceivingAccountReviewService;
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
class PaymentPersistenceTests {
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
        Clock paymentTestClock(){return CLOCK;}
    }
    String url,user,password,legacyBefore;
    ConfigurableApplicationContext context;PaymentService payments;EnrollmentService enrollments;
    Connection connect()throws Exception{return DriverManager.getConnection(url,user,password);}
    void sql(String value)throws Exception{try(var c=connect();var s=c.createStatement()){s.execute(value);}}
    String text(String value)throws Exception{try(var c=connect();var s=c.createStatement();var r=s.executeQuery(value)){assertTrue(r.next());return r.getString(1);}}
    long count(String value)throws Exception{return Long.parseLong(text(value));}
    @BeforeAll void start()throws Exception {
        url=System.getenv("PAYMENT_TEST_URL");user=System.getenv("PAYMENT_TEST_USER");password=System.getenv("PAYMENT_TEST_PASSWORD");
        assertEquals("jdbc:postgresql://127.0.0.1:15442/task022c_payments",url);assertEquals("task022c_test",user);
        assertEquals(0,count("SELECT count(*) FROM pg_tables WHERE schemaname NOT IN ('pg_catalog','information_schema')"));
        assertEquals(8,Flyway.configure().dataSource(url,user,password).target("8").cleanDisabled(true).load().migrate().migrationsExecuted);
        sql("INSERT INTO users(id,email,password_hash,full_name,role,email_verified,locked,created_at,updated_at) VALUES (1,'teacher@example.invalid','unused','Teacher','TEACHER',true,false,now(),now()),(2,'admin@example.invalid','unused','Admin','ADMIN',true,false,now(),now()),(3,'student@example.invalid','unused','Student','STUDENT',false,false,now(),now()),(4,'other@example.invalid','unused','Other','STUDENT',true,false,now(),now()),(5,'legacy@example.invalid','unused','Legacy','STUDENT',true,false,now(),now())");
        sql("INSERT INTO teacher_applications(teacher_id,specialization,teaching_experience,introduction,status,submitted_at,reviewed_at,reviewed_by,created_at,updated_at) VALUES (1,'Test','Test','Test','APPROVED',now(),now(),2,now(),now())");
        sql("INSERT INTO course_categories(id,name,created_at,updated_at) VALUES (100,'Payments',now(),now())");
        sql("INSERT INTO courses(id,teacher_id,category_id,name,description,tuition,currency,min_students,max_students,planned_start_date,timezone,session_count,status,created_at,updated_at) VALUES (100,1,100,'Legacy','Legacy',100,'VND',1,1,current_date,'Asia/Ho_Chi_Minh',1,'DRAFT',now(),now()),(200,1,100,'Payment fixture','Fixture',100.50,'VND',1,1,current_date,'Asia/Ho_Chi_Minh',1,'PUBLISHED',now(),now())");
        sql("INSERT INTO sessions(course_id,session_number,scheduled_start_at,scheduled_end_at,status,created_at,updated_at) VALUES (200,1,'"+START.plusSeconds(3600)+"','"+START.plusSeconds(7200)+"','SCHEDULED',now(),now())");
        sql("INSERT INTO enrollments(id,student_id,course_id,status,created_at,updated_at) VALUES (100,5,100,'PENDING',now(),now())");
        sql("INSERT INTO teacher_payment_accounts(id,teacher_id,bank_code,account_number,account_holder_name,provider,status,created_at,updated_at) VALUES (100,1,'LEGACY','000123','Legacy Holder','VIETQR','INACTIVE',now(),now())");
        sql("INSERT INTO payments(id,enrollment_id,teacher_payment_account_id,payment_code,provider,original_amount,discount_amount,final_amount,currency,status,expires_at,created_at,updated_at) VALUES (100,100,100,'LEGACY','VIETQR',100.50,0.25,100.25,'VND','PENDING',now()-interval '1 hour',now()-interval '2 hours',now())");
        legacyBefore=legacy();
        var flyway=Flyway.configure().dataSource(url,user,password).cleanDisabled(true).load();assertEquals(3,flyway.migrate().migrationsExecuted);flyway.validate();
        assertEquals(11,count("SELECT count(*) FROM flyway_schema_history WHERE success"));
        var props=new HashMap<String,Object>();props.put("spring.datasource.url",url);props.put("spring.datasource.username",user);props.put("spring.datasource.password",password);
        props.put("spring.flyway.enabled",false);props.put("spring.jpa.hibernate.ddl-auto","validate");props.put("spring.sql.init.mode","never");
        props.put("spring.datasource.hikari.maximum-pool-size",4);props.put("spring.datasource.hikari.connection-init-sql","SET lock_timeout TO '5s'");
        byte[] key=new byte[32];new java.security.SecureRandom().nextBytes(key);props.put("app.security.password-reset.hmac-secret",Base64.getEncoder().encodeToString(key));
        context=new SpringApplicationBuilder(EnglishLearningApplication.class,TimeFixture.class).web(WebApplicationType.NONE)
            .initializers(c->c.getEnvironment().getPropertySources().addFirst(new MapPropertySource("isolated-payment",props))).run();
        payments=context.getBean(PaymentService.class);enrollments=context.getBean(EnrollmentService.class);
    }
    String legacy()throws Exception{return text("SELECT id||'|'||enrollment_id||'|'||teacher_payment_account_id||'|'||payment_code||'|'||original_amount||'|'||discount_amount::numeric(15,2)||'|'||final_amount||'|'||status||'|'||expires_at||'|'||created_at FROM payments WHERE id=100");}
    @AfterAll void close(){if(context!=null)context.close();}
    @BeforeEach void fixture()throws Exception {
        CLOCK.now=START;sql("DELETE FROM payments WHERE id<>100");sql("DELETE FROM enrollments WHERE id<>100");sql("DELETE FROM teacher_payment_accounts WHERE id<>100");
        sql("UPDATE users SET locked=false WHERE id IN (1,3)");
        sql("UPDATE courses SET tuition=100.50,discount_percent=NULL,discount_start_at=NULL,discount_end_at=NULL,max_students=1,status='PUBLISHED' WHERE id=200");
        sql("UPDATE sessions SET scheduled_start_at='"+START.plusSeconds(3600)+"' WHERE course_id=200");
        account(200,"00123456");context.getBean(ReceivingAccountReviewService.class).review(2,200,true,null);
    }
    void account(long id,String number)throws Exception {
        String key=UUID.randomUUID().toString();
        sql("INSERT INTO teacher_payment_accounts(id,teacher_id,bank_code,bank_bin,bank_name,account_number,account_holder_name,provider,status,review_status,qr_object,qr_sha256,qr_size,submission_key,request_sha256,created_at,updated_at) VALUES ("+id+",1,'VCB','970436','Bank','"+number+"','Teacher','VIETQR','INACTIVE','PENDING','"+key+".png',repeat('a',64),100,'"+key+"',repeat('b',64),now(),now())");
    }
    long reserve(){return enrollments.create(3,200).enrollment().id().value();}
    @Test void v8HistoryAndLegacyIntegritySurviveV9()throws Exception {
        assertEquals(legacyBefore,legacy());assertEquals("LEGACY",text("SELECT monetary_policy FROM payments WHERE id=100"));
        assertEquals("PENDING",text("SELECT status FROM enrollments WHERE id=100"));
        var failure=assertThrows(SQLException.class,()->sql("INSERT INTO payments(enrollment_id,teacher_payment_account_id,payment_code,provider,original_amount,discount_amount,final_amount,currency,status,expires_at,created_at,updated_at) VALUES (100,100,'INVALID-LEGACY','VIETQR',100,0,101,'VND','EXPIRED',now()+interval '1 hour',now(),now())"));
        assertEquals("23514",failure.getSQLState());assertEquals(legacyBefore,legacy());
    }
    @Test void checkoutPreviewIsReadOnlyAndRetainsPendingSnapshot()throws Exception {
        var checkout=context.getBean(CheckoutService.class);
        String before=text("SELECT jsonb_agg(to_jsonb(e) ORDER BY id)::text FROM enrollments e");
        String beforePayments=text("SELECT jsonb_agg(to_jsonb(p) ORDER BY id)::text FROM payments p");
        var preview=checkout.get(3,200);assertTrue(preview.available());assertEquals("101",preview.price().finalAmount());
        assertNull(preview.enrollment());assertNull(preview.paymentId());
        assertEquals(before,text("SELECT jsonb_agg(to_jsonb(e) ORDER BY id)::text FROM enrollments e"));
        assertEquals(beforePayments,text("SELECT jsonb_agg(to_jsonb(p) ORDER BY id)::text FROM payments p"));
        long e=reserve();var payment=payments.create(3,e).payment();
        sql("UPDATE courses SET tuition=999 WHERE id=200");
        var recovered=checkout.get(3,200);assertTrue(recovered.available());
        assertEquals(payment.finalAmount(),recovered.price().finalAmount());assertEquals(payment.id(),recovered.paymentId());
        assertNull(checkout.get(4,200).paymentId());assertFalse(checkout.get(4,200).available());
        CLOCK.now=payment.expiresAt();assertEquals("ENROLLMENT_RENEWAL_REQUIRED",checkout.get(3,200).blockedReason());
        assertEquals("PENDING",text("SELECT status FROM payments WHERE id="+payment.id().value()));
        assertEquals("PENDING",text("SELECT status FROM enrollments WHERE id="+e));assertEquals(legacyBefore,legacy());
    }
    @Test void checkoutRepricesBeforeCreationWithoutChangingPreviewOrActivating()throws Exception {
        var checkout=context.getBean(CheckoutService.class);var preview=checkout.get(3,200);
        sql("UPDATE courses SET tuition=200 WHERE id=200");
        var payment=payments.create(3,reserve()).payment();assertEquals("101",preview.price().finalAmount());assertEquals("200",payment.finalAmount());
        assertEquals("PENDING",payment.status());assertEquals("PENDING",enrollments.get(3,payment.enrollmentId().value()).status());
        sql("UPDATE courses SET status='CANCELLED' WHERE id=200");
        assertEquals(EnrollmentFailure.Reason.COURSE_NOT_FOUND,assertThrows(EnrollmentFailure.class,()->checkout.get(3,200)).reason());
    }
    @Test void validCreationRetriesSnapshotsAndNoActivation()throws Exception {
        long e=reserve();var first=payments.create(3,e);assertTrue(first.created());var p=first.payment();
        assertEquals("101",p.finalAmount());assertEquals("0.5",p.roundingAdjustment());assertEquals("00123456",p.accountNumber());
        assertEquals(START.plusSeconds(900),p.expiresAt());assertTrue(p.transferReference().matches("ELP[0-9A-F]{22}"));
        sql("UPDATE courses SET tuition=999 WHERE id=200");
        var retry=payments.create(3,e);assertFalse(retry.created());assertEquals(p,retry.payment());
        assertEquals("PENDING",text("SELECT status FROM enrollments WHERE id="+e));assertEquals("PENDING",payments.get(3,p.id().value()).status());
        assertEquals(1,count("SELECT count(*) FROM payments WHERE id<>100"));
    }
    @Test void discountPrecisionAndWindowBoundaries()throws Exception {
        sql("UPDATE courses SET tuition=123.45,discount_percent=12.34,discount_start_at='"+START+"',discount_end_at='"+START.plusSeconds(100)+"' WHERE id=200");
        var p=payments.create(3,reserve()).payment();assertEquals("15.23373",p.exactDiscountAmount());assertEquals("108",p.finalAmount());assertEquals("-0.21627",p.roundingAdjustment());
        sql("DELETE FROM payments WHERE id<>100");CLOCK.now=START.plusSeconds(100);
        assertEquals("0",payments.create(3,reserve()).payment().discountPercent());
    }
    @Test void accountReplacementChangesOnlyNewPayment()throws Exception {
        long e=reserve();var first=payments.create(3,e).payment();account(201,"00987654");context.getBean(ReceivingAccountReviewService.class).review(2,201,true,null);
        assertEquals(first,payments.get(3,first.id().value()));assertEquals(first,payments.create(3,e).payment());
        CLOCK.now=first.expiresAt();enrollments.renew(3,e);var next=payments.create(3,e).payment();
        assertEquals("00987654",next.accountNumber());assertNotEquals(first.transferReference(),next.transferReference());assertEquals("EXPIRED",payments.get(3,first.id().value()).status());
        assertEquals("00123456",payments.get(3,first.id().value()).accountNumber());assertEquals(legacyBefore,legacy());
    }
    @Test void noApprovedSelectedAccountCannotCreatePayment()throws Exception {
        long e=reserve();sql("UPDATE teacher_payment_accounts SET status='INACTIVE' WHERE id=200");
        for(boolean rejected:new boolean[]{false,true}) {
            if(!rejected)account(201,"00222222");else context.getBean(ReceivingAccountReviewService.class).review(2,201,false,"Fixture rejection");
            assertEquals(PaymentFailure.Reason.RECEIVING_ACCOUNT_UNAVAILABLE,assertThrows(PaymentFailure.class,()->payments.create(3,e)).reason());
        }
        assertEquals(0,count("SELECT count(*) FROM payments WHERE id<>100"));
    }
    @Test void exactExpiryRequiresExplicitRenewalAndAllowsCancellation()throws Exception {
        long e=reserve();var p=payments.create(3,e).payment();CLOCK.now=p.expiresAt();
        assertEquals(PaymentFailure.Reason.RESERVATION_EXPIRED,assertThrows(PaymentFailure.class,()->payments.create(3,e)).reason());
        assertEquals("PENDING",text("SELECT status FROM payments WHERE id="+p.id().value()));
        String snapshot=text("SELECT to_jsonb(p)-'status'-'updated_at' FROM payments p WHERE id="+p.id().value());
        assertEquals("CANCELLED",enrollments.cancel(3,e).status());
        assertEquals("EXPIRED",text("SELECT status FROM payments WHERE id="+p.id().value()));
        assertEquals(snapshot,text("SELECT to_jsonb(p)-'status'-'updated_at' FROM payments p WHERE id="+p.id().value()));
        assertEquals(legacyBefore,legacy());
    }
    @Test void concurrentRequestsReturnSameObligation()throws Exception {
        long e=reserve();var pool=Executors.newFixedThreadPool(2);var barrier=new CyclicBarrier(2);var connections=ConcurrentHashMap.<Integer>newKeySet();
        try {
            var a=pool.submit(()->concurrent(barrier,connections,()->payments.create(3,e).payment().id().value()));
            var b=pool.submit(()->concurrent(barrier,connections,()->payments.create(3,e).payment().id().value()));
            assertEquals(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));assertEquals(2,connections.size());
            assertEquals(1,count("SELECT count(*) FROM payments WHERE id<>100"));
        }finally{pool.shutdownNow();}
    }
    @Test void expiryAndRenewalRacePreservesHistoryAndOnePending()throws Exception {
        long e=reserve();var old=payments.create(3,e).payment();CLOCK.now=old.expiresAt();
        var pool=Executors.newFixedThreadPool(2);var barrier=new CyclicBarrier(2);var connections=ConcurrentHashMap.<Integer>newKeySet();
        try {
            var a=pool.submit(()->concurrent(barrier,connections,()->{payments.get(3,old.id().value());return 1L;}));
            var b=pool.submit(()->concurrent(barrier,connections,()->{enrollments.renew(3,e);return payments.create(3,e).payment().id().value();}));
            a.get(15,TimeUnit.SECONDS);long next=b.get(15,TimeUnit.SECONDS);assertNotEquals(old.id().value(),next);
            assertEquals(2,connections.size());assertEquals("EXPIRED",payments.get(3,old.id().value()).status());
            assertEquals(1,count("SELECT count(*) FROM payments WHERE enrollment_id="+e+" AND status='PENDING'"));
        }finally{pool.shutdownNow();}
    }
    long concurrent(CyclicBarrier barrier,Set<Integer> connections,java.util.function.LongSupplier operation) {
        var tx=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        tx.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
        return tx.execute(state->{
            var em=org.springframework.orm.jpa.EntityManagerFactoryUtils.getTransactionalEntityManager(context.getBean(jakarta.persistence.EntityManagerFactory.class));
            connections.add(((Number)em.createNativeQuery("SELECT pg_backend_pid()").getSingleResult()).intValue());
            try{barrier.await(5,TimeUnit.SECONDS);}catch(Exception e){throw new IllegalStateException("Fixture rendezvous failed",e);}
            return operation.getAsLong();
        });
    }
    @Test void crossStudentRolesCapacityAndCutoffAreEnforced()throws Exception {
        long e=reserve();var p=payments.create(3,e).payment();assertEquals(PaymentFailure.Reason.PAYMENT_NOT_FOUND,assertThrows(PaymentFailure.class,()->payments.get(4,p.id().value())).reason());
        assertEquals(PaymentFailure.Reason.ENROLLMENT_NOT_FOUND,assertThrows(PaymentFailure.class,()->payments.create(4,e)).reason());
        for(long actor:new long[]{1,2})assertEquals(PaymentFailure.Reason.ACCESS_DENIED,assertThrows(PaymentFailure.class,()->payments.create(actor,e)).reason());
        assertEquals(EnrollmentFailure.Reason.COURSE_FULL,assertThrows(EnrollmentFailure.class,()->enrollments.create(4,200)).reason());
        sql("UPDATE sessions SET scheduled_start_at='"+START+"' WHERE course_id=200");
        assertEquals(PaymentFailure.Reason.COURSE_NOT_PAYABLE,assertThrows(PaymentFailure.class,()->payments.create(3,e)).reason());
    }
    @Test void rollbackAndImmutableEvidencePreserveHistory()throws Exception {
        long e=reserve();var tx=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        assertThrows(IllegalStateException.class,()->tx.execute(s->{payments.create(3,e);throw new IllegalStateException("Fixture rollback");}));
        assertEquals(0,count("SELECT count(*) FROM payments WHERE id<>100"));var p=payments.create(3,e).payment();
        assertEquals("23514",assertThrows(SQLException.class,()->sql("UPDATE payments SET account_number='999' WHERE id="+p.id().value())).getSQLState());
        assertEquals(p,payments.get(3,p.id().value()));assertEquals(legacyBefore,legacy());
    }
    @Test void expiredPaymentDoesNotBlockRenewalButCapacityStillDoes()throws Exception {
        long e=reserve();var p=payments.create(3,e).payment();CLOCK.now=p.expiresAt();
        var other=enrollments.create(4,200).enrollment();
        assertEquals(EnrollmentFailure.Reason.COURSE_FULL,assertThrows(EnrollmentFailure.class,()->enrollments.renew(3,e)).reason());
        assertEquals("EXPIRED",payments.get(3,p.id().value()).status());
        enrollments.cancel(4,other.id().value());
        var renewed=enrollments.renew(3,e);assertFalse(renewed.reservationExpired());
        var next=payments.create(3,e).payment();assertNotEquals(p.id(),next.id());
        assertEquals(1,count("SELECT count(*) FROM enrollments WHERE course_id=200 AND (status='ACTIVE' OR (status='PENDING' AND seat_reserved_until>'"+CLOCK.now+"'))"));
    }
    @Test void invalidEnrollmentCourseAndTeacherStatesDoNotCreatePayments()throws Exception {
        long e=reserve();
        sql("UPDATE courses SET status='DRAFT' WHERE id=200");
        assertEquals(PaymentFailure.Reason.COURSE_NOT_PAYABLE,assertThrows(PaymentFailure.class,()->payments.create(3,e)).reason());
        sql("UPDATE courses SET status='PUBLISHED' WHERE id=200");sql("UPDATE users SET locked=true WHERE id=1");
        assertEquals(PaymentFailure.Reason.RECEIVING_ACCOUNT_UNAVAILABLE,assertThrows(PaymentFailure.class,()->payments.create(3,e)).reason());
        sql("UPDATE users SET locked=false WHERE id=1");enrollments.cancel(3,e);
        assertEquals(PaymentFailure.Reason.ENROLLMENT_NOT_PAYABLE,assertThrows(PaymentFailure.class,()->payments.create(3,e)).reason());
        assertEquals(0,count("SELECT count(*) FROM payments WHERE id<>100"));assertEquals(legacyBefore,legacy());
    }
    @Test void accountSelectionAndCreationRaceHaveConsistentSnapshot()throws Exception {
        long e=reserve();account(201,"00987654");
        var pool=Executors.newFixedThreadPool(2);var barrier=new CyclicBarrier(2);var connections=ConcurrentHashMap.<Integer>newKeySet();
        try {
            var a=pool.submit(()->concurrent(barrier,connections,()->payments.create(3,e).payment().id().value()));
            var b=pool.submit(()->concurrent(barrier,connections,()->{context.getBean(ReceivingAccountReviewService.class).review(2,201,true,null);return 1L;}));
            long id=a.get(15,TimeUnit.SECONDS);b.get(15,TimeUnit.SECONDS);var payment=payments.get(3,id);
            assertEquals(payment.receivingAccountId().value()==200?"00123456":"00987654",payment.accountNumber());
            assertTrue(Set.of(200L,201L).contains(payment.receivingAccountId().value()));
            assertEquals(1,count("SELECT count(*) FROM teacher_payment_accounts WHERE teacher_id=1 AND status='ACTIVE'"));
            assertEquals(payment,payments.create(3,e).payment());assertEquals(2,connections.size());
        }finally{pool.shutdownNow();}
    }
    @Test void newMonetaryConstraintsRejectInvalidSnapshots()throws Exception {
        for(String values:List.of("100.50,-1,0,101,1.50","100.50,0,0,101,0","100.50,0,0,100.50,0")) {
            var error=assertThrows(org.postgresql.util.PSQLException.class,()->insertMoney(values));
            assertEquals("23514",error.getSQLState());assertEquals("ck_payments_amounts",error.getServerErrorMessage().getConstraint());
        }
        insertMoney("100.50,0,0,101,0.50");insertMoney("100.49,0,0,100,-0.49");
        assertEquals(legacyBefore,legacy());
    }
    @Test void defaultPolicyRequiresValidMoneyAndSnapshots()throws Exception {
        long id=insertMoney("100.50,0,0,101,0.50",null,true);
        assertEquals("VND_HALF_UP_V1",text("SELECT monetary_policy FROM payments WHERE id="+id));
        var missing=assertThrows(org.postgresql.util.PSQLException.class,()->insertMoney("100.50,0,0,101,0.50",null,false));
        assertEquals("ck_payment_transfer_snapshot",missing.getServerErrorMessage().getConstraint());
        var invalid=assertThrows(org.postgresql.util.PSQLException.class,()->insertMoney("100.50,0,0,101,0",null,true));
        assertEquals("ck_payments_amounts",invalid.getServerErrorMessage().getConstraint());
    }
    @Test void explicitLegacyInsertCannotBypassNewPolicy()throws Exception {
        long before=count("SELECT count(*) FROM payments");
        var failure=assertThrows(org.postgresql.util.PSQLException.class,()->insertMoney("100.50,0,NULL,100.50,0","LEGACY",false));
        assertEquals("23514",failure.getSQLState());assertEquals("ck_payment_new_policy",failure.getServerErrorMessage().getConstraint());
        assertEquals(before,count("SELECT count(*) FROM payments"));assertEquals(legacyBefore,legacy());
    }
    @Test void newPaymentPolicyCannotBeDowngraded()throws Exception {
        long id=insertMoney("100.50,0,0,101,0.50",null,true);
        String before=text("SELECT to_jsonb(p) FROM payments p WHERE id="+id);
        assertEquals("23514",assertThrows(SQLException.class,()->sql("UPDATE payments SET monetary_policy='LEGACY' WHERE id="+id)).getSQLState());
        assertEquals(before,text("SELECT to_jsonb(p) FROM payments p WHERE id="+id));
    }
    @Test void legacyLifecycleUpdatePreservesHistoricalSnapshot()throws Exception {
        String before=text("SELECT to_jsonb(p)-'status'-'updated_at' FROM payments p WHERE id=100");
        try(var c=connect()) {
            c.setAutoCommit(false);
            try(var s=c.createStatement()) {
                assertEquals(1,s.executeUpdate("UPDATE payments SET status='EXPIRED',updated_at=now() WHERE id=100"));
                try(var r=s.executeQuery("SELECT status,monetary_policy,to_jsonb(p)-'status'-'updated_at' FROM payments p WHERE id=100")) {
                    assertTrue(r.next());assertEquals("EXPIRED",r.getString(1));assertEquals("LEGACY",r.getString(2));assertEquals(before,r.getString(3));
                }
            }finally{c.rollback();}
        }
        assertEquals(legacyBefore,legacy());
    }
    @Test void creationVersusExplicitRenewalPreservesOnePending()throws Exception {
        long e=reserve();var old=payments.create(3,e).payment();CLOCK.now=old.expiresAt();
        String snapshot=text("SELECT to_jsonb(p)-'status'-'updated_at' FROM payments p WHERE id="+old.id().value());
        var pool=Executors.newFixedThreadPool(2);var barrier=new CyclicBarrier(2);var connections=ConcurrentHashMap.<Integer>newKeySet();
        try {
            var creation=pool.submit(()->{
                try{return concurrent(barrier,connections,()->payments.create(3,e).payment().id().value());}
                catch(PaymentFailure failure){assertEquals(PaymentFailure.Reason.RESERVATION_EXPIRED,failure.reason());return 0L;}
            });
            var renewal=pool.submit(()->concurrent(barrier,connections,()->enrollments.renew(3,e).id().value()));
            long created=creation.get(15,TimeUnit.SECONDS);assertEquals(e,renewal.get(15,TimeUnit.SECONDS).longValue());
            assertEquals(2,connections.size());assertEquals("PENDING",text("SELECT status FROM enrollments WHERE id="+e));
            assertEquals("EXPIRED",text("SELECT status FROM payments WHERE id="+old.id().value()));
            assertEquals(created==0?0:1,count("SELECT count(*) FROM payments WHERE enrollment_id="+e+" AND status='PENDING'"));
            var next=payments.create(3,e).payment();if(created!=0)assertEquals(created,next.id().value());
            assertEquals(START.plusSeconds(1800),next.expiresAt());assertNotEquals(old.id(),next.id());
            assertEquals(next,payments.create(3,e).payment());
            assertEquals(1,count("SELECT count(*) FROM payments WHERE enrollment_id="+e+" AND status='PENDING'"));
            assertEquals(snapshot,text("SELECT to_jsonb(p)-'status'-'updated_at' FROM payments p WHERE id="+old.id().value()));assertEquals(legacyBefore,legacy());
        }finally{pool.shutdownNow();}
    }
    @Test void creationVersusCancellationHasOnlyPermittedOutcomes()throws Exception {
        long e=reserve();var pool=Executors.newFixedThreadPool(2);var barrier=new CyclicBarrier(2);var connections=ConcurrentHashMap.<Integer>newKeySet();
        try {
            var creation=pool.submit(()->{
                try{return concurrent(barrier,connections,()->payments.create(3,e).payment().id().value());}
                catch(PaymentFailure failure){assertEquals(PaymentFailure.Reason.ENROLLMENT_NOT_PAYABLE,failure.reason());return 0L;}
            });
            var cancellation=pool.submit(()->{
                try{return concurrent(barrier,connections,()->{assertEquals("CANCELLED",enrollments.cancel(3,e).status());return 1L;});}
                catch(EnrollmentFailure failure){assertEquals(EnrollmentFailure.Reason.PAYMENT_COORDINATION_REQUIRED,failure.reason());return 0L;}
            });
            long created=creation.get(15,TimeUnit.SECONDS),cancelled=cancellation.get(15,TimeUnit.SECONDS);
            assertEquals(2,connections.size());assertEquals(created==0?1L:0L,cancelled);
            assertEquals(created==0?"CANCELLED":"PENDING",text("SELECT status FROM enrollments WHERE id="+e));
            assertEquals(created==0?0:1,count("SELECT count(*) FROM payments WHERE enrollment_id="+e));
            if(created!=0){var p=payments.get(3,created);assertEquals("PENDING",p.status());assertEquals(START.plusSeconds(900),p.expiresAt());assertEquals(p,payments.create(3,e).payment());}
            assertEquals(legacyBefore,legacy());
        }finally{pool.shutdownNow();}
    }
    void insertMoney(String values)throws Exception {insertMoney(values,"VND_HALF_UP_V1",true);}
    long insertMoney(String values,String policy,boolean snapshots)throws Exception {
        String code="ELP"+UUID.randomUUID().toString().replace("-","").substring(0,22).toUpperCase(Locale.ROOT);
        return count("INSERT INTO payments(enrollment_id,teacher_payment_account_id,payment_code,provider,original_amount,discount_amount,discount_percent,final_amount,rounding_adjustment,currency,status,expires_at,created_at,updated_at"
            +(policy==null?"":",monetary_policy")+(snapshots?",bank_bin,bank_name,account_number,account_holder_name,qr_payload":"")
            +") VALUES (100,200,'"+code+"','VIETQR',"+values+",'VND','EXPIRED',now()+interval '1 hour',now(),now()"
            +(policy==null?"":",'"+policy+"'")+(snapshots?",'970436','Bank','00123456','Teacher',repeat('0',40)":"")+") RETURNING id");
    }
}
