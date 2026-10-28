package com.englishlearning.features.category;

import com.englishlearning.EnglishLearningApplication;
import com.englishlearning.features.auth.security.AccessTokenIssuer;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.security.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="categoryLive", matches="true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CategoryLiveHttpTests {
    String url, user, password, base;
    ConfigurableApplicationContext context;
    Path keyDirectory;
    final HttpClient http = HttpClient.newHttpClient();
    final JsonMapper json = JsonMapper.builder().build();
    final Map<String, String> tokens = new HashMap<>();
    String category;
    Connection connect() throws Exception { return DriverManager.getConnection(url,user,password); }
    long scalar(String query) throws Exception {
        try(var c=connect(); var s=c.createStatement(); var r=s.executeQuery(query)){r.next();return r.getLong(1);}
    }
    void sql(String query) throws Exception { try(var c=connect();var s=c.createStatement()){s.execute(query);} }
    @BeforeAll void start() throws Exception {
        url=System.getenv("CATEGORY_LIVE_URL");user=System.getenv("CATEGORY_TEST_USER");password=System.getenv("CATEGORY_TEST_PASSWORD");
        assertEquals("jdbc:postgresql://127.0.0.1:55438/task034_live",url);
        assertEquals("task034_test",user);
        assertEquals(0,scalar("SELECT count(*) FROM pg_tables WHERE schemaname NOT IN ('pg_catalog','information_schema')"));
        var flyway=Flyway.configure().dataSource(url,user,password).cleanDisabled(true).load();
        assertEquals(6,flyway.migrate().migrationsExecuted); flyway.validate();
        assertEquals(6,scalar("SELECT count(*) FROM flyway_schema_history WHERE success"));
        keyDirectory=Files.createTempDirectory("task034-jwt-");
        var generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);var pair=generator.generateKeyPair();
        Files.writeString(keyDirectory.resolve("private.pem"),pem("PRIVATE KEY",pair.getPrivate().getEncoded()));
        Files.writeString(keyDirectory.resolve("public.pem"),pem("PUBLIC KEY",pair.getPublic().getEncoded()));
        var properties=new HashMap<String,Object>();
        properties.put("spring.datasource.url",url);properties.put("spring.datasource.username",user);properties.put("spring.datasource.password",password);
        properties.put("spring.flyway.enabled",false);properties.put("spring.jpa.hibernate.ddl-auto","none");properties.put("spring.sql.init.mode","never");
        properties.put("spring.datasource.hikari.maximum-pool-size",3);
        properties.put("spring.datasource.hikari.connection-init-sql","SET lock_timeout TO '5s'");
        properties.put("spring.mail.host","127.0.0.1");properties.put("spring.mail.port",1);
        properties.put("spring.mail.username","unused");properties.put("spring.mail.password","unused");
        properties.put("server.address","127.0.0.1");properties.put("server.port",0);
        properties.put("app.security.jwt.private-key",keyDirectory.resolve("private.pem").toUri().toString());
        properties.put("app.security.jwt.public-key",keyDirectory.resolve("public.pem").toUri().toString());
        byte[] key=new byte[32];new SecureRandom().nextBytes(key);
        properties.put("app.security.password-reset.hmac-secret",Base64.getEncoder().encodeToString(key));
        properties.put("app.security.browser.local-http",true);
        properties.put("app.security.browser.allowed-origins","http://localhost:3012");
        context=new SpringApplicationBuilder(EnglishLearningApplication.class)
            .initializers(c->c.getEnvironment().getPropertySources().addFirst(new MapPropertySource("isolated-live",properties))).run();
        base="http://127.0.0.1:"+((WebServerApplicationContext)context).getWebServer().getPort()+"/api/v1";
        for(String role:List.of("ADMIN","TEACHER","STUDENT")){
            sql("INSERT INTO users(email,password_hash,full_name,role,email_verified,locked,created_at,updated_at) VALUES ('"+role.toLowerCase()+"@example.invalid','unused','Fixture','"+role+"',true,false,now(),now())");
            long id=scalar("SELECT id FROM users WHERE role='"+role+"'");
            tokens.put(role,context.getBean(AccessTokenIssuer.class).issue(id).value());
        }
        sql("INSERT INTO teacher_applications(teacher_id,specialization,teaching_experience,introduction,status,submitted_at,reviewed_at,reviewed_by,created_at,updated_at) SELECT id,'Fixture','Fixture','Fixture','APPROVED',now(),now(),(SELECT id FROM users WHERE role='ADMIN'),now(),now() FROM users WHERE role='TEACHER'");
    }
    String pem(String type,byte[] bytes){return "-----BEGIN "+type+"-----\n"+Base64.getMimeEncoder(64,new byte[]{10}).encodeToString(bytes)+"\n-----END "+type+"-----\n";}
    HttpResponse<String> request(String role,String method,String path,String body) throws Exception {
        var b=HttpRequest.newBuilder(URI.create(base+path)).timeout(java.time.Duration.ofSeconds(15));
        if(role!=null)b.header("Authorization","Bearer "+tokens.get(role));
        if(body!=null)b.header("Content-Type","application/json");
        return http.send(b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    @Test @Order(1) void adminCrudAndTeacherFiltering() throws Exception {
        var created=request("ADMIN","POST","/admin/categories","{\"name\":\"  École  \"}");
        assertEquals(201,created.statusCode());category=json.readTree(created.body()).get("id").asString();
        assertTrue(category.matches("[1-9][0-9]*"));
        assertEquals("École",json.readTree(created.body()).get("name").asString());
        assertEquals(201,request("ADMIN","POST","/admin/categories","{\"name\":\"Hidden\",\"active\":false}").statusCode());
        var page=request("TEACHER","GET","/teacher/categories?size=1",null);
        assertEquals(200,page.statusCode());assertEquals("no-store",page.headers().firstValue("Cache-Control").orElse(""));
        assertEquals(1,json.readTree(page.body()).get("items").size());
        assertEquals(category,json.readTree(page.body()).get("items").get(0).get("id").asString());
        assertFalse(json.readTree(page.body()).get("hasNext").asBoolean());
        assertEquals(200,request("ADMIN","PATCH","/admin/categories/"+category,"{\"name\":\"Renamed\",\"active\":false}").statusCode());
        assertEquals(0,json.readTree(request("TEACHER","GET","/teacher/categories",null).body()).get("items").size());
        assertEquals(200,request("ADMIN","PATCH","/admin/categories/"+category,"{\"active\":true}").statusCode());
        assertEquals(1,json.readTree(request("TEACHER","GET","/teacher/categories",null).body()).get("items").size());
        assertEquals(1,json.readTree(request("ADMIN","GET","/admin/categories?active=false",null).body()).get("items").size());
    }
    @Test @Order(2) void authorizationValidationAndEligibility() throws Exception {
        assertEquals(401,request(null,"GET","/teacher/categories",null).statusCode());
        for(String role:List.of("ADMIN","STUDENT"))assertEquals(403,request(role,"GET","/teacher/categories",null).statusCode());
        assertEquals(403,request("TEACHER","POST","/admin/categories","{\"name\":\"Denied\"}").statusCode());
        assertEquals(403,request("TEACHER","DELETE","/teacher/categories/1",null).statusCode());
        assertEquals(400,request("TEACHER","GET","/teacher/categories?active=false",null).statusCode());
        assertEquals(400,request("TEACHER","GET","/teacher/categories?size=101",null).statusCode());
        assertEquals(400,request("ADMIN","PATCH","/admin/categories/"+category,"{\"unknown\":true}").statusCode());
        var missing=request("ADMIN","PATCH","/admin/categories/9223372036854775807","{\"active\":false}");
        assertEquals(404,missing.statusCode());assertEquals("CATEGORY_NOT_FOUND",json.readTree(missing.body()).get("code").asString());
        sql("UPDATE users SET locked=true WHERE role='TEACHER'");
        assertEquals(401,request("TEACHER","GET","/teacher/categories",null).statusCode());
        sql("UPDATE users SET locked=false,email_verified=false WHERE role='TEACHER'");
        assertEquals(403,request("TEACHER","GET","/teacher/categories",null).statusCode());
        sql("UPDATE users SET email_verified=true WHERE role='TEACHER'");
        sql("UPDATE teacher_applications SET status='REJECTED',rejection_reason='Fixture'");
        assertEquals(403,request("TEACHER","GET","/teacher/categories",null).statusCode());
        sql("UPDATE teacher_applications SET status='APPROVED',rejection_reason=null");
    }
    @Test @Order(3) void uniquenessConcurrencyAndPreservation() throws Exception {
        var conflict=request("ADMIN","POST","/admin/categories","{\"name\":\" hidden \"}");
        assertEquals(409,conflict.statusCode());assertEquals("CATEGORY_NAME_CONFLICT",json.readTree(conflict.body()).get("code").asString());
        var pool=Executors.newFixedThreadPool(2);
        try{
            var gate=new CountDownLatch(1);
            var a=pool.submit(()->{gate.await();return request("ADMIN","POST","/admin/categories","{\"name\":\"Race\"}").statusCode();});
            var b=pool.submit(()->{gate.await();return request("ADMIN","POST","/admin/categories","{\"name\":\"race\"}").statusCode();});
            gate.countDown();assertEquals(Set.of(201,409),Set.of(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS)));
        }finally{pool.shutdownNow();}
        assertEquals(3,scalar("SELECT count(*) FROM course_categories"));
        assertEquals(409,request("ADMIN","PATCH","/admin/categories/"+category,"{\"name\":\"HIDDEN\",\"active\":false}").statusCode());
        assertEquals(1,scalar("SELECT count(*) FROM course_categories WHERE id="+category+" AND name='Renamed' AND active"));
        var page=json.readTree(request("TEACHER","GET","/teacher/categories?size=1&page=0",null).body());
        assertTrue(page.get("hasNext").asBoolean());
        assertEquals(1,json.readTree(request("TEACHER","GET","/teacher/categories?size=1&page=1",null).body()).get("items").size());
        assertEquals(3,scalar("SELECT count(*) FROM users"));
    }
    @AfterAll void stop() throws Exception {
        if(context!=null)context.close();
        if(keyDirectory!=null){Files.deleteIfExists(keyDirectory.resolve("private.pem"));Files.deleteIfExists(keyDirectory.resolve("public.pem"));Files.deleteIfExists(keyDirectory);}
    }
}
