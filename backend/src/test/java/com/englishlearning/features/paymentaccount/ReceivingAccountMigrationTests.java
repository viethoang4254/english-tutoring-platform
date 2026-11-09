package com.englishlearning.features.paymentaccount;

import java.sql.*;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.postgresql.util.PSQLException;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="receivingPersistence",matches="true")
class ReceivingAccountMigrationTests {
    final String user=System.getenv("RECEIVING_TEST_USER"),password=System.getenv("RECEIVING_TEST_PASSWORD");
    String target(String database)throws Exception {
        assertEquals("jdbc:postgresql://127.0.0.1:55441/task022_receiving",System.getenv("RECEIVING_TEST_URL"));
        assertEquals("task022_test",user);
        assertTrue(Set.of("task022_upgrade_pending","task022_upgrade_approved","task022_upgrade_rejected").contains(database));
        String url="jdbc:postgresql://127.0.0.1:55441/"+database;
        assertEquals("0",text(url,"SELECT count(*) FROM pg_tables WHERE schemaname NOT IN ('pg_catalog','information_schema')"));
        return url;
    }
    void sql(String url,String statement)throws Exception {
        try(var c=DriverManager.getConnection(url,user,password);var s=c.createStatement()){s.execute(statement);}
    }
    String text(String url,String query)throws Exception {
        try(var c=DriverManager.getConnection(url,user,password);var s=c.createStatement();var r=s.executeQuery(query)){assertTrue(r.next());return r.getString(1);}
    }
    void seedV7(String url,String state)throws Exception {
        assertEquals(7,Flyway.configure().dataSource(url,user,password).target("7").cleanDisabled(true).load().migrate().migrationsExecuted);
        sql(url,"INSERT INTO users(id,email,password_hash,full_name,role,email_verified,locked,created_at,updated_at) VALUES (1,'teacher@example.invalid','unused','Teacher','TEACHER',true,false,now(),now()),(2,'student@example.invalid','unused','Student','STUDENT',true,false,now(),now())");
        sql(url,"INSERT INTO course_categories(id,name,created_at,updated_at) VALUES (100,'Upgrade',now(),now())");
        sql(url,"INSERT INTO courses(id,teacher_id,category_id,name,description,tuition,currency,min_students,max_students,planned_start_date,timezone,session_count,status,created_at,updated_at) VALUES (100,1,100,'Upgrade','Upgrade',100,'VND',1,10,current_date,'Asia/Ho_Chi_Minh',1,'DRAFT',now(),now())");
        sql(url,"INSERT INTO enrollments(id,student_id,course_id,status,created_at,updated_at) VALUES (100,2,100,'PENDING',now(),now())");
        // V7 permits these review states but has no reviewer/time columns. Do not fabricate audit values.
        String selection=state.equals("APPROVED")?"ACTIVE":"INACTIVE";
        sql(url,"INSERT INTO teacher_payment_accounts(id,teacher_id,bank_code,bank_bin,bank_name,account_number,account_holder_name,provider,status,review_status,qr_object,qr_sha256,qr_size,submission_key,request_sha256,created_at,updated_at) VALUES (100,1,'VCB','970436','Bank','000123','Original Holder','VIETQR','"+selection+"','"+state+"','12345678-1234-1234-1234-123456789abc.png',repeat('a',64),100,'12345678-1234-1234-1234-123456789abc',repeat('b',64),now(),now())");
        sql(url,"INSERT INTO payments(id,enrollment_id,teacher_payment_account_id,payment_code,provider,original_amount,discount_amount,final_amount,currency,status,expires_at,created_at,updated_at) VALUES (100,100,100,'UPGRADE-HISTORY','VIETQR',100,0,100,'VND','EXPIRED',now()-interval '1 hour',now()-interval '2 hours',now())");
    }
    String account(String url)throws Exception {
        return text(url,"SELECT (to_jsonb(a)-'reviewed_by'-'reviewed_at'-'rejection_reason')::text FROM teacher_payment_accounts a WHERE id=100");
    }
    String payment(String url)throws Exception {return text(url,"SELECT to_jsonb(p)::text FROM payments p WHERE id=100");}
    @Test void pendingV7SubmissionUpgradesWithoutChangingBankEvidenceOrPaymentHistory()throws Exception {
        String url=target("task022_upgrade_pending");seedV7(url,"PENDING");String account=account(url),payment=payment(url);
        var flyway=Flyway.configure().dataSource(url,user,password).target("8").cleanDisabled(true).load();
        assertEquals(1,flyway.migrate().migrationsExecuted);flyway.validate();
        assertEquals("8",text(url,"SELECT count(*) FROM flyway_schema_history WHERE success"));
        assertEquals(account,account(url));assertEquals(payment,payment(url));
        assertEquals("1",text(url,"SELECT count(*) FROM teacher_payment_accounts WHERE id=100 AND review_status='PENDING' AND status='INACTIVE' AND reviewed_by IS NULL AND reviewed_at IS NULL AND rejection_reason IS NULL"));
        assertEquals("100",text(url,"SELECT teacher_payment_account_id FROM payments WHERE id=100"));
    }
    @Test void approvedV7RowWithoutAuditFailsClosedAndPreservesHistory()throws Exception { incompatible("task022_upgrade_approved","APPROVED"); }
    @Test void rejectedV7RowWithoutAuditFailsClosedAndPreservesHistory()throws Exception { incompatible("task022_upgrade_rejected","REJECTED"); }
    void incompatible(String database,String state)throws Exception {
        String url=target(database);seedV7(url,state);String account=account(url),payment=payment(url);
        var failure=assertThrows(FlywayException.class,()->Flyway.configure().dataSource(url,user,password).target("8").cleanDisabled(true).load().migrate());
        PSQLException postgres=null;
        for(Throwable cause=failure;cause!=null;cause=cause.getCause())if(cause instanceof PSQLException p){postgres=p;break;}
        assertNotNull(postgres);assertEquals("23514",postgres.getSQLState());assertEquals("ck_receiving_audit",postgres.getServerErrorMessage().getConstraint());
        assertEquals(account,account(url));assertEquals(payment,payment(url));
        assertEquals("1,2,3,4,5,6,7",text(url,"SELECT string_agg(version,',' ORDER BY installed_rank) FROM flyway_schema_history WHERE success"));
        assertEquals("7",text(url,"SELECT count(*) FROM flyway_schema_history"));
        assertEquals("0",text(url,"SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND table_name='teacher_payment_accounts' AND column_name IN ('reviewed_by','reviewed_at','rejection_reason')"));
        assertEquals("100",text(url,"SELECT teacher_payment_account_id FROM payments WHERE id=100"));
        Flyway.configure().dataSource(url,user,password).target("7").cleanDisabled(true).load().validate();
    }
}
