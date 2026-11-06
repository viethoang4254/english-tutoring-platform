package com.englishlearning.features.paymentaccount;
import com.englishlearning.EnglishLearningApplication;
import com.englishlearning.features.paymentaccount.service.*;
import com.englishlearning.features.paymentaccount.storage.PrivateQrStorage;
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
import static org.junit.jupiter.api.Assertions.*;
@EnabledIfSystemProperty(named="receivingPersistence",matches="true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReceivingAccountPersistenceTests {
    String url,user,password;ConfigurableApplicationContext context;ReceivingAccountService service;
    static final Map<String,byte[]> objects=new ConcurrentHashMap<>();
    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods=false)
    static class StorageFixture {
        @org.springframework.context.annotation.Bean @org.springframework.context.annotation.Primary
        PrivateQrStorage storage(){return new PrivateQrStorage(){public void put(String key,byte[] png){assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());objects.put(key,png.clone());}public void delete(String key){assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());objects.remove(key);}public byte[] read(String key){return objects.get(key).clone();}};}
    }
    Connection connect()throws Exception{return DriverManager.getConnection(url,user,password);}
    void sql(String sql)throws Exception{try(var c=connect();var s=c.createStatement()){s.execute(sql);}}
    long count(String sql)throws Exception{try(var c=connect();var s=c.createStatement();var r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
    String text(String sql)throws Exception{try(var c=connect();var s=c.createStatement();var r=s.executeQuery(sql)){r.next();return r.getString(1);}}
    @BeforeAll void start()throws Exception {
        url=System.getenv("RECEIVING_TEST_URL");user=System.getenv("RECEIVING_TEST_USER");password=System.getenv("RECEIVING_TEST_PASSWORD");
        assertEquals("jdbc:postgresql://127.0.0.1:55441/task022_receiving",url);assertEquals("task022_test",user);
        assertEquals(0,count("SELECT count(*) FROM pg_tables WHERE schemaname NOT IN ('pg_catalog','information_schema')"));
        Flyway.configure().dataSource(url,user,password).target("6").cleanDisabled(true).load().migrate();
        sql("INSERT INTO users(id,email,password_hash,full_name,role,email_verified,locked,created_at,updated_at) VALUES (1,'teacher@example.invalid','unused','Teacher','TEACHER',true,false,now(),now()),(2,'admin@example.invalid','unused','Admin','ADMIN',true,false,now(),now()),(3,'student@example.invalid','unused','Student','STUDENT',true,false,now(),now()),(4,'pending@example.invalid','unused','Pending','TEACHER',true,false,now(),now()),(5,'rejected@example.invalid','unused','Rejected','TEACHER',true,false,now(),now()),(6,'other@example.invalid','unused','Other','TEACHER',true,false,now(),now())");
        sql("INSERT INTO teacher_applications(teacher_id,specialization,teaching_experience,introduction,status,submitted_at,reviewed_at,reviewed_by,rejection_reason,created_at,updated_at) VALUES (1,'Test','Test','Test','APPROVED',now(),now(),2,NULL,now(),now()),(4,'Test','Test','Test','PENDING',now(),NULL,NULL,NULL,now(),now()),(5,'Test','Test','Test','REJECTED',now(),now(),2,'Test rejection',now(),now()),(6,'Test','Test','Test','APPROVED',now(),now(),2,NULL,now(),now())");
        sql("INSERT INTO course_categories(id,name,created_at,updated_at) VALUES (100,'Legacy',now(),now())");
        sql("INSERT INTO courses(id,teacher_id,category_id,name,description,tuition,currency,min_students,max_students,planned_start_date,timezone,session_count,status,created_at,updated_at) VALUES (100,1,100,'Legacy','Legacy',100,'VND',1,10,current_date,'Asia/Ho_Chi_Minh',1,'DRAFT',now(),now())");
        sql("INSERT INTO enrollments(id,student_id,course_id,status,created_at,updated_at) VALUES (100,3,100,'PENDING',now(),now())");
        sql("INSERT INTO teacher_payment_accounts(id,teacher_id,bank_code,account_number,account_holder_name,provider,status,created_at,updated_at) VALUES (100,1,'LEGACY DO NOT GUESS','000123','Legacy Holder','VIETQR','ACTIVE',now(),now())");
        sql("INSERT INTO payments(id,enrollment_id,teacher_payment_account_id,payment_code,provider,original_amount,discount_amount,final_amount,currency,status,expires_at,created_at,updated_at) VALUES (100,100,100,'LEGACY','VIETQR',100,0,100,'VND','PENDING',now()+interval '1 hour',now(),now())");
        var f=Flyway.configure().dataSource(url,user,password).cleanDisabled(true).load();assertEquals(1,f.migrate().migrationsExecuted);f.validate();
        assertEquals(7,count("SELECT count(*) FROM flyway_schema_history WHERE success"));
        var props=new HashMap<String,Object>();props.put("spring.datasource.url",url);props.put("spring.datasource.username",user);props.put("spring.datasource.password",password);
        props.put("spring.flyway.enabled",false);props.put("spring.jpa.hibernate.ddl-auto","validate");props.put("spring.sql.init.mode","never");
        props.put("spring.datasource.hikari.maximum-pool-size",4);props.put("spring.datasource.hikari.connection-init-sql","SET lock_timeout TO '5s'");
        byte[] key=new byte[32];new java.security.SecureRandom().nextBytes(key);props.put("app.security.password-reset.hmac-secret",Base64.getEncoder().encodeToString(key));
        context=new SpringApplicationBuilder(EnglishLearningApplication.class,StorageFixture.class).web(WebApplicationType.NONE).initializers(c->c.getEnvironment().getPropertySources().addFirst(new MapPropertySource("isolated-receiving",props))).run();
        service=context.getBean(ReceivingAccountService.class);
    }
    @AfterAll void close(){if(context!=null)context.close();}
    @BeforeEach void reset()throws Exception{sql("DELETE FROM teacher_payment_accounts WHERE id<>100");objects.clear();}
    ReceivingAccountService.Created submit(long actor,String key){return service.submit(actor,"970436","Teacher","0012345678",key,ReceivingImageTests.png());}
    @Test void migrationPreservesHistoricalDestinationWithoutApproval()throws Exception {
        assertEquals("LEGACY DO NOT GUESS|000123|Legacy Holder|UNREVIEWED|INACTIVE",text("SELECT bank_code||'|'||account_number||'|'||account_holder_name||'|'||review_status||'|'||status FROM teacher_payment_accounts WHERE id=100"));
        assertEquals(1,count("SELECT count(*) FROM teacher_payment_accounts WHERE id=100 AND bank_bin IS NULL AND qr_object IS NULL"));
        assertEquals(100,count("SELECT teacher_payment_account_id FROM payments WHERE id=100"));assertEquals("PENDING",text("SELECT status FROM payments WHERE id=100"));
    }
    @Test void submissionIdempotencyAndPrivateHistory()throws Exception {
        String key=UUID.randomUUID().toString();var first=submit(1,key);var retry=submit(1,key);
        assertTrue(first.created());assertFalse(retry.created());assertEquals(first.account(),retry.account());assertEquals(1,objects.size());
        assertEquals("PENDING",first.account().reviewStatus());assertEquals("INACTIVE",first.account().selectionStatus());
        assertEquals(ReceivingFailure.Reason.IDEMPOTENCY_CONFLICT,assertThrows(ReceivingFailure.class,()->service.submit(1,"970436","Other Name","0012345678",key,ReceivingImageTests.png())).reason());
        assertEquals(ReceivingFailure.Reason.CONFLICT,assertThrows(ReceivingFailure.class,()->submit(1,UUID.randomUUID().toString())).reason());
        assertEquals("****5678",service.list(1,0,1).items().get(0).accountNumber());assertTrue(service.list(1,0,1).hasNext());
        assertArrayEquals(QrImage.sanitize(ReceivingImageTests.png()),service.image(1,first.account().id().value(),false));
        assertEquals(1,count("SELECT count(*) FROM payments"));assertEquals("PENDING",text("SELECT status FROM enrollments WHERE id=100"));
    }
    @Test void persistedTeacherEligibilityAndOwnership() {
        for(long actor:new long[]{3,4,5})assertEquals(ReceivingFailure.Reason.DENIED,assertThrows(ReceivingFailure.class,()->submit(actor,UUID.randomUUID().toString())).reason());
        var first=submit(1,UUID.randomUUID().toString());assertThrows(ReceivingFailure.class,()->service.detail(6,first.account().id().value()));
        assertThrows(ReceivingFailure.class,()->service.image(6,first.account().id().value(),false));assertThrows(ReceivingFailure.class,()->service.image(3,first.account().id().value(),true));
        assertNotNull(service.image(2,first.account().id().value(),true));assertTrue(service.list(6,0,20).items().isEmpty());
    }
    @Test void concurrentSameAndDifferentKeysCannotCreateTwoPending()throws Exception {
        for(boolean same:new boolean[]{true,false}) {
            sql("DELETE FROM teacher_payment_accounts WHERE id<>100");objects.clear();String one=UUID.randomUUID().toString(),two=same?one:UUID.randomUUID().toString();
            var pool=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var go=new CountDownLatch(1);
            try {
                List<Future<Boolean>> jobs=new ArrayList<>();
                for(String key:List.of(one,two))jobs.add(pool.submit(()->{ready.countDown();if(!go.await(10,TimeUnit.SECONDS))throw new AssertionError();try{submit(1,key);return true;}catch(ReceivingFailure failure){assertEquals(ReceivingFailure.Reason.CONFLICT,failure.reason());return false;}}));
                assertTrue(ready.await(10,TimeUnit.SECONDS));go.countDown();int success=0;for(var job:jobs)if(job.get(20,TimeUnit.SECONDS))success++;
                assertEquals(same?2:1,success);assertEquals(1,count("SELECT count(*) FROM teacher_payment_accounts WHERE review_status='PENDING'"));assertEquals(1,objects.size());
            } finally {go.countDown();pool.shutdownNow();}
        }
    }
    @Test void confirmedPostgresInsertFailureReclaimsOnlyNewUpload()throws Exception {
        sql("CREATE FUNCTION task022_fail_insert() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'test rejection' USING ERRCODE='23514'; END; $$");
        try {
            sql("CREATE TRIGGER task022_fail_insert BEFORE INSERT ON teacher_payment_accounts FOR EACH ROW EXECUTE FUNCTION task022_fail_insert()");
            assertThrows(org.springframework.dao.DataAccessException.class,()->submit(1,UUID.randomUUID().toString()));
            assertTrue(objects.isEmpty());assertEquals(1,count("SELECT count(*) FROM teacher_payment_accounts"));
            assertEquals(100,count("SELECT teacher_payment_account_id FROM payments WHERE id=100"));
        } finally {
            sql("DROP TRIGGER IF EXISTS task022_fail_insert ON teacher_payment_accounts");
            sql("DROP FUNCTION task022_fail_insert()");
        }
    }
    @Test void databaseRejectsPendingSelectionAndDuplicatePending()throws Exception {
        var first=submit(1,UUID.randomUUID().toString());long id=first.account().id().value();
        assertThrows(SQLException.class,()->sql("UPDATE teacher_payment_accounts SET account_number='999' WHERE id="+id));
        assertThrows(SQLException.class,()->sql("UPDATE teacher_payment_accounts SET status='ACTIVE' WHERE id="+id));
        var failure=assertThrows(org.postgresql.util.PSQLException.class,()->sql("INSERT INTO teacher_payment_accounts(teacher_id,bank_code,bank_bin,bank_name,account_number,account_holder_name,provider,status,review_status,qr_object,qr_sha256,qr_size,submission_key,request_sha256,created_at,updated_at) SELECT teacher_id,bank_code,bank_bin,bank_name,account_number,account_holder_name,provider,status,review_status,'"+UUID.randomUUID()+".png',qr_sha256,qr_size,'"+UUID.randomUUID()+"',request_sha256,created_at,updated_at FROM teacher_payment_accounts WHERE id="+id));
        assertEquals("23505",failure.getSQLState());
        assertEquals("uq_receiving_pending",failure.getServerErrorMessage().getConstraint());
    }
}
