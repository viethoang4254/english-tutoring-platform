package com.englishlearning.features.user;

import com.englishlearning.EnglishLearningApplication;
import com.englishlearning.features.user.entity.User.Role;
import com.englishlearning.features.user.service.CurrentAccountService;
import java.sql.DriverManager;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.core.env.MapPropertySource;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "accountPersistence", matches = "true")
class CurrentAccountPersistenceTests {
    @Test
    void readsCurrentAccountFromIsolatedV1SchemaWithoutMutatingIt() throws Exception {
        String url = System.getenv("TASK006_TEST_URL");
        assertNotNull(url, "Explicit isolated test configuration is required");
        assertEquals("jdbc:postgresql://127.0.0.1:55432/task006_accounts", url);
        String user = System.getenv("TASK006_TEST_USER");
        String password = System.getenv("TASK006_TEST_PASSWORD");
        assertEquals("task006_test", user);
        assertNotNull(password);
        try (var connection = DriverManager.getConnection(url, user, password);
             var statement = connection.createStatement()) {
            try (var tables = statement.executeQuery("SELECT count(*) FROM pg_tables WHERE schemaname='public'")) {
                assertTrue(tables.next());
                assertEquals(0, tables.getInt(1), "Only a fresh disposable database may be used");
            }
            var properties = Map.<String, Object>of(
                    "spring.datasource.url", url, "spring.datasource.username", user, "spring.datasource.password", password,
                    "spring.flyway.enabled", true, "spring.flyway.clean-disabled", true,
                    "spring.jpa.hibernate.ddl-auto", "validate", "spring.sql.init.mode", "never",
                    "spring.jpa.open-in-view", false);
            try (var context = new SpringApplicationBuilder(EnglishLearningApplication.class)
                    .web(WebApplicationType.NONE)
                    .initializers(c -> c.getEnvironment().getPropertySources()
                            .addFirst(new MapPropertySource("isolated-account-test", properties)))
                    .run()) {
                var accounts = context.getBean(CurrentAccountService.class);
                assertTrue(accounts.findById(999L).isEmpty());
                statement.executeUpdate("""
                        INSERT INTO public.users (id,email,password_hash,full_name,role,email_verified,locked,created_at,updated_at)
                        VALUES (9007199254740993,'account@example.invalid','test-only-not-a-password','Test User','STUDENT',false,false,now(),now())
                        """);
                var initial = accounts.findById(9007199254740993L).orElseThrow();
                assertEquals(9007199254740993L, initial.id());
                assertEquals(Role.STUDENT, initial.role());
                assertFalse(initial.locked());
                assertFalse(initial.emailVerified());
                statement.executeUpdate("UPDATE public.users SET role='TEACHER',email_verified=true WHERE id=9007199254740993");
                var teacher = accounts.findById(initial.id()).orElseThrow();
                assertEquals(Role.TEACHER, teacher.role());
                assertTrue(teacher.emailVerified());
                statement.executeUpdate("UPDATE public.users SET role='ADMIN',locked=true WHERE id=9007199254740993");
                var admin = accounts.findById(initial.id()).orElseThrow();
                assertEquals(Role.ADMIN, admin.role());
                assertTrue(admin.locked());
                assertEquals(Role.STUDENT, initial.role(), "Snapshots must remain immutable");
                try (var rows = statement.executeQuery("""
                        SELECT count(*), min(password_hash), min(full_name), bool_and(created_at=updated_at)
                        FROM public.users
                        """)) {
                    assertTrue(rows.next());
                    assertEquals(1, rows.getInt(1));
                    assertEquals("test-only-not-a-password", rows.getString(2));
                    assertEquals("Test User", rows.getString(3));
                    assertTrue(rows.getBoolean(4), "Lookup must not update timestamps");
                }
                try (var history = statement.executeQuery("SELECT count(*) FROM public.flyway_schema_history WHERE success")) {
                    assertTrue(history.next());
                    assertEquals(2, history.getInt(1));
                }
            }
        }
    }
}
