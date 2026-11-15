package com.englishlearning.features.googlecalendar;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.*;
import static org.mockito.Mockito.*;
import com.englishlearning.features.user.service.*;
import com.englishlearning.features.user.entity.User.Role;
import com.englishlearning.features.teacher.service.TeacherBusinessEligibility;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.transaction.PlatformTransactionManager;

@EnabledIfSystemProperty(named="googlePersistence",matches="true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GooglePersistenceTests {
    JdbcTemplate jdbc;GoogleStore store;long owner,other;
    @BeforeAll void setup(){
        String url=System.getenv("GOOGLE_TEST_URL"),user=System.getenv("GOOGLE_TEST_USER"),password=System.getenv("GOOGLE_TEST_PASSWORD");
        assertEquals("jdbc:postgresql://127.0.0.1:15443/task_c3_google",url);assertEquals("task_c3_test",user);
        var ds=new DriverManagerDataSource(url,user,password);jdbc=new JdbcTemplate(ds);
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM pg_tables WHERE schemaname NOT IN ('pg_catalog','information_schema')",Integer.class));
        var flyway=Flyway.configure().dataSource(ds).cleanDisabled(true).load();flyway.migrate();flyway.validate();
        assertEquals(12,jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success",Integer.class));
        var beans=new StaticListableBeanFactory();beans.addBean("jdbc",jdbc);beans.addBean("tx",new DataSourceTransactionManager(ds));
        store=new GoogleStore(beans.getBeanProvider(JdbcTemplate.class),beans.getBeanProvider(PlatformTransactionManager.class));
        owner=insert("owner@example.invalid");other=insert("other@example.invalid");
    }
    long insert(String email){return jdbc.queryForObject("INSERT INTO users(email,password_hash,full_name,role,email_verified,locked,created_at,updated_at) VALUES (?,'unused','Synthetic','STUDENT',true,false,now(),now()) RETURNING id",Long.class,email);}
    @BeforeEach void clear(){jdbc.update("DELETE FROM google_calendar_oauth_intents");jdbc.update("DELETE FROM google_calendar_connections");}
    long begin(long user,String state){return store.transaction(()->{store.lock(user);long version=store.advance(user);store.intent(state,user,"binding","encrypted-verifier","key","nonce",version);return version;});}
    @Test void intentIsSingleUseBoundToBrowserAndHasNoRecoverableVerifierAfterConsumption(){
        begin(owner,"state");assertThrows(GoogleFailure.class,()->store.transaction(()->store.consume("state","other-browser")));
        assertThrows(GoogleFailure.class,()->store.transaction(()->store.consume("unknown","binding")));
        var intent=store.transaction(()->store.consume("state","binding"));assertEquals(owner,intent.user());
        assertThrows(GoogleFailure.class,()->store.transaction(()->store.consume("state","binding")));
        assertNull(jdbc.queryForObject("SELECT verifier_ciphertext FROM google_calendar_oauth_intents WHERE user_id=?",String.class,owner));
    }
    @Test void expiredAndSupersededIntentsCannotBeConsumed(){
        begin(owner,"old");begin(owner,"new");assertThrows(GoogleFailure.class,()->store.transaction(()->store.consume("old","binding")));
        jdbc.update("UPDATE google_calendar_oauth_intents SET created_at=now()-interval '20 minutes',expires_at=now()-interval '1 minute'");
        assertThrows(GoogleFailure.class,()->store.transaction(()->store.consume("new","binding")));
    }
    @Test void concurrentCallbacksConsumeExactlyOnce()throws Exception{
        begin(owner,"state");var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        Callable<Boolean> call=()->{start.await();try{store.transaction(()->store.consume("state","binding"));return true;}catch(GoogleFailure rejected){return false;}};
        try{var a=pool.submit(call);var b=pool.submit(call);start.countDown();assertNotEquals(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS));}finally{pool.shutdownNow();}
    }
    @Test void optimisticVersionOwnershipAndConflictingGoogleSubjects(){
        long first=begin(owner,"owner"),second=begin(other,"other");
        var grant=new GoogleProvider.Grant("subject","synthetic@example.test","openid email "+GoogleSettings.SCOPE,"not-persisted-raw");
        assertTrue(store.save(owner,first,grant,"encrypted-refresh","key"));assertFalse(store.save(owner,first,grant,"stale","key"));
        assertEquals("DISCONNECTED",store.connection(other).status());
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->store.save(other,second,grant,"encrypted-refresh","key"));
        store.transaction(()->{store.lock(owner);store.disconnect(owner);return null;});
        assertNull(store.connection(owner).ciphertext());assertEquals("REVOCATION_PENDING",store.connection(owner).status());
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM google_calendar_oauth_intents WHERE user_id=?",Integer.class,owner));
        assertTrue(store.transaction(()->store.release(owner,first+2,"subject")));
        assertFalse(store.save(other,second,grant,"encrypted-refresh","key"));
        assertTrue(store.save(other,begin(other,"fresh"),grant,"encrypted-refresh","key"));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"student-lock","teacher-approval","eligible","competing-lock"})
    void finalizationUsesCurrentLockedRows(String scenario)throws Exception {
        boolean teacher=scenario.equals("teacher-approval");
        long actor=insert(scenario+"@example.invalid");
        if(teacher){
            jdbc.update("UPDATE users SET role='TEACHER', email_verified=false WHERE id=?",actor);
            long admin=insert("reviewer@example.invalid");
            jdbc.update("UPDATE users SET role='ADMIN' WHERE id=?",admin);
            jdbc.update("INSERT INTO teacher_applications(teacher_id,specialization,teaching_experience,introduction,status,submitted_at,reviewed_at,reviewed_by,created_at,updated_at) VALUES (?,'Fixture','Fixture','Fixture','APPROVED',now(),now(),?,now(),now())",actor,admin);
        }
        var settings=GoogleTestSettings.create(folder);
        var accounts=mock(CurrentAccountService.class);
        // Deliberately stale preflight snapshot throughout the request.
        when(accounts.findById(actor)).thenReturn(java.util.Optional.of(new AccountSnapshot(actor,teacher?Role.TEACHER:Role.STUDENT,false,!teacher,false,teacher)));
        var provider=mock(GoogleProvider.class);
        var entered=new CountDownLatch(1);var resume=new CountDownLatch(1);
        when(provider.authorize(anyString(),anyString(),anyString())).thenAnswer(call->call.getArgument(0));
        when(provider.exchange(anyString(),anyString(),anyString())).thenAnswer(call->{
            entered.countDown();assertTrue(resume.await(10,TimeUnit.SECONDS));
            return new GoogleProvider.Grant("fresh-subject","synthetic@example.test","scope","synthetic-refresh");
        });
        var audited=spy(store);
        var atSave=new CountDownLatch(1);var allowSave=new CountDownLatch(1);
        if(scenario.equals("competing-lock")){
            doAnswer(call->{atSave.countDown();assertTrue(allowSave.await(10,TimeUnit.SECONDS));return call.callRealMethod();})
                .when(audited).save(eq(actor),anyLong(),any(),anyString(),anyString());
        }
        var service=new GoogleService(settings,new GoogleCrypto(settings),provider,audited,accounts,mock(TeacherBusinessEligibility.class));
        var started=service.start(actor);
        var pool=Executors.newSingleThreadExecutor();
        try{
            var callback=pool.submit(()->service.complete(started.authorizationUrl(),started.browserBinding(),"code",null));
            assertTrue(entered.await(10,TimeUnit.SECONDS));
            if(scenario.equals("student-lock"))jdbc.update("UPDATE users SET locked=true WHERE id=?",actor);
            if(teacher)jdbc.update("UPDATE teacher_applications SET status='REJECTED',rejection_reason='Fixture' WHERE teacher_id=?",actor);
            resume.countDown();
            if(teacher||scenario.equals("student-lock")){
                var failure=assertThrows(ExecutionException.class,()->callback.get(10,TimeUnit.SECONDS));
                assertEquals("DENIED",assertInstanceOf(GoogleFailure.class,failure.getCause()).code);
                assertEquals("DISCONNECTED",store.connection(actor).status());assertNull(store.connection(actor).ciphertext());
            }else{
                if(scenario.equals("competing-lock")){
                    assertTrue(atSave.await(10,TimeUnit.SECONDS));
                    assertThrows(org.springframework.dao.CannotAcquireLockException.class,()->jdbc.queryForObject("SELECT id FROM users WHERE id=? FOR UPDATE NOWAIT",Long.class,actor));
                    allowSave.countDown();
                }
                callback.get(10,TimeUnit.SECONDS);assertEquals("CONNECTED",store.connection(actor).status());
                if(scenario.equals("competing-lock"))assertEquals(1,jdbc.update("UPDATE users SET locked=true WHERE id=?",actor));
            }
        }finally{resume.countDown();allowSave.countDown();pool.shutdownNow();}
    }
    @org.junit.jupiter.api.io.TempDir java.nio.file.Path folder;
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans={true,false})
    void subjectHandoffWaitsForOneDefinitiveRevocation(boolean acknowledged)throws Exception {
        var settings=GoogleTestSettings.create(folder);
        var crypto=new GoogleCrypto(settings);
        var provider=mock(GoogleProvider.class);
        var accounts=mock(CurrentAccountService.class);
        for(long id:new long[]{owner,other})when(accounts.findById(id)).thenReturn(java.util.Optional.of(new AccountSnapshot(id,Role.STUDENT,false,true,true,false)));
        when(provider.authorize(anyString(),anyString(),anyString())).thenAnswer(call->call.getArgument(0));
        when(provider.exchange(anyString(),anyString(),anyString())).thenAnswer(call->new GoogleProvider.Grant("subject","synthetic@example.test","scope","token-"+call.getArgument(0)));
        var service=new GoogleService(settings,crypto,provider,store,accounts,mock(TeacherBusinessEligibility.class));
        var restarted=new GoogleService(settings,crypto,provider,store,accounts,mock(TeacherBusinessEligibility.class));
        var initial=service.start(owner);
        service.complete(initial.authorizationUrl(),initial.browserBinding(),"A",null);
        var stale=service.start(owner);
        var entered=new CountDownLatch(1);var finish=new CountDownLatch(1);
        when(provider.revoke("token-A")).thenAnswer(call->{entered.countDown();assertTrue(finish.await(10,TimeUnit.SECONDS));return acknowledged;});
        var pool=Executors.newSingleThreadExecutor();
        try {
            var result=pool.submit(()->service.disconnect(owner));assertTrue(entered.await(10,TimeUnit.SECONDS));
            assertNull(store.connection(owner).ciphertext());
            assertEquals("REVOCATION_PENDING",restarted.status(owner).status());
            assertThrows(GoogleFailure.class,()->restarted.accessToken(owner));
            assertThrows(GoogleFailure.class,()->restarted.start(owner));
            assertFalse(restarted.disconnect(owner));
            assertThrows(GoogleFailure.class,()->restarted.complete(stale.authorizationUrl(),stale.browserBinding(),"old",null));
            var blocked=restarted.start(other);
            assertThrows(GoogleFailure.class,()->restarted.complete(blocked.authorizationUrl(),blocked.browserBinding(),"B",null));
            var earlier=restarted.start(other);
            finish.countDown();assertEquals(acknowledged,result.get(10,TimeUnit.SECONDS));
            assertThrows(GoogleFailure.class,()->restarted.complete(earlier.authorizationUrl(),earlier.browserBinding(),"B",null));
            var fresh=restarted.start(other);
            if(acknowledged){
                restarted.complete(fresh.authorizationUrl(),fresh.browserBinding(),"B",null);
                assertEquals("CONNECTED",restarted.status(other).status());
                assertNull(restarted.status(owner).email());
                assertTrue(restarted.disconnect(owner));
                assertEquals("token-B",crypto.decrypt(store.connection(other).ciphertext(),"test-1","refresh:"+other+":subject"));
            }else{
                assertThrows(GoogleFailure.class,()->restarted.complete(fresh.authorizationUrl(),fresh.browserBinding(),"B",null));
                assertFalse(restarted.disconnect(owner));assertEquals("REVOCATION_PENDING",restarted.status(owner).status());
            }
            verify(provider,times(1)).revoke(anyString());
        }finally{finish.countDown();pool.shutdownNow();}
    }
}
