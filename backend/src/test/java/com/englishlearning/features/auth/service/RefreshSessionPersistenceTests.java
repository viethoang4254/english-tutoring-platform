package com.englishlearning.features.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.englishlearning.EnglishLearningApplication;
import com.englishlearning.features.auth.repository.RefreshSessionRepository;
import com.englishlearning.features.auth.security.RefreshTokens;
import com.englishlearning.features.auth.security.AccessTokenIssuer;
import com.englishlearning.features.auth.security.AccessTokenConfiguration;
import com.englishlearning.features.auth.security.PasswordConfiguration;
import com.englishlearning.features.user.service.UserCredentialService;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.nio.file.*;
import java.security.KeyPairGenerator;
import com.englishlearning.features.user.service.CurrentAccountService;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.transaction.PlatformTransactionManager;

@EnabledIfSystemProperty(named = "refreshPersistence", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RefreshSessionPersistenceTests {
    @org.junit.jupiter.api.io.TempDir static Path keyDirectory;
    AccessTokenIssuer issuer;
    PasswordEncoder passwords;
    UserCredentialService credentials;
    String passwordHash;
    String url;
    String username;
    String password;
    ConfigurableApplicationContext context;
    RefreshSessionRepository sessions;
    CurrentAccountService accounts;
    PlatformTransactionManager manager;
    final RefreshTokens tokens = new RefreshTokens();
    final AtomicReference<Instant> time = new AtomicReference<>();
    final Clock clock = new Clock() {
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return time.get(); }
    };

    @BeforeAll void startOnlyOnEmptyIsolatedDatabase() throws Exception {
        url = System.getenv("TASK007_TEST_URL");
        username = System.getenv("TASK007_TEST_USER");
        password = System.getenv("TASK007_TEST_PASSWORD");
        assertTrue("jdbc:postgresql://127.0.0.1:55432/task007_refresh".equals(url)
                || "jdbc:postgresql://127.0.0.1:55434/task007_profile_acceptance".equals(url));
        assertEquals("task007_test", username);
        assertNotNull(password);
        try (var c = connection(); var st = c.createStatement();
                var rows = st.executeQuery("SELECT count(*) FROM pg_tables WHERE schemaname NOT IN ('pg_catalog','information_schema')")) {
            assertTrue(rows.next());
            assertEquals(0, rows.getInt(1), "Fresh disposable database required; never clean/reuse");
        }
        var properties = Map.<String, Object>of(
                "spring.datasource.url", url, "spring.datasource.username", username,
                "spring.datasource.password", password, "spring.flyway.enabled", true,
                "spring.flyway.clean-disabled", true, "spring.jpa.hibernate.ddl-auto", "validate",
                "spring.sql.init.mode", "never", "spring.jpa.open-in-view", false,
                "spring.datasource.hikari.connection-init-sql", "SET lock_timeout TO '5s'");
        context = new SpringApplicationBuilder(EnglishLearningApplication.class).web(WebApplicationType.NONE)
                .initializers(c -> c.getEnvironment().getPropertySources()
                        .addFirst(new MapPropertySource("isolated-refresh-test", properties))).run();
        sessions = context.getBean(RefreshSessionRepository.class);
        accounts = context.getBean(CurrentAccountService.class);
        manager = context.getBean(PlatformTransactionManager.class);
        credentials = context.getBean(UserCredentialService.class);
        passwords = new PasswordConfiguration().passwordEncoder();
        passwordHash = passwords.encode(" test password ");
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var pair = generator.generateKeyPair();
        Path privateKey = keyDirectory.resolve("private.pem");
        Path publicKey = keyDirectory.resolve("public.pem");
        Files.writeString(privateKey, "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder().encodeToString(pair.getPrivate().getEncoded()) + "\n-----END PRIVATE KEY-----\n");
        Files.writeString(publicKey, "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder().encodeToString(pair.getPublic().getEncoded()) + "\n-----END PUBLIC KEY-----\n");
        issuer = new AccessTokenConfiguration().accessTokenIssuer(privateKey.toUri().toString(),
                publicKey.toUri().toString(), "english-tutoring-platform", "english-tutoring-api", Duration.ofMinutes(15), clock);
        try (var c = connection(); var insert = c.prepareStatement(
                "INSERT INTO public.users(id,email,password_hash,full_name,role,email_verified,locked,created_at,updated_at)"
                + " VALUES (?,?,?,'Workflow test',?,true,false,now(),now())")) {
            for (int id = 10; id <= 12; id++) {
                insert.setLong(1, id); insert.setString(2, "workflow" + id + "@example.invalid");
                insert.setString(3, passwordHash);
                insert.setString(4, new String[]{"STUDENT","TEACHER","ADMIN"}[id-10]);
                insert.executeUpdate();
            }
        }

        execute("""
                INSERT INTO public.users(id,email,password_hash,full_name,role,email_verified,locked,created_at,updated_at)
                VALUES (1,'one@example.invalid','test-only','Test One','STUDENT',false,false,now(),now()),
                       (2,'two@example.invalid','test-only','Test Two','TEACHER',false,true,now(),now()),
                       (3,'three@example.invalid','test-only','Test Three','TEACHER',false,false,now(),now())
                """);
        execute("SELECT setval(pg_get_serial_sequence('public.users','id'), (SELECT max(id) FROM public.users))");
    }
    @AfterAll void close() { if (context != null) context.close(); }
    @BeforeEach void resetClock() { time.set(Instant.parse("2026-10-11T00:00:00Z")); }
    Connection connection() throws SQLException { return DriverManager.getConnection(url, username, password); }
    RefreshSessionService service() { return service(tokens, Duration.ofDays(7)); }
    RefreshSessionService service(RefreshTokens source, Duration lifetime) {
        return new RefreshSessionService(sessions, accounts, source, clock, lifetime, manager);
    }
    void execute(String sql) throws SQLException {
        try (var c = connection(); var st = c.createStatement()) { st.execute(sql); }
    }
    long count(String condition) throws SQLException {
        try (var c = connection(); var st = c.createStatement();
                var rows = st.executeQuery("SELECT count(*) FROM public.refresh_sessions WHERE " + condition)) {
            rows.next(); return rows.getLong(1);
        }
    }
    void invalid(Runnable operation) {
        var failure = assertThrows(RefreshFailure.class, operation::run);
        assertEquals(RefreshFailure.Reason.INVALID_CREDENTIAL, failure.reason());
        assertEquals("Invalid refresh credential.", failure.getMessage());
        assertNull(failure.getCause());
    }

    @Test void rotatesAndPreservesHistoryWithSlidingExpiryAndHashOnlyStorage() throws Exception {
        var service = service();
        var original = service.create(1L);
        time.set(time.get().plus(Duration.ofDays(2)));
        var replacement = service.rotate(original.credential());
        assertNotEquals(original.sessionId(), replacement.sessionId());
        assertEquals(time.get().plus(Duration.ofDays(7)), replacement.expiresAt());
        assertEquals(1, count("id=" + original.sessionId() + " AND revoked_at IS NOT NULL"));
        assertEquals(1, count("id=" + replacement.sessionId() + " AND revoked_at IS NULL"));
        try (var c = connection(); var st = c.prepareStatement("SELECT token_hash FROM public.refresh_sessions WHERE id=?")) {
            st.setLong(1, replacement.sessionId());
            try (var rows = st.executeQuery()) {
                assertTrue(rows.next());
                assertEquals(tokens.hash(replacement.credential()), rows.getString(1));
                assertNotEquals(replacement.credential(), rows.getString(1));
            }
        }
        invalid(() -> service.rotate(original.credential()));
        assertEquals(1, count("id=" + replacement.sessionId() + " AND revoked_at IS NULL"));
    }

    @Test void concurrentRotationHasExactlyOneCommittedWinner() throws Exception {
        var service = service();
        var original = service.create(1L);
        long before = count("true");
        var pool = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var go = new CountDownLatch(1);
        Callable<Object> operation = () -> {
            ready.countDown();
            assertTrue(go.await(5, TimeUnit.SECONDS));
            try { return service.rotate(original.credential()); }
            catch (RefreshFailure failure) { return failure.reason(); }
        };
        try {
            var first = pool.submit(operation); var second = pool.submit(operation);
            assertTrue(ready.await(5, TimeUnit.SECONDS)); go.countDown();
            var results = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertEquals(1, results.stream().filter(RefreshCredential.class::isInstance).count());
            assertEquals(1, results.stream().filter(r -> r == RefreshFailure.Reason.INVALID_CREDENTIAL).count());
            assertEquals(before + 1, count("true"));
        } finally { pool.shutdownNow(); }
    }

    @Test void uniqueCollisionRollsBackRevocation() throws Exception {
        var service = service();
        var original = service.create(1L);
        var other = service.create(1L);
        var source = spy(new RefreshTokens());
        doReturn(other.credential()).when(source).generate();
        long before = count("true");
        var failure = assertThrows(RefreshFailure.class, () -> service(source, Duration.ofDays(7)).rotate(original.credential()));
        assertEquals(RefreshFailure.Reason.INTERNAL_FAILURE, failure.reason());
        assertEquals(before, count("true"));
        assertEquals(1, count("id=" + original.sessionId() + " AND revoked_at IS NULL"));
        assertEquals(1, count("id=" + other.sessionId() + " AND revoked_at IS NULL"));
        assertNotNull(service.rotate(original.credential()));
    }

    @Test void notNullInsertFailureAlsoRollsBack() throws Exception {
        var original = service().create(1L);
        var source = spy(new RefreshTokens());
        String replacement = tokens.generate();
        doReturn(replacement).when(source).generate();
        doReturn(null).when(source).hash(replacement);
        long before = count("true");
        var failure = assertThrows(RefreshFailure.class, () -> service(source, Duration.ofDays(7)).rotate(original.credential()));
        assertEquals(RefreshFailure.Reason.INTERNAL_FAILURE, failure.reason());
        assertEquals(before, count("true"));
        assertEquals(1, count("id=" + original.sessionId() + " AND revoked_at IS NULL"));
        assertNotNull(service().rotate(original.credential()));
    }

    @Test void rejectsExpiryBoundaryUnknownRevokedAndReplayUniformly() {
        var service = service();
        var original = service.create(1L);
        time.set(original.expiresAt());
        invalid(() -> service.rotate(original.credential()));
        time.set(original.expiresAt().plusSeconds(1));
        invalid(() -> service.rotate(original.credential()));
        invalid(() -> service.rotate(tokens.generate()));
        invalid(() -> service.rotate("malformed"));
        time.set(Instant.parse("2026-10-11T00:00:00Z"));
        var revoked = service.create(1L);
        service.revoke(revoked.credential());
        invalid(() -> service.rotate(revoked.credential()));
        invalid(() -> service.revoke(revoked.credential()));
    }

    @Test void independentSessionsAndTargetedRevocation() throws Exception {
        var service = service();
        var first = service.create(1L); var second = service.create(1L);
        service.revoke(first.credential());
        assertEquals(1, count("id=" + second.sessionId() + " AND revoked_at IS NULL"));
        assertNotNull(service.rotate(second.credential()));
        invalid(() -> service.rotate(first.credential()));
        var teacher = service.create(3L);
        assertNotNull(service.rotate(teacher.credential())); // Primitive does not decide onboarding eligibility.
    }

    @Test void rejectsMissingOrLockedAccountsWithoutChanges() throws Exception {
        var service = service();
        invalid(() -> service.create(99999L));
        invalid(() -> service.create(2L));
        var original = service.create(1L);
        execute("UPDATE public.users SET locked=true WHERE id=1");
        try {
            invalid(() -> service.rotate(original.credential()));
            invalid(() -> service.revoke(original.credential()));
            assertEquals(1, count("id=" + original.sessionId() + " AND revoked_at IS NULL"));
        } finally { execute("UPDATE public.users SET locked=false WHERE id=1"); }
    }

    @Test void checksClockAfterAcquiringContendedLock() throws Exception {
        var service = service();
        var original = service.create(1L);
        var pool = Executors.newSingleThreadExecutor();
        try (var holder = connection(); var st = holder.createStatement()) {
            holder.setAutoCommit(false);
            st.executeQuery("SELECT id FROM public.refresh_sessions WHERE id=" + original.sessionId() + " FOR UPDATE").close();
            var attempt = pool.submit(() -> {
                try { service.rotate(original.credential()); return null; }
                catch (RefreshFailure failure) { return failure.reason(); }
            });
            boolean waiting = false;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            while (System.nanoTime() < deadline) {
                try (var c = connection(); var probe = c.createStatement();
                     var rows = probe.executeQuery("SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock'")) {
                    rows.next(); waiting = rows.getInt(1) > 0;
                }
                if (waiting) break;
                Thread.sleep(20);
            }
            assertTrue(waiting, "Rotation must actually wait for the row lock");
            time.set(original.expiresAt());
            holder.commit();
            assertEquals(RefreshFailure.Reason.INVALID_CREDENTIAL, attempt.get(5, TimeUnit.SECONDS));
            assertEquals(1, count("id=" + original.sessionId() + " AND revoked_at IS NULL"));
        } finally { pool.shutdownNow(); }
    }

    @Test void supportsConfiguredLifetimeAndImmediateCommittedVisibility() throws Exception {
        var result = service(tokens, Duration.ofHours(2)).create(1L);
        assertEquals(time.get().plus(Duration.ofHours(2)), result.expiresAt());
        assertEquals(1, count("id=" + result.sessionId()), "A separate connection sees the row before caller proceeds");
    }
    AuthenticationWorkflowService workflow() { return workflow(tokens, issuer); }
    AuthenticationWorkflowService workflow(RefreshTokens source, AccessTokenIssuer signer) {
        return new AuthenticationWorkflowService(credentials, service(source, Duration.ofDays(7)), passwords, signer);
    }
    void authenticationInvalid(Runnable operation) {
        assertEquals(AuthenticationFailure.Reason.INVALID_CREDENTIAL,
                assertThrows(AuthenticationFailure.class, operation::run).reason());
    }

    @Test void workflowLoginUsesCanonicalEmailAndAllRolesWithoutTeacherApproval() throws Exception {
        for (long id = 10; id <= 12; id++) {
            var result = workflow().login("  WORKFLOW" + id + "@EXAMPLE.INVALID  ", " test password ");
            assertEquals(id, result.refreshCredential().userId());
            assertEquals(1, count("id=" + result.refreshCredential().sessionId()));
            assertEquals(Long.toString(id), com.nimbusds.jwt.SignedJWT.parse(
                    result.accessToken().value()).getJWTClaimsSet().getSubject());
        }
        // The Teacher has no profile or application approval. Neither is login authority.
        execute("INSERT INTO public.teacher_applications(teacher_id,specialization,teaching_experience,introduction,status,submitted_at,created_at,updated_at)"
                + " VALUES (11,'Test','Test','Test','PENDING',now(),now(),now())");
        assertNotNull(workflow().login("workflow11@example.invalid", " test password "));
        execute("UPDATE public.teacher_applications SET status='REJECTED',reviewed_at=now(),reviewed_by=12,rejection_reason='Test' WHERE teacher_id=11");
        assertNotNull(workflow().login("workflow11@example.invalid", " test password "));
    }
    @Test void workflowPasswordAndEligibilityFailuresCreateNoSessions() throws Exception {
        var workflow = workflow();
        long before = count("true");
        authenticationInvalid(() -> workflow.login("unknown@example.invalid", " test password "));
        authenticationInvalid(() -> workflow.login("workflow10@example.invalid", "wrong password"));
        authenticationInvalid(() -> workflow.login("workflow10@example.invalid", "test password"));
        for (String hash : new String[]{"malformed", "{noop} test password "}) {
            try (var c=connection(); var st=c.prepareStatement("UPDATE public.users SET password_hash=? WHERE id=10")) {
                st.setString(1, hash); st.executeUpdate();
            }
            authenticationInvalid(() -> workflow.login("workflow10@example.invalid", " test password "));
        }
        try (var c=connection(); var st=c.prepareStatement("UPDATE public.users SET password_hash=? WHERE id=10")) {
            st.setString(1, passwordHash); st.executeUpdate();
        }
        execute("UPDATE public.users SET locked=true WHERE id=10");
        try { authenticationInvalid(() -> workflow.login("workflow10@example.invalid", " test password ")); }
        finally { execute("UPDATE public.users SET locked=false WHERE id=10"); }
        execute("UPDATE public.users SET email_verified=false WHERE id=10");
        try { assertEquals(AuthenticationFailure.Reason.EMAIL_VERIFICATION_REQUIRED,
                assertThrows(AuthenticationFailure.class, () -> workflow.login("workflow10@example.invalid", " test password ")).reason()); }
        finally { execute("UPDATE public.users SET email_verified=true WHERE id=10"); }
        assertEquals(before, count("true"));
    }
    @Test void workflowRefreshCommitsPairPreservesHistoryAndIndependentSessions() throws Exception {
        var workflow = workflow();
        var first = workflow.login("workflow10@example.invalid", " test password ");
        var other = workflow.login("workflow10@example.invalid", " test password ");
        var rotated = workflow.refresh(first.refreshCredential().credential());
        assertEquals(1, count("id=" + first.refreshCredential().sessionId() + " AND revoked_at IS NOT NULL"));
        assertEquals(1, count("id=" + rotated.refreshCredential().sessionId() + " AND revoked_at IS NULL"));
        assertEquals(1, count("id=" + other.refreshCredential().sessionId() + " AND revoked_at IS NULL"));
        authenticationInvalid(() -> workflow.refresh(first.refreshCredential().credential()));
        assertEquals("AuthenticationResult[REDACTED]", rotated.toString());
    }
    @Test void workflowSigningFailurePreservesOriginalAndCreatesNoLoginSession() throws Exception {
        var original = workflow().login("workflow10@example.invalid", " test password ");
        var broken = mock(AccessTokenIssuer.class);
        when(broken.issue(any())).thenThrow(new IllegalStateException("PRIVATE signing detail"));
        var workflow = workflow(tokens, broken);
        long before = count("true");
        for (Runnable operation : java.util.List.<Runnable>of(
                () -> workflow.login("workflow10@example.invalid", " test password "),
                () -> workflow.refresh(original.refreshCredential().credential()))) {
            var failure = assertThrows(AuthenticationFailure.class, operation::run);
            assertEquals(AuthenticationFailure.Reason.INTERNAL_FAILURE, failure.reason());
            assertNull(failure.getCause());
        }
        assertEquals(before, count("true"));
        assertEquals(1, count("id=" + original.refreshCredential().sessionId() + " AND revoked_at IS NULL"));
        assertNotNull(workflow().refresh(original.refreshCredential().credential()));
    }
    @Test void workflowCollisionRollsBackAfterSigning() throws Exception {
        var first = workflow().login("workflow10@example.invalid", " test password ");
        var other = workflow().login("workflow10@example.invalid", " test password ");
        var source = spy(new RefreshTokens());
        doReturn(other.refreshCredential().credential()).when(source).generate();
        var workflow = workflow(source, issuer);
        long before = count("true");
        assertEquals(AuthenticationFailure.Reason.INTERNAL_FAILURE, assertThrows(AuthenticationFailure.class,
                () -> workflow.refresh(first.refreshCredential().credential())).reason());
        assertEquals(before, count("true"));
        assertEquals(1, count("id=" + first.refreshCredential().sessionId() + " AND revoked_at IS NULL"));
        assertEquals(1, count("id=" + other.refreshCredential().sessionId() + " AND revoked_at IS NULL"));
        assertNotNull(workflow().refresh(first.refreshCredential().credential()));
    }
    @Test void workflowConcurrentRotationHasOneWinner() throws Exception {
        var workflow = workflow();
        var first = workflow.login("workflow10@example.invalid", " test password ");
        long before = count("true");
        var pool = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2); var go = new CountDownLatch(1);
        Callable<Object> rotate = () -> {
            ready.countDown(); assertTrue(go.await(5, TimeUnit.SECONDS));
            try { return workflow.refresh(first.refreshCredential().credential()); }
            catch (AuthenticationFailure failure) { return failure.reason(); }
        };
        try {
            var a=pool.submit(rotate); var b=pool.submit(rotate);
            assertTrue(ready.await(5, TimeUnit.SECONDS)); go.countDown();
            var results=List.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
            assertEquals(1, results.stream().filter(AuthenticationResult.class::isInstance).count());
            assertEquals(1, results.stream().filter(x -> x==AuthenticationFailure.Reason.INVALID_CREDENTIAL).count());
            assertEquals(before+1,count("true"));
        } finally { pool.shutdownNow(); }
    }
    @Test void workflowRefreshRejectsExpiredUnknownLockedAndUnverified() throws Exception {
        var workflow = workflow();
        var original=workflow.login("workflow10@example.invalid", " test password ");
        time.set(original.refreshCredential().expiresAt());
        authenticationInvalid(() -> workflow.refresh(original.refreshCredential().credential()));
        time.set(Instant.parse("2026-10-11T00:00:00Z"));
        authenticationInvalid(() -> workflow.refresh(tokens.generate()));
        for (String change : new String[]{"locked=true","email_verified=false"}) {
            execute("UPDATE public.users SET " + change + " WHERE id=10");
            try { authenticationInvalid(() -> workflow.refresh(original.refreshCredential().credential())); }
            finally { execute("UPDATE public.users SET locked=false,email_verified=true WHERE id=10"); }
        }
        assertEquals(1,count("id=" + original.refreshCredential().sessionId() + " AND revoked_at IS NULL"));
    }
    @Test void workflowRechecksAccountAfterContendedUserLock() throws Exception {
        var workflow=workflow();
        var original=workflow.login("workflow10@example.invalid", " test password ");
        var pool=Executors.newSingleThreadExecutor();
        try (var holder=connection(); var st=holder.createStatement()) {
            holder.setAutoCommit(false);
            st.executeUpdate("UPDATE public.users SET email_verified=false WHERE id=10");
            var result=pool.submit(() -> {
                try { workflow.refresh(original.refreshCredential().credential()); return null; }
                catch (AuthenticationFailure failure) { return failure.reason(); }
            });
            boolean waiting=false;
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
            while (System.nanoTime()<deadline) {
                try (var c=connection(); var probe=c.createStatement();
                        var rows=probe.executeQuery("SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock'")) {
                    rows.next(); waiting=rows.getInt(1)>0;
                }
                if (waiting) break;
                Thread.sleep(20);
            }
            assertTrue(waiting);
            holder.commit();
            assertEquals(AuthenticationFailure.Reason.INVALID_CREDENTIAL,result.get(5,TimeUnit.SECONDS));
            assertEquals(1,count("id="+original.refreshCredential().sessionId()+" AND revoked_at IS NULL"));
        } finally {
            pool.shutdownNow();
            execute("UPDATE public.users SET email_verified=true WHERE id=10");
        }
    }

    @Test void workflowLogoutLockedUserIsTargetedIdempotentAndDoesNotRevokeAccessJwt() throws Exception {
        var workflow=workflow();
        var first=workflow.login("workflow10@example.invalid"," test password ");
        var other=workflow.login("workflow10@example.invalid"," test password ");
        String jwt=first.accessToken().value();
        execute("UPDATE public.users SET locked=true WHERE id=10");
        try {
            workflow.logout(first.refreshCredential().credential());
            workflow.logout(first.refreshCredential().credential());
            assertEquals(1,count("id="+first.refreshCredential().sessionId()+" AND revoked_at IS NOT NULL"));
            assertEquals(1,count("id="+other.refreshCredential().sessionId()+" AND revoked_at IS NULL"));
        } finally { execute("UPDATE public.users SET locked=false WHERE id=10"); }
        workflow.logout(null); workflow.logout("malformed"); workflow.logout(tokens.generate());
        var signed=com.nimbusds.jwt.SignedJWT.parse(jwt);
        var key=java.security.KeyFactory.getInstance("RSA").generatePublic(new java.security.spec.X509EncodedKeySpec(
                Base64.getMimeDecoder().decode(Files.readString(keyDirectory.resolve("public.pem"))
                        .replace("-----BEGIN PUBLIC KEY-----","").replace("-----END PUBLIC KEY-----",""))));
        assertTrue(signed.verify(new com.nimbusds.jose.crypto.RSASSAVerifier((java.security.interfaces.RSAPublicKey)key)));
        assertTrue(signed.getJWTClaimsSet().getExpirationTime().toInstant().isAfter(time.get()));
        assertNotNull(workflow.refresh(other.refreshCredential().credential()));
    }
    @Test void workflowLogoutExpiredAndRotatedCredentialDoesNotRevokeReplacement() throws Exception {
        var workflow=workflow();
        var first=workflow.login("workflow10@example.invalid"," test password ");
        var replacement=workflow.refresh(first.refreshCredential().credential());
        workflow.logout(first.refreshCredential().credential());
        assertEquals(1,count("id="+replacement.refreshCredential().sessionId()+" AND revoked_at IS NULL"));
        time.set(replacement.refreshCredential().expiresAt());
        workflow.logout(replacement.refreshCredential().credential());
        assertEquals(1,count("id="+replacement.refreshCredential().sessionId()+" AND revoked_at IS NULL"));
    }


    RegistrationService registration(VerificationMail mail) {
        return registration(mail, new com.englishlearning.features.auth.security.VerificationTokens());
    }
    RegistrationService registration(VerificationMail mail,
            com.englishlearning.features.auth.security.VerificationTokens source) {
        return new RegistrationService(credentials,
                context.getBean(com.englishlearning.features.auth.repository.EmailVerificationRepository.class),
                source, passwords, mail, clock, manager,
                context.getBean(com.englishlearning.features.teacher.repository.TeacherApplicationRepository.class));
    }
    String email() { return "registration-" + UUID.randomUUID() + "@example.invalid"; }
    long scalar(String sql) throws SQLException {
        try (var c=connection(); var st=c.createStatement(); var rs=st.executeQuery(sql)) {
            rs.next(); return rs.getLong(1);
        }
    }
    void verificationFailure(VerificationFailure.Reason reason, Runnable action) {
        var error=assertThrows(VerificationFailure.class, action::run);
        assertEquals(reason,error.reason()); assertNull(error.getCause());
    }
    @Test void studentProfilePreservesSecurityAndHistoricalNulls() throws Exception {
        String email=resetStudent();long id=credentials.findByEmail(email).orElseThrow().id();var before=credentials.findByEmail(email).orElseThrow();
        execute("UPDATE users SET username=NULL,phone=NULL WHERE id="+id);
        var profiles=context.getBean(com.englishlearning.features.user.service.StudentProfileService.class);
        assertNull(profiles.read(id).username());assertNull(profiles.read(id).phone());
        var updated=profiles.update(id,Map.of("fullName"," Preserved name "));assertEquals(" Preserved name ",updated.fullName());assertNull(updated.phone());
        assertEquals("+84912345678",profiles.update(id,Map.of("phone","0912345678")).phone());assertEquals(before,credentials.findByEmail(email).orElseThrow());
        assertThrows(com.englishlearning.features.user.service.ProfileFailure.class,()->profiles.update(id,Map.of("fullName","Should rollback","phone","bad")));assertEquals(" Preserved name ",profiles.read(id).fullName());
        var tx=new org.springframework.transaction.support.TransactionTemplate(manager);assertThrows(IllegalStateException.class,()->tx.execute(status->{profiles.update(id,Map.of("fullName","Rollback"));throw new IllegalStateException("rollback");}));assertEquals(" Preserved name ",profiles.read(id).fullName());
    }
    @Test void profileConcurrentPartialUpdatesPreserveBothFields() throws Exception {
        String email=resetStudent();long id=credentials.findByEmail(email).orElseThrow().id();var profiles=context.getBean(com.englishlearning.features.user.service.StudentProfileService.class);var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try{var a=pool.submit(()->{start.await();return profiles.update(id,Map.of("fullName","Concurrent"));});var b=pool.submit(()->{start.await();return profiles.update(id,Map.of("phone","0987654321"));});start.countDown();a.get(15,TimeUnit.SECONDS);b.get(15,TimeUnit.SECONDS);assertEquals("Concurrent",profiles.read(id).fullName());assertEquals("+84987654321",profiles.read(id).phone());}finally{pool.shutdownNow();}
    }
    @Test void profileLockedAndHistoricalUnverifiedAccountsCannotWrite() throws Exception {
        String email=resetStudent();long id=credentials.findByEmail(email).orElseThrow().id();var profiles=context.getBean(com.englishlearning.features.user.service.StudentProfileService.class);
        execute("UPDATE users SET student_login_without_verification=false WHERE id="+id);
        assertThrows(com.englishlearning.features.user.service.ProfileFailure.class,()->profiles.update(id,Map.of("fullName","Denied")));
        execute("UPDATE users SET email_verified=true,locked=true WHERE id="+id);
        assertThrows(com.englishlearning.features.user.service.ProfileFailure.class,()->profiles.update(id,Map.of("fullName","Denied")));
        assertEquals(1,scalar("SELECT count(*) FROM users WHERE id="+id+" AND full_name='Student'"));
    }
    @Test void profileSameFieldLastCommittedUpdateWins() throws Exception {
        String email=resetStudent();long id=credentials.findByEmail(email).orElseThrow().id();var profiles=context.getBean(com.englishlearning.features.user.service.StudentProfileService.class);
        var firstWritten=new CountDownLatch(1);var secondStarted=new CountDownLatch(1);var release=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try {
            var first=pool.submit(()->new org.springframework.transaction.support.TransactionTemplate(manager).execute(status->{profiles.update(id,Map.of("fullName","First"));firstWritten.countDown();try{assertTrue(release.await(10,TimeUnit.SECONDS));}catch(InterruptedException e){throw new RuntimeException(e);}return true;}));
            assertTrue(firstWritten.await(10,TimeUnit.SECONDS));var second=pool.submit(()->{secondStarted.countDown();return profiles.update(id,Map.of("fullName","Second"));});assertTrue(secondStarted.await(10,TimeUnit.SECONDS));release.countDown();assertTrue(first.get(15,TimeUnit.SECONDS));second.get(15,TimeUnit.SECONDS);assertEquals("Second",profiles.read(id).fullName());
        }finally{release.countDown();pool.shutdownNow();}
    }
    PasswordChangeService changes(){return new PasswordChangeService(credentials,sessions,context.getBean(com.englishlearning.features.auth.repository.PasswordResetRepository.class),passwords,clock,manager);}
    @Test void changePreservesCurrentSessionRevokesOthersAndInvalidatesOtp() throws Exception {
        String email=resetStudent();long id=credentials.findByEmail(email).orElseThrow().id();var current=workflow().login(email,"password");var other=workflow().login(email,"password");
        var sent=new ArrayList<String>();var recovery=recovery((e,t)->sent.add(t));recovery.forgot(email);
        changes().change(id,current.refreshCredential().credential(),"password","new password");
        assertEquals(1,count("id="+current.refreshCredential().sessionId()+" AND revoked_at IS NULL"));
        assertEquals(current.refreshCredential().expiresAt(),sessionsTime(current.refreshCredential().sessionId()));
        authenticationInvalid(()->workflow().login(email,"password"));assertNotNull(workflow().login(email,"new password"));
        authenticationInvalid(()->workflow().refresh(other.refreshCredential().credential()));assertNotNull(workflow().refresh(current.refreshCredential().credential()));
        verificationFailure(VerificationFailure.Reason.INVALID,()->recovery.reset(email,sent.get(0),"other password"));
        var user=credentials.findByEmail(email).orElseThrow();assertFalse(user.emailVerified());assertTrue(user.studentLoginWithoutVerification());
    }
    Instant sessionsTime(long id)throws Exception {try(var c=connection();var st=c.createStatement();var rs=st.executeQuery("SELECT expires_at FROM refresh_sessions WHERE id="+id)){rs.next();return rs.getTimestamp(1).toInstant();}}
    @Test void changeRejectsWrongSamePasswordOtherOwnerLockedAndExpired() throws Exception {
        String email=resetStudent();long id=credentials.findByEmail(email).orElseThrow().id();var current=workflow().login(email,"password");var change=changes();
        assertEquals(AuthenticationFailure.Reason.ACCESS_DENIED,assertThrows(AuthenticationFailure.class,()->change.change(id,current.refreshCredential().credential(),"wrong","new password")).reason());
        assertEquals(AuthenticationFailure.Reason.INVALID_REQUEST,assertThrows(AuthenticationFailure.class,()->change.change(id,current.refreshCredential().credential(),"password","password")).reason());
        assertEquals(AuthenticationFailure.Reason.INVALID_REQUEST,assertThrows(AuthenticationFailure.class,()->change.change(id,current.refreshCredential().credential(),"password","short")).reason());
        String otherEmail=resetStudent();var other=workflow().login(otherEmail,"password");
        assertThrows(AuthenticationFailure.class,()->change.change(id,other.refreshCredential().credential(),"password","new password"));
        execute("UPDATE users SET locked=true WHERE id="+id);assertThrows(AuthenticationFailure.class,()->change.change(id,current.refreshCredential().credential(),"password","new password"));execute("UPDATE users SET locked=false WHERE id="+id);
        time.set(current.refreshCredential().expiresAt());assertThrows(AuthenticationFailure.class,()->change.change(id,current.refreshCredential().credential(),"password","new password"));assertNotNull(workflow().login(email,"password"));
    }
    @Test void changeRollbackPreservesPasswordOtpAndOtherSessions() throws Exception {
        String email=resetStudent();long id=credentials.findByEmail(email).orElseThrow().id();var current=workflow().login(email,"password");var other=workflow().login(email,"password");
        var sent=new ArrayList<String>();var recovery=recovery((e,t)->sent.add(t));recovery.forgot(email);
        com.englishlearning.features.auth.repository.PasswordResetRepository target=org.springframework.test.util.AopTestUtils.getUltimateTargetObject(context.getBean(com.englishlearning.features.auth.repository.PasswordResetRepository.class));var broken=spy(target);doThrow(new IllegalStateException("test rollback")).when(broken).invalidateCredentials(any(),any());
        assertThrows(AuthenticationFailure.class,()->new PasswordChangeService(credentials,sessions,broken,passwords,clock,manager).change(id,current.refreshCredential().credential(),"password","new password"));
        assertNotNull(workflow().login(email,"password"));assertNotNull(workflow().refresh(other.refreshCredential().credential()));recovery.reset(email,sent.get(0),"recovered password");
    }
    @Test void concurrentChangesHaveOneWinner() throws Exception {
        String email=resetStudent();long id=credentials.findByEmail(email).orElseThrow().id();var current=workflow().login(email,"password");var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        Callable<Boolean> action=()->{start.await();try{changes().change(id,current.refreshCredential().credential(),"password","new password");return true;}catch(AuthenticationFailure e){assertEquals(AuthenticationFailure.Reason.ACCESS_DENIED,e.reason());return false;}};
        try{var a=pool.submit(action);var b=pool.submit(action);start.countDown();assertNotEquals(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));}finally{pool.shutdownNow();}
    }
    @Test void concurrentChangeAndRefreshCannotLeaveOtherSessionActive() throws Exception {
        String email=resetStudent();long id=credentials.findByEmail(email).orElseThrow().id();var current=workflow().login(email,"password");var other=workflow().login(email,"password");
        var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try {
            var change=pool.submit(()->{start.await();changes().change(id,current.refreshCredential().credential(),"password","new password");return true;});
            var refresh=pool.submit(()->{start.await();try{return workflow().refresh(other.refreshCredential().credential());}catch(AuthenticationFailure e){assertEquals(AuthenticationFailure.Reason.INVALID_CREDENTIAL,e.reason());return null;}});
            start.countDown();assertTrue(change.get(15,TimeUnit.SECONDS));var replacement=refresh.get(15,TimeUnit.SECONDS);
            if(replacement!=null)authenticationInvalid(()->workflow().refresh(replacement.refreshCredential().credential()));
            assertEquals(1,count("user_id="+id+" AND revoked_at IS NULL"));
        } finally {pool.shutdownNow();}
    }
    @Test void concurrentChangeAndResetHaveOneWinner() throws Exception {
        String email=resetStudent();long id=credentials.findByEmail(email).orElseThrow().id();var current=workflow().login(email,"password");var sent=new ArrayList<String>();var recovery=recovery((e,t)->sent.add(t));recovery.forgot(email);
        var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try {
            var change=pool.submit(()->{start.await();try{changes().change(id,current.refreshCredential().credential(),"password","changed password");return true;}catch(AuthenticationFailure e){assertEquals(AuthenticationFailure.Reason.INVALID_CREDENTIAL,e.reason());return false;}});
            var reset=pool.submit(()->{start.await();try{recovery.reset(email,sent.get(0),"reset password");return true;}catch(VerificationFailure e){assertEquals(VerificationFailure.Reason.INVALID,e.reason());return false;}});
            start.countDown();boolean changed=change.get(15,TimeUnit.SECONDS);assertNotEquals(changed,reset.get(15,TimeUnit.SECONDS));assertNotNull(workflow().login(email,changed?"changed password":"reset password"));
        } finally {pool.shutdownNow();}
    }
    PasswordRecoveryService recovery(PasswordResetMail mail) {
        return new PasswordRecoveryService(credentials,context.getBean(com.englishlearning.features.auth.repository.PasswordResetRepository.class),
                new com.englishlearning.features.auth.security.PasswordResetTokens(java.util.Base64.getEncoder().encodeToString(new byte[32])),passwords,mail,clock,manager);
    }
    String resetStudent() {
        String email=email();registration((e,t)->fail("Student email prohibited")).register(email,"password","Student","u"+UUID.randomUUID().toString().replace("-", "").substring(0,20),"0912345678");return email;
    }
    @Test void passwordResetChangesPasswordRevokesAllSessionsAndPreservesAccountPolicy() throws Exception {
        String email=resetStudent();var user=credentials.findByEmail(email).orElseThrow();
        var first=workflow().login(email,"password");var second=workflow().login(email,"password");
        var sent=new ArrayList<String>();var recovery=recovery((e,t)->sent.add(t));recovery.forgot(email);
        assertEquals(1,sent.size());
        assertEquals(1,scalar("SELECT count(*) FROM password_reset_credentials WHERE user_id="+user.id()+" AND length(secret_hash)=64"));
        recovery.reset(email,sent.get(0)," new password ");
        var changed=credentials.findByEmail(email).orElseThrow();assertFalse(changed.emailVerified());assertTrue(changed.studentLoginWithoutVerification());
        assertNotNull(workflow().login(email," new password "));
        assertThrows(AuthenticationFailure.class,()->workflow().login(email,"password"));
        assertThrows(AuthenticationFailure.class,()->workflow().refresh(first.refreshCredential().credential()));
        assertThrows(AuthenticationFailure.class,()->workflow().refresh(second.refreshCredential().credential()));
        verificationFailure(VerificationFailure.Reason.INVALID,()->recovery.reset(email,sent.get(0),"another password"));
    }
    @Test void resetExpiryInvalidCodesAndLockedAccountsCannotChangePassword() throws Exception {
        String email=resetStudent();long id=credentials.findByEmail(email).orElseThrow().id();var sent=new ArrayList<String>();var recovery=recovery((e,t)->sent.add(t));
        recovery.forgot(email);verificationFailure(VerificationFailure.Reason.INVALID,()->recovery.reset(email,"bad","new password"));
        execute("UPDATE users SET locked=true WHERE id="+id);
        verificationFailure(VerificationFailure.Reason.INVALID,()->recovery.reset(email,sent.get(0),"new password"));
        recovery.forgot(email);assertEquals(1,sent.size());execute("UPDATE users SET locked=false WHERE id="+id);
        time.set(time.get().plusSeconds(900));verificationFailure(VerificationFailure.Reason.INVALID,()->recovery.reset(email,sent.get(0),"new password"));
        assertNotNull(workflow().login(email,"password"));
    }
    @Test void resetRequestCooldownLimitAndFailedMailRemainNeutral() throws Exception {
        String email=resetStudent();long id=credentials.findByEmail(email).orElseThrow().id();var attempts=new java.util.concurrent.atomic.AtomicInteger();
        var recovery=recovery((e,t)->{attempts.incrementAndGet();throw new IllegalStateException("private mail");});
        recovery.forgot(email());recovery.forgot(email);recovery.forgot(email);assertEquals(1,attempts.get());
        for(int i=0;i<5;i++){time.set(time.get().plusSeconds(60));recovery.forgot(email);}
        assertEquals(5,attempts.get());assertEquals(5,scalar("SELECT count(*) FROM password_reset_credentials WHERE user_id="+id));
        time.set(time.get().plusSeconds(86400));recovery.forgot(email);assertEquals(6,attempts.get());
    }
    @Test void otpAttemptLimitIsAtomicAndResendInvalidatesPriorCode() throws Exception {
        String email=resetStudent();long id=credentials.findByEmail(email).orElseThrow().id();
        var sent=new ArrayList<String>();var recovery=recovery((e,t)->sent.add(t));recovery.forgot(email);
        String wrong=sent.get(0).equals("000000")?"000001":"000000";
        var pool=Executors.newFixedThreadPool(8);var start=new CountDownLatch(1);
        try {var futures=new ArrayList<Future<?>>();for(int i=0;i<8;i++)futures.add(pool.submit(()->{start.await();verificationFailure(VerificationFailure.Reason.INVALID,()->recovery.reset(email,wrong,"new password"));return true;}));start.countDown();for(var f:futures)f.get(20,TimeUnit.SECONDS);}
        finally {pool.shutdownNow();}
        assertEquals(5,scalar("SELECT failed_attempts FROM password_reset_credentials WHERE user_id="+id));
        verificationFailure(VerificationFailure.Reason.INVALID,()->recovery.reset(email,sent.get(0),"new password"));
        time.set(time.get().plusSeconds(60));recovery.forgot(email);
        assertEquals(1,scalar("SELECT count(*) FROM password_reset_credentials WHERE user_id="+id+" AND used_at IS NOT NULL"));
        if(!sent.get(0).equals(sent.get(1))) verificationFailure(VerificationFailure.Reason.INVALID,()->recovery.reset(email,sent.get(0),"new password"));
        recovery.reset(email,sent.get(1),"new password");assertNotNull(workflow().login(email,"new password"));
    }
    @Test void concurrentResetHasOneWinnerAndInvalidatesOlderCodes() throws Exception {
        String email=resetStudent();var sent=new ArrayList<String>();var recovery=recovery((e,t)->sent.add(t));recovery.forgot(email);
        time.set(time.get().plusSeconds(60));recovery.forgot(email);
        var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        Callable<Boolean> action=()->{start.await();try{recovery.reset(email,sent.get(1),"new password");return true;}
            catch(VerificationFailure failure){assertEquals(VerificationFailure.Reason.INVALID,failure.reason());return false;}};
        try {var a=pool.submit(action);var b=pool.submit(action);start.countDown();assertNotEquals(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));}
        finally {pool.shutdownNow();}
        verificationFailure(VerificationFailure.Reason.INVALID,()->recovery.reset(email,sent.get(0),"other password"));
    }
    @Test void concurrentResetRequestsCannotBypassCooldown() throws Exception {
        String email=resetStudent();var sent=new java.util.concurrent.atomic.AtomicInteger();var recovery=recovery((e,t)->sent.incrementAndGet());
        var pool=Executors.newFixedThreadPool(4);var start=new CountDownLatch(1);
        try {var futures=new ArrayList<Future<?>>();for(int i=0;i<4;i++)futures.add(pool.submit(()->{start.await();recovery.forgot(email);return true;}));start.countDown();for(var f:futures)f.get(15,TimeUnit.SECONDS);assertEquals(1,sent.get());}
        finally {pool.shutdownNow();}
    }
    @Test void resetRollbackPreservesPasswordCredentialAndRefreshAuthority() throws Exception {
        String email=resetStudent();var session=workflow().login(email,"password");var sent=new ArrayList<String>();var recovery=recovery((e,t)->sent.add(t));recovery.forgot(email);
        com.englishlearning.features.auth.repository.PasswordResetRepository target=org.springframework.test.util.AopTestUtils.getUltimateTargetObject(context.getBean(com.englishlearning.features.auth.repository.PasswordResetRepository.class));
        var repository=spy(target);
        doThrow(new IllegalStateException("test-only failure")).when(repository).invalidateAll(any(),any());
        var broken=new PasswordRecoveryService(credentials,repository,new com.englishlearning.features.auth.security.PasswordResetTokens(java.util.Base64.getEncoder().encodeToString(new byte[32])),passwords,(e,t)->{},clock,manager);
        verificationFailure(VerificationFailure.Reason.INTERNAL,()->broken.reset(email,sent.get(0),"new password"));
        assertNotNull(workflow().login(email,"password"));assertNotNull(workflow().refresh(session.refreshCredential().credential()));
        recovery.reset(email,sent.get(0),"new password");assertNotNull(workflow().login(email,"new password"));
    }
    @Test void resetDoesNotVerifyHistoricalStudentsTeachersOrAdmins() throws Exception {
        for(String role:new String[]{"STUDENT","TEACHER","ADMIN"}) {
            String email=resetStudent();long id=credentials.findByEmail(email).orElseThrow().id();
            execute("UPDATE users SET student_login_without_verification=false,role='"+role+"' WHERE id="+id);
            var sent=new ArrayList<String>();var recovery=recovery((e,t)->sent.add(t));recovery.forgot(email);recovery.reset(email,sent.get(0),"new password");
            var user=credentials.findByEmail(email).orElseThrow();assertFalse(user.emailVerified());assertFalse(user.studentLoginWithoutVerification());assertEquals(role,user.role().name());
            assertEquals(AuthenticationFailure.Reason.EMAIL_VERIFICATION_REQUIRED,assertThrows(AuthenticationFailure.class,()->workflow().login(email,"new password")).reason());
        }
    }
    @Test void resetAndRefreshRaceCannotLeaveAnActiveOldSession() throws Exception {
        String email=resetStudent();long id=credentials.findByEmail(email).orElseThrow().id();var session=workflow().login(email,"password");
        var sent=new ArrayList<String>();var recovery=recovery((e,t)->sent.add(t));recovery.forgot(email);
        var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try {var a=pool.submit(()->{start.await();recovery.reset(email,sent.get(0),"new password");return true;});
            var b=pool.submit(()->{start.await();try{workflow().refresh(session.refreshCredential().credential());}catch(AuthenticationFailure failure){assertEquals(AuthenticationFailure.Reason.INVALID_CREDENTIAL,failure.reason());}return true;});
            start.countDown();assertTrue(a.get(15,TimeUnit.SECONDS));assertTrue(b.get(15,TimeUnit.SECONDS));
            assertEquals(0,scalar("SELECT count(*) FROM refresh_sessions WHERE user_id="+id+" AND revoked_at IS NULL"));
        } finally {pool.shutdownNow();}
    }

    @Test void studentUsernameCollisionIsAtomicAndPhoneIsNotUnique() throws Exception {
        String handle="u"+UUID.randomUUID().toString().replace("-", "").substring(0,20);
        var delivered=new java.util.concurrent.atomic.AtomicInteger();
        var registration=registration((e,t)->delivered.incrementAndGet());
        var start=new CountDownLatch(1); var pool=Executors.newFixedThreadPool(2);
        Callable<Boolean> attempt=()->{start.await();try {
            registration.register(email(),"password","Student",handle.toUpperCase(Locale.ROOT),"0912345678");
            return true;
        } catch(VerificationFailure failure) { assertEquals(VerificationFailure.Reason.CONFLICT,failure.reason());return false; }};
        try {
            var first=pool.submit(attempt);var second=pool.submit(attempt);start.countDown();
            assertNotEquals(first.get(20,TimeUnit.SECONDS),second.get(20,TimeUnit.SECONDS));
        } finally {pool.shutdownNow();}
        assertEquals(0,delivered.get());
        assertEquals(1,scalar("SELECT count(*) FROM users WHERE username='"+handle+"' AND phone='+84912345678'"));
        assertEquals(0,scalar("SELECT count(*) FROM email_verification_credentials c JOIN users u ON u.id=c.user_id WHERE u.username='"+handle+"'"));
        registration.register(email(),"password","Other",handle+"x","+84912345678");
        assertEquals(2,scalar("SELECT count(*) FROM users WHERE username IN ('"+handle+"','"+handle+"x')"));
    }

    @Test void newStudentImmediatelyLogsInWithoutVerifyingOwnership() throws Exception {
        String email=email(); var sent=new ArrayList<String>();
        registration((e,t)->sent.add(t)).register(email," test password ","Student","u"+UUID.randomUUID().toString().replace("-", "").substring(0,20),"0912345678");
        var user=credentials.findByEmail(email).orElseThrow();
        assertFalse(user.emailVerified()); assertTrue(user.studentLoginWithoutVerification());
        assertTrue(sent.isEmpty());
        assertEquals(0,scalar("SELECT count(*) FROM email_verification_credentials WHERE user_id="+user.id()));
        var loggedIn=workflow().login(email," test password ");
        var refreshed=workflow().refresh(loggedIn.refreshCredential().credential());
        workflow().logout(refreshed.refreshCredential().credential());
        assertEquals(AuthenticationFailure.Reason.INVALID_CREDENTIAL,
                assertThrows(AuthenticationFailure.class,()->workflow().refresh(refreshed.refreshCredential().credential())).reason());
        var active=workflow().login(email," test password ");
        assertFalse(credentials.findByEmail(email).orElseThrow().emailVerified());
        execute("UPDATE users SET student_login_without_verification=false WHERE id="+user.id());
        assertEquals(AuthenticationFailure.Reason.INVALID_CREDENTIAL,
                assertThrows(AuthenticationFailure.class,()->workflow().refresh(active.refreshCredential().credential())).reason());
        assertEquals(AuthenticationFailure.Reason.EMAIL_VERIFICATION_REQUIRED,
                assertThrows(AuthenticationFailure.class,()->workflow().login(email," test password ")).reason());
        execute("UPDATE users SET student_login_without_verification=true,locked=true WHERE id="+user.id());
        assertEquals(AuthenticationFailure.Reason.INVALID_CREDENTIAL,
                assertThrows(AuthenticationFailure.class,()->workflow().refresh(active.refreshCredential().credential())).reason());
        assertEquals(AuthenticationFailure.Reason.INVALID_CREDENTIAL,
                assertThrows(AuthenticationFailure.class,()->workflow().login(email," test password ")).reason());
    }
    @Test void duplicateCanonicalRegistrationCannotOverwriteAccount() {
        String email=email(); var registration=registration((e,t)->{});
        registration.register(email,"password","First","u"+UUID.randomUUID().toString().replace("-", "").substring(0,20),"0912345678");
        var original=credentials.findByEmail(email).orElseThrow();
        verificationFailure(VerificationFailure.Reason.CONFLICT,
                ()->registration.register(" "+email.toUpperCase(Locale.ROOT)+" ","different password","Replacement","u"+UUID.randomUUID().toString().replace("-", "").substring(0,20),"0912345678"));
        assertEquals(original,credentials.findByEmail(email).orElseThrow());
    }
    @Test void verificationExpiresExactlyAt24HoursAndRejectsLockedOrWrongPurpose() throws Exception {
        String email=email(); var sent=new ArrayList<String>(); var registration=registration((e,t)->sent.add(t));
        registration.registerTeacher(email,"password","Test","English","Experience","Intro"); long id=credentials.findByEmail(email).orElseThrow().id();
        execute("UPDATE public.users SET locked=true WHERE id="+id);
        verificationFailure(VerificationFailure.Reason.INVALID,()->registration.verify(sent.get(0)));
        execute("UPDATE public.users SET locked=false WHERE id="+id);
        execute("UPDATE public.email_verification_credentials SET purpose='EMAIL_CHANGE' WHERE user_id="+id);
        verificationFailure(VerificationFailure.Reason.INVALID,()->registration.verify(sent.get(0)));
        execute("UPDATE public.email_verification_credentials SET purpose='INITIAL_VERIFICATION' WHERE user_id="+id);
        time.set(time.get().plusSeconds(86400));
        verificationFailure(VerificationFailure.Reason.INVALID,()->registration.verify(sent.get(0)));
        assertFalse(credentials.findByEmail(email).orElseThrow().emailVerified());
    }
    @Test void resendCooldownAndRollingLimitIncludeFailedDeliveryAndExcludeInitial() {
        String email=email(); var sent=new ArrayList<String>(); var registration=registration((e,t)->sent.add(t));
        registration.registerTeacher(email,"password","Test","English","Experience","Intro");
        verificationFailure(VerificationFailure.Reason.RATE_LIMITED,()->registration.resend(email));
        time.set(time.get().plusSeconds(59));
        verificationFailure(VerificationFailure.Reason.RATE_LIMITED,()->registration.resend(email));
        time.set(time.get().plusSeconds(1));
        verificationFailure(VerificationFailure.Reason.DELIVERY,()->registration((e,t)->{throw new IllegalStateException();}).resend(email));
        for(int i=0;i<4;i++) {time.set(time.get().plusSeconds(60));registration.resend(email);}
        time.set(time.get().plusSeconds(60));
        verificationFailure(VerificationFailure.Reason.RATE_LIMITED,()->registration.resend(email));
        assertEquals(5,sent.size());
        time.set(Instant.parse("2026-10-11T00:01:00Z").plusSeconds(86400));
        registration.resend(email); assertEquals(6,sent.size());
    }
    @Test void earlierUnexpiredCredentialRemainsValidAfterResend() {
        String email=email();var sent=new ArrayList<String>();var registration=registration((e,t)->sent.add(t));
        registration.registerTeacher(email,"password","Test","English","Experience","Intro");time.set(time.get().plusSeconds(60));registration.resend(email);
        registration.verify(sent.get(0));
        verificationFailure(VerificationFailure.Reason.INVALID,()->registration.verify(sent.get(1)));
        registration.resend(email); assertEquals(2,sent.size());
    }
    @Test void deliveryFailureRetainsAccountAndSupportsResendRecovery() {
        String email=email();
        verificationFailure(VerificationFailure.Reason.DELIVERY,
                ()->registration((e,t)->{throw new IllegalStateException("private body");}).registerTeacher(email,"password","Test","English","Experience","Intro"));
        assertFalse(credentials.findByEmail(email).orElseThrow().emailVerified());
        time.set(time.get().plusSeconds(60)); var sent=new ArrayList<String>();
        var registration=registration((e,t)->sent.add(t)); registration.resend(email);registration.verify(sent.get(0));
        assertTrue(credentials.findByEmail(email).orElseThrow().emailVerified());
    }
    @Test void credentialInsertFailureRollsBackNewAccount() {
        var source=mock(com.englishlearning.features.auth.security.VerificationTokens.class);
        when(source.generate()).thenReturn("test"); when(source.hash(anyString())).thenReturn(null);
        String email=email();
        verificationFailure(VerificationFailure.Reason.INTERNAL,
                ()->registration((e,t)->fail("Must not deliver"),source).registerTeacher(email,"password","Test","English","Experience","Intro"));
        assertTrue(credentials.findByEmail(email).isEmpty());
    }
    @Test void concurrentVerificationHasExactlyOneWinner() throws Exception {
        String email=email(); var sent=new CopyOnWriteArrayList<String>();var registration=registration((e,t)->sent.add(t));
        registration.registerTeacher(email,"password","Test","English","Experience","Intro");
        var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        Callable<Boolean> action=()->{start.await();try{registration.verify(sent.get(0));return true;}
            catch(VerificationFailure e){assertEquals(VerificationFailure.Reason.INVALID,e.reason());return false;}};
        try {var a=pool.submit(action);var b=pool.submit(action);start.countDown();
            assertNotEquals(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS));
        } finally {pool.shutdownNow();}
    }
    @Test void concurrentResendsCannotBypassCooldownOrFinalAllowance() throws Exception {
        String email=email();var sent=new CopyOnWriteArrayList<String>();var registration=registration((e,t)->sent.add(t));
        registration.registerTeacher(email,"password","Test","English","Experience","Intro");
        for(int i=0;i<4;i++){time.set(time.get().plusSeconds(60));registration.resend(email);}
        time.set(time.get().plusSeconds(60));var pool=Executors.newFixedThreadPool(6);var start=new CountDownLatch(1);
        Callable<Boolean> action=()->{start.await();try{registration.resend(email);return true;}
            catch(VerificationFailure e){assertEquals(VerificationFailure.Reason.RATE_LIMITED,e.reason());return false;}};
        try {var results=new ArrayList<Future<Boolean>>();for(int i=0;i<6;i++)results.add(pool.submit(action));start.countDown();
            int wins=0;for(var result:results)if(result.get(10,TimeUnit.SECONDS))wins++;
            assertEquals(1,wins);assertEquals(6,sent.size());
        } finally {pool.shutdownNow();}
    }
    @Test void simultaneousResendAndVerificationPreserveAccountAndSingleUse() throws Exception {
        String email=email();var sent=new CopyOnWriteArrayList<String>();var registration=registration((e,t)->sent.add(t));
        registration.registerTeacher(email,"password","Test","English","Experience","Intro");time.set(time.get().plusSeconds(60));
        var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try {var a=pool.submit(()->{start.await();registration.verify(sent.get(0));return true;});
            var b=pool.submit(()->{start.await();registration.resend(email);return true;});start.countDown();
            assertTrue(a.get(10,TimeUnit.SECONDS));assertTrue(b.get(10,TimeUnit.SECONDS));
            assertTrue(credentials.findByEmail(email).orElseThrow().emailVerified());
            for(String token:sent)verificationFailure(VerificationFailure.Reason.INVALID,()->registration.verify(token));
        } finally {pool.shutdownNow();}
    }
    @Test void unknownVerifiedAndLockedResendAreNonDisclosingNoOps() throws Exception {
        var registration=registration((e,t)->fail("No message permitted"));
        registration.resend(email());registration.resend("workflow10@example.invalid");registration.resend("two@example.invalid");
    }

    @Test void teacherRegistrationAtomicallyRetainsSnapshotAndUsesSharedVerificationLogin() throws Exception {
        String email=email();var sent=new ArrayList<String>();var registration=registration((e,t)->sent.add(t));
        registration.registerTeacher(email," test password ","Teacher"," English "," Experience "," Intro ");
        var user=credentials.findByEmail(email).orElseThrow();
        assertEquals("TEACHER",accounts.findById(user.id()).orElseThrow().role().name());
        assertFalse(user.emailVerified());assertFalse(user.locked());assertEquals(0,count("user_id="+user.id()));
        try(var c=connection();var st=c.prepareStatement("SELECT specialization,teaching_experience,introduction,status,reviewed_at,reviewed_by,rejection_reason,submitted_at=created_at AND created_at=updated_at FROM public.teacher_applications WHERE teacher_id=?")) {
            st.setLong(1,user.id());try(var rs=st.executeQuery()) {
                assertTrue(rs.next());assertEquals(" English ",rs.getString(1));assertEquals(" Experience ",rs.getString(2));
                assertEquals(" Intro ",rs.getString(3));assertEquals("PENDING",rs.getString(4));
                for(int i=5;i<=7;i++)assertNull(rs.getObject(i));assertTrue(rs.getBoolean(8));assertFalse(rs.next());
            }
        }
        assertEquals(0,scalar("SELECT count(*) FROM public.teacher_profiles WHERE teacher_id="+user.id()));
        assertEquals(AuthenticationFailure.Reason.EMAIL_VERIFICATION_REQUIRED,
                assertThrows(AuthenticationFailure.class,()->workflow().login(email," test password ")).reason());
        time.set(time.get().plusSeconds(60));registration.resend(email);assertEquals(2,sent.size());
        registration.verify(sent.get(0));assertNotNull(workflow().login(email," test password "));
        assertEquals(1,scalar("SELECT count(*) FROM public.teacher_applications WHERE teacher_id="+user.id()+" AND status='PENDING'"));
        assertEquals(0,scalar("SELECT count(*) FROM public.teacher_profiles WHERE teacher_id="+user.id()));
    }
    @Test void teacherApplicationInsertFailureRollsBackUserAndCredential() throws Exception {
        String email=email();long before=scalar("SELECT count(*) FROM public.email_verification_credentials");
        var broken=mock(com.englishlearning.features.teacher.repository.TeacherApplicationRepository.class);
        doThrow(new org.springframework.dao.DataIntegrityViolationException("test-only application failure")).when(broken).saveAndFlush(any());
        var registration=new RegistrationService(credentials,
                context.getBean(com.englishlearning.features.auth.repository.EmailVerificationRepository.class),
                new com.englishlearning.features.auth.security.VerificationTokens(),passwords,(e,t)->fail("No email"),clock,manager,broken);
        verificationFailure(VerificationFailure.Reason.INTERNAL,
                ()->registration.registerTeacher(email,"password","Teacher","English","Experience","Intro"));
        assertTrue(credentials.findByEmail(email).isEmpty());
        assertEquals(before,scalar("SELECT count(*) FROM public.email_verification_credentials"));
    }
    @Test void teacherCredentialFailureRollsBackAndSmtpFailurePreservesAllRows() throws Exception {
        var source=mock(com.englishlearning.features.auth.security.VerificationTokens.class);
        when(source.generate()).thenReturn("test");when(source.hash(any())).thenReturn(null);
        String failed=email();verificationFailure(VerificationFailure.Reason.INTERNAL,
                ()->registration((e,t)->fail("No email"),source).registerTeacher(failed,"password","Teacher","English","Experience","Intro"));
        assertTrue(credentials.findByEmail(failed).isEmpty());
        String persisted=email();verificationFailure(VerificationFailure.Reason.DELIVERY,
                ()->registration((e,t)->{throw new IllegalStateException();}).registerTeacher(persisted,"password","Teacher","English","Experience","Intro"));
        long id=credentials.findByEmail(persisted).orElseThrow().id();
        assertEquals(1,scalar("SELECT count(*) FROM public.teacher_applications WHERE teacher_id="+id));
        assertEquals(1,scalar("SELECT count(*) FROM public.email_verification_credentials WHERE user_id="+id));
    }
    @Test void concurrentTeacherRegistrationsHaveOneWinnerWithoutOverwriting() throws Exception {
        String email=email();var sent=new CopyOnWriteArrayList<String>();var registration=registration((e,t)->sent.add(t));
        var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        Callable<Boolean> action=()->{start.await();try{registration.registerTeacher(" "+email.toUpperCase(Locale.ROOT)+" ","password","Teacher","English","Experience","Intro");return true;}
            catch(VerificationFailure e){assertEquals(VerificationFailure.Reason.CONFLICT,e.reason());return false;}};
        try{var a=pool.submit(action);var b=pool.submit(action);start.countDown();
            assertNotEquals(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS));assertEquals(1,sent.size());
            long id=credentials.findByEmail(email).orElseThrow().id();
            assertEquals(1,scalar("SELECT count(*) FROM public.teacher_applications WHERE teacher_id="+id));
            assertEquals(1,scalar("SELECT count(*) FROM public.email_verification_credentials WHERE user_id="+id));
        }finally{pool.shutdownNow();}
    }
    @Test void studentTeacherRegistrationRaceCannotPromoteOrDuplicateUser() throws Exception {
        String email=email();var registration=registration((e,t)->{});var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try{var student=pool.submit(()->{start.await();try{registration.register(email,"password","Student","u"+UUID.randomUUID().toString().replace("-", "").substring(0,20),"0912345678");return true;}
                catch(VerificationFailure e){assertEquals(VerificationFailure.Reason.CONFLICT,e.reason());return false;}});
            var teacher=pool.submit(()->{start.await();try{registration.registerTeacher(email,"password","Teacher","English","Experience","Intro");return true;}
                catch(VerificationFailure e){assertEquals(VerificationFailure.Reason.CONFLICT,e.reason());return false;}});
            start.countDown();boolean studentWon=student.get(10,TimeUnit.SECONDS);assertNotEquals(studentWon,teacher.get(10,TimeUnit.SECONDS));
            long id=credentials.findByEmail(email).orElseThrow().id();
            assertEquals(studentWon?"STUDENT":"TEACHER",accounts.findById(id).orElseThrow().role().name());
            assertEquals(studentWon?0:1,scalar("SELECT count(*) FROM public.teacher_applications WHERE teacher_id="+id));
            assertEquals(studentWon?0:1,scalar("SELECT count(*) FROM public.email_verification_credentials WHERE user_id="+id));
        }finally{pool.shutdownNow();}
    }

    com.englishlearning.features.teacher.service.TeacherApplicationReviewService reviews() {
        return context.getBean(com.englishlearning.features.teacher.service.TeacherApplicationReviewService.class);
    }
    com.englishlearning.features.teacher.service.TeacherBusinessEligibility teacherEligibility() {
        return context.getBean(com.englishlearning.features.teacher.service.TeacherBusinessEligibility.class);
    }
    long pendingApplication() throws Exception {
        String email=email(); registration((e,t)->{}).registerTeacher(email,"password","Teacher"," English "," Experience "," Intro ");
        long teacher=credentials.findByEmail(email).orElseThrow().id();
        return scalar("SELECT id FROM public.teacher_applications WHERE teacher_id="+teacher);
    }
    long applicant(long application) throws Exception {
        return scalar("SELECT teacher_id FROM public.teacher_applications WHERE id="+application);
    }
    void reviewFailure(com.englishlearning.features.teacher.service.TeacherReviewFailure.Reason reason, Runnable action) {
        assertEquals(reason,assertThrows(com.englishlearning.features.teacher.service.TeacherReviewFailure.class,action::run).reason());
    }
    @Test void approvalCopiesSnapshotAndDoesNotUnlockOrVerifyApplicant() throws Exception {
        long id=pendingApplication(), teacher=applicant(id);
        execute("UPDATE public.users SET locked=true WHERE id="+teacher);
        reviews().review(12L,id,true,null);
        var detail=reviews().detail(12L,id);
        assertEquals("APPROVED",detail.status()); assertEquals("12",detail.reviewedBy().decimal());assertNotNull(detail.reviewedAt());
        assertNull(detail.rejectionReason());
        assertEquals(1,scalar("SELECT count(*) FROM public.teacher_profiles WHERE teacher_id="+teacher
                +" AND avatar_path IS NULL AND specialization=' English ' AND teaching_experience=' Experience ' AND introduction=' Intro '"));
        var account=accounts.findById(teacher).orElseThrow();assertTrue(account.locked());assertFalse(account.emailVerified());
        reviewFailure(com.englishlearning.features.teacher.service.TeacherReviewFailure.Reason.DENIED,()->teacherEligibility().requireEligible(teacher));
        execute("UPDATE public.users SET locked=false,email_verified=true WHERE id="+teacher);
        teacherEligibility().requireEligible(teacher);
        execute("UPDATE public.users SET role='STUDENT' WHERE id="+teacher);
        reviewFailure(com.englishlearning.features.teacher.service.TeacherReviewFailure.Reason.DENIED,()->teacherEligibility().requireEligible(teacher));
    }

    @Test void rejectionPreservesReasonAndReviewedHistoryIsImmutable() throws Exception {
        long id=pendingApplication(), teacher=applicant(id);
        reviews().review(12L,id,false," reason ");
        var before=reviews().detail(12L,id);assertEquals("REJECTED",before.status());assertEquals(" reason ",before.rejectionReason());
        assertEquals(0,scalar("SELECT count(*) FROM public.teacher_profiles WHERE teacher_id="+teacher));
        reviewFailure(com.englishlearning.features.teacher.service.TeacherReviewFailure.Reason.ALREADY_REVIEWED,()->reviews().review(12L,id,true,null));
        assertEquals(before,reviews().detail(12L,id));
        execute("UPDATE public.users SET email_verified=true WHERE id="+teacher);
        reviewFailure(com.englishlearning.features.teacher.service.TeacherReviewFailure.Reason.DENIED,()->teacherEligibility().requireEligible(teacher));
    }
    @Test void existingProfileConflictDoesNotChangeApplicationOrProfile() throws Exception {
        long id=pendingApplication(),teacher=applicant(id);
        execute("INSERT INTO public.teacher_profiles(teacher_id,specialization,teaching_experience,introduction,created_at,updated_at) VALUES ("
                +teacher+",'original','original','original',now(),now())");
        reviewFailure(com.englishlearning.features.teacher.service.TeacherReviewFailure.Reason.PROFILE_EXISTS,()->reviews().review(12L,id,true,null));
        assertEquals("PENDING",reviews().detail(12L,id).status());assertNull(reviews().detail(12L,id).reviewedAt());
        assertEquals(1,scalar("SELECT count(*) FROM public.teacher_profiles WHERE teacher_id="+teacher+" AND introduction='original'"));
        execute("UPDATE public.users SET email_verified=true WHERE id="+teacher);
        reviewFailure(com.englishlearning.features.teacher.service.TeacherReviewFailure.Reason.DENIED,()->teacherEligibility().requireEligible(teacher));
    }
    @Test void concurrentOpposingReviewsHaveExactlyOneWinner() throws Exception {
        long id=pendingApplication(),teacher=applicant(id);var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        Callable<Boolean> approve=()->{start.await();try{reviews().review(12L,id,true,null);return true;}
            catch(com.englishlearning.features.teacher.service.TeacherReviewFailure e){assertEquals(com.englishlearning.features.teacher.service.TeacherReviewFailure.Reason.ALREADY_REVIEWED,e.reason());return false;}};
        Callable<Boolean> reject=()->{start.await();try{reviews().review(12L,id,false,"reason");return true;}
            catch(com.englishlearning.features.teacher.service.TeacherReviewFailure e){assertEquals(com.englishlearning.features.teacher.service.TeacherReviewFailure.Reason.ALREADY_REVIEWED,e.reason());return false;}};
        try{var a=pool.submit(approve);var b=pool.submit(reject);start.countDown();
            boolean approved=a.get(10,TimeUnit.SECONDS);assertNotEquals(approved,b.get(10,TimeUnit.SECONDS));
            assertEquals(approved?"APPROVED":"REJECTED",reviews().detail(12L,id).status());
            assertEquals(approved?1:0,scalar("SELECT count(*) FROM public.teacher_profiles WHERE teacher_id="+teacher));
        }finally{pool.shutdownNow();}
    }
    @Test void reviewPersistenceFailureRollsBackInsertedProfile() throws Exception {
        long id=pendingApplication(),teacher=applicant(id);
        var real=context.getBean(com.englishlearning.features.teacher.repository.TeacherApplicationRepository.class);
        var broken=mock(com.englishlearning.features.teacher.repository.TeacherApplicationRepository.class,org.mockito.AdditionalAnswers.delegatesTo(real));
        doThrow(new org.springframework.dao.DataIntegrityViolationException("test-only flush failure")).when(broken).saveAndFlush(any());
        var service=new com.englishlearning.features.teacher.service.TeacherApplicationReviewService(broken,
                context.getBean(com.englishlearning.features.teacher.repository.TeacherProfileRepository.class),credentials,accounts,clock);
        var tx=new org.springframework.transaction.support.TransactionTemplate(manager);
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->tx.execute(s->{service.review(12L,id,true,null);return null;}));
        assertEquals("PENDING",reviews().detail(12L,id).status());
        assertEquals(0,scalar("SELECT count(*) FROM public.teacher_profiles WHERE teacher_id="+teacher));
    }
    @Test void reviewChecksCurrentReviewerAndApplicantWithoutBusinessBypass() throws Exception {
        long id=pendingApplication(),teacher=applicant(id);
        for(long reviewer:new long[]{10L,11L,999999L})
            reviewFailure(com.englishlearning.features.teacher.service.TeacherReviewFailure.Reason.DENIED,()->reviews().review(reviewer,id,true,null));
        try {
            execute("UPDATE public.users SET email_verified=false WHERE id=12");
            reviewFailure(com.englishlearning.features.teacher.service.TeacherReviewFailure.Reason.DENIED,()->reviews().detail(12L,id));
            execute("UPDATE public.users SET email_verified=true,locked=true WHERE id=12");
            reviewFailure(com.englishlearning.features.teacher.service.TeacherReviewFailure.Reason.DENIED,()->reviews().review(12L,id,true,null));
        } finally { execute("UPDATE public.users SET email_verified=true,locked=false WHERE id=12"); }
        execute("UPDATE public.users SET role='STUDENT' WHERE id="+teacher);
        reviewFailure(com.englishlearning.features.teacher.service.TeacherReviewFailure.Reason.INVALID_APPLICANT,()->reviews().review(12L,id,true,null));
        assertEquals("PENDING",reviews().detail(12L,id).status());
        reviewFailure(com.englishlearning.features.teacher.service.TeacherReviewFailure.Reason.NOT_FOUND,()->reviews().detail(12L,Long.MAX_VALUE));
    }

    com.englishlearning.features.teacher.service.TeacherOnboardingService onboarding() {
        return context.getBean(com.englishlearning.features.teacher.service.TeacherOnboardingService.class);
    }
    long rejectedForResubmission() throws Exception {
        long id=pendingApplication();reviews().review(12L,id,false," original reason ");
        execute("UPDATE public.users SET email_verified=true WHERE id="+applicant(id));return id;
    }
    @Test void resubmissionPreservesHistoryAndRejectsDelayedRetries() throws Exception {
        long old=rejectedForResubmission(),teacher=applicant(old);var history=reviews().detail(12L,old);
        var created=onboarding().resubmit(teacher,old," New "," Experience "," Intro ");
        long next=created.applicationId().value();assertNotEquals(old,next);assertEquals("PENDING",created.status());
        assertNull(created.reviewedAt());assertNull(created.rejectionReason());assertEquals(history,reviews().detail(12L,old));
        assertEquals(" New ",reviews().detail(12L,next).specialization());
        assertEquals(created,onboarding().ownStatus(teacher));
        assertEquals(0,scalar("SELECT count(*) FROM public.teacher_profiles WHERE teacher_id="+teacher));
        reviewFailure(com.englishlearning.features.teacher.service.TeacherReviewFailure.Reason.ALREADY_PENDING,
                ()->onboarding().resubmit(teacher,old,"a","b","c"));
        reviews().review(12L,next,false,"second rejection");
        reviewFailure(com.englishlearning.features.teacher.service.TeacherReviewFailure.Reason.RESUBMISSION_NOT_ALLOWED,
                ()->onboarding().resubmit(teacher,old,"a","b","c"));
        assertEquals("PENDING",onboarding().resubmit(teacher,next,"a","b","c").status());
        assertEquals(history,reviews().detail(12L,old));
        reviewFailure(com.englishlearning.features.teacher.service.TeacherReviewFailure.Reason.DENIED,
                ()->teacherEligibility().requireEligible(teacher));
    }
    @Test void concurrentResubmissionsHaveExactlyOneWinner() throws Exception {
        long id=rejectedForResubmission(),teacher=applicant(id);var before=reviews().detail(12L,id);
        var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        Callable<Boolean> submit=()->{start.await();try{onboarding().resubmit(teacher,id,"a","b","c");return true;}
            catch(com.englishlearning.features.teacher.service.TeacherReviewFailure e){
                assertEquals(com.englishlearning.features.teacher.service.TeacherReviewFailure.Reason.ALREADY_PENDING,e.reason());return false;}};
        try{var a=pool.submit(submit);var b=pool.submit(submit);start.countDown();
            assertNotEquals(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS));
        }finally{pool.shutdownNow();}
        assertEquals(2,scalar("SELECT count(*) FROM public.teacher_applications WHERE teacher_id="+teacher));
        assertEquals(1,scalar("SELECT count(*) FROM public.teacher_applications WHERE teacher_id="+teacher+" AND status='PENDING'"));
        assertEquals(before,reviews().detail(12L,id));
    }
    @Test void resubmissionChecksOwnershipApprovalProfileAndCurrentAccount() throws Exception {
        long id=rejectedForResubmission(),teacher=applicant(id),other=rejectedForResubmission();
        reviewFailure(com.englishlearning.features.teacher.service.TeacherReviewFailure.Reason.NOT_FOUND,
                ()->onboarding().resubmit(teacher,other,"a","b","c"));
        reviewFailure(com.englishlearning.features.teacher.service.TeacherReviewFailure.Reason.NOT_FOUND,
                ()->onboarding().resubmit(teacher,Long.MAX_VALUE,"a","b","c"));
        for(String assignment:List.of("locked=true","locked=false,email_verified=false","email_verified=true,role='STUDENT'")) {
            execute("UPDATE public.users SET "+assignment+" WHERE id="+teacher);
            reviewFailure(com.englishlearning.features.teacher.service.TeacherReviewFailure.Reason.DENIED,
                    ()->onboarding().resubmit(teacher,id,"a","b","c"));
        }
        execute("UPDATE public.users SET role='TEACHER' WHERE id="+teacher);
        execute("INSERT INTO public.teacher_profiles(teacher_id,specialization,teaching_experience,introduction,created_at,updated_at) VALUES ("+teacher+",'a','b','c',now(),now())");
        reviewFailure(com.englishlearning.features.teacher.service.TeacherReviewFailure.Reason.PROFILE_EXISTS,
                ()->onboarding().resubmit(teacher,id,"a","b","c"));
        long approved=pendingApplication(),approvedTeacher=applicant(approved);
        reviews().review(12L,approved,true,null);execute("UPDATE public.users SET email_verified=true WHERE id="+approvedTeacher);
        reviewFailure(com.englishlearning.features.teacher.service.TeacherReviewFailure.Reason.RESUBMISSION_NOT_ALLOWED,
                ()->onboarding().resubmit(approvedTeacher,approved,"a","b","c"));
        assertEquals(1,scalar("SELECT count(*) FROM public.teacher_applications WHERE teacher_id="+teacher));
    }
    @Test void resubmissionInsertFailureRollsBackAndReleasesAccountLock() throws Exception {
        long id=rejectedForResubmission(),teacher=applicant(id);var before=reviews().detail(12L,id);
        var real=context.getBean(com.englishlearning.features.teacher.repository.TeacherApplicationRepository.class);
        var failing=mock(com.englishlearning.features.teacher.repository.TeacherApplicationRepository.class,org.mockito.AdditionalAnswers.delegatesTo(real));
        doAnswer(invocation->{real.saveAndFlush(invocation.getArgument(0));throw new IllegalStateException("test failure after insert");})
                .when(failing).saveAndFlush(any());
        var service=new com.englishlearning.features.teacher.service.TeacherOnboardingService(accounts,failing,credentials,
                context.getBean(com.englishlearning.features.teacher.repository.TeacherProfileRepository.class),clock);
        var tx=new org.springframework.transaction.support.TransactionTemplate(manager);
        tx.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
        assertThrows(IllegalStateException.class,()->tx.execute(status->service.resubmit(teacher,id,"a","b","c")));
        assertEquals(1,scalar("SELECT count(*) FROM public.teacher_applications WHERE teacher_id="+teacher));
        assertEquals(before,reviews().detail(12L,id));
        assertEquals("PENDING",onboarding().resubmit(teacher,id,"a","b","c").status());
    }
    @Test void pendingQueueFiltersOrdersBoundsAndReflectsReviews() throws Exception {
        long first=pendingApplication(),second=pendingApplication(),reviewed=pendingApplication();
        execute("UPDATE public.teacher_applications SET submitted_at='1900-01-01T00:00:00Z' WHERE id IN ("+first+","+second+","+reviewed+")");
        execute("UPDATE public.users SET locked=true WHERE id="+applicant(first));
        reviews().review(12L,reviewed,false,"reviewed");
        var page=reviews().pending(12L,0,1);
        assertEquals(1,page.items().size());assertTrue(page.hasNext());
        assertEquals(Long.toString(first),page.items().get(0).id().decimal());
        assertEquals(Long.toString(applicant(first)),page.items().get(0).teacherId().decimal());
        assertEquals("PENDING",page.items().get(0).status());
        assertEquals(Instant.parse("1900-01-01T00:00:00Z"),page.items().get(0).submittedAt());
        assertEquals(Long.toString(second),reviews().pending(12L,1,1).items().get(0).id().decimal());
        long count=scalar("SELECT count(*) FROM public.teacher_applications WHERE status='PENDING'");
        assertFalse(reviews().pending(12L,(int)count-1,1).hasNext());
        var empty=reviews().pending(12L,(int)count,1);
        assertTrue(empty.items().isEmpty());assertFalse(empty.hasNext());
        assertTrue(reviews().pending(12L,Integer.MAX_VALUE,1).items().isEmpty());
        reviews().review(12L,first,false,"reviewed after listing");
        assertEquals(Long.toString(second),reviews().pending(12L,0,1).items().get(0).id().decimal());
        reviewFailure(com.englishlearning.features.teacher.service.TeacherReviewFailure.Reason.ALREADY_REVIEWED,
                ()->reviews().review(12L,first,true,null));
        for(long denied:new long[]{10L,11L,Long.MAX_VALUE})
            reviewFailure(com.englishlearning.features.teacher.service.TeacherReviewFailure.Reason.DENIED,
                    ()->reviews().pending(denied,0,20));
    }
    @Test void ownStatusSupportsAllStatesAndNeverChangesApplication() throws Exception {
        for(boolean approve:new boolean[]{true,false}) {
            long id=pendingApplication(),teacher=applicant(id);
            execute("UPDATE public.users SET email_verified=true WHERE id="+teacher);
            var before=reviews().detail(12L,id);
            var pending=onboarding().ownStatus(teacher);
            assertEquals("PENDING",pending.status());assertNull(pending.reviewedAt());assertNull(pending.rejectionReason());
            assertEquals(Long.toString(id),pending.applicationId().decimal());
            assertEquals(before,reviews().detail(12L,id));
            reviews().review(12L,id,approve,approve?null:" reason ");
            var reviewed=reviews().detail(12L,id);
            var status=onboarding().ownStatus(teacher);
            assertEquals(approve?"APPROVED":"REJECTED",status.status());assertEquals(reviewed.reviewedAt(),status.reviewedAt());
            assertEquals(approve?null:" reason ",status.rejectionReason());assertEquals(reviewed,reviews().detail(12L,id));
        }
    }
    @Test void latestOwnApplicationOrdersByTimestampThenIdAndIsolatesOwners() throws Exception {
        long first=pendingApplication(),teacher=applicant(first),other=pendingApplication();
        execute("UPDATE public.users SET email_verified=true WHERE id IN ("+teacher+","+applicant(other)+")");
        execute("INSERT INTO public.teacher_applications(teacher_id,specialization,teaching_experience,introduction,status,submitted_at,reviewed_at,reviewed_by,rejection_reason,created_at,updated_at) "
                +"SELECT teacher_id,specialization,teaching_experience,introduction,'REJECTED',submitted_at-interval '1 day',now(),12,'history',created_at,updated_at FROM public.teacher_applications WHERE id="+first);
        long history=scalar("SELECT max(id) FROM public.teacher_applications WHERE teacher_id="+teacher);
        assertTrue(history>first);
        assertEquals(Long.toString(first),onboarding().ownStatus(teacher).applicationId().decimal());
        execute("UPDATE public.teacher_applications SET submitted_at=(SELECT submitted_at FROM public.teacher_applications WHERE id="+first+") WHERE id="+history);
        assertEquals(Long.toString(history),onboarding().ownStatus(teacher).applicationId().decimal());
        assertEquals(Long.toString(other),onboarding().ownStatus(applicant(other)).applicationId().decimal());
        assertEquals(2,scalar("SELECT count(*) FROM public.teacher_applications WHERE teacher_id="+teacher));
    }
    @Test void ownStatusMissingAndCurrentAccountRestrictionsAreEnforced() throws Exception {
        long missing = new org.springframework.transaction.support.TransactionTemplate(manager).execute(
                tx -> credentials.createTeacher(email(),passwordHash,"No application",clock.instant()).orElseThrow());
        execute("UPDATE public.users SET email_verified=true WHERE id="+missing);
        reviewFailure(com.englishlearning.features.teacher.service.TeacherReviewFailure.Reason.ONBOARDING_NOT_FOUND,()->onboarding().ownStatus(missing));
        long id=pendingApplication(),teacher=applicant(id);
        reviewFailure(com.englishlearning.features.teacher.service.TeacherReviewFailure.Reason.DENIED,()->onboarding().ownStatus(teacher));
        execute("UPDATE public.users SET email_verified=true,locked=true WHERE id="+teacher);
        reviewFailure(com.englishlearning.features.teacher.service.TeacherReviewFailure.Reason.DENIED,()->onboarding().ownStatus(teacher));
        execute("UPDATE public.users SET locked=false WHERE id="+teacher);
        assertEquals("PENDING",onboarding().ownStatus(teacher).status());
        for(long denied:new long[]{10L,12L,Long.MAX_VALUE})
            reviewFailure(com.englishlearning.features.teacher.service.TeacherReviewFailure.Reason.DENIED,()->onboarding().ownStatus(denied));
    }
}
