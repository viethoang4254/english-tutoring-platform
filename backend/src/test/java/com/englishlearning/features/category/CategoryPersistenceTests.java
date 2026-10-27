package com.englishlearning.features.category;
import com.englishlearning.EnglishLearningApplication;
import com.englishlearning.features.category.service.*;
import com.englishlearning.features.category.repository.CategoryRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
@EnabledIfSystemProperty(named="categoryPersistence",matches="true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CategoryPersistenceTests {
 String url,user,password;ConfigurableApplicationContext context;CategoryService service;long admin,other;
 Connection connect()throws Exception{return DriverManager.getConnection(url,user,password);}
 void sql(String sql)throws Exception{try(var c=connect();var st=c.createStatement()){st.execute(sql);}}
 long count(String sql)throws Exception{try(var c=connect();var st=c.createStatement();var r=st.executeQuery(sql)){r.next();return r.getLong(1);}}
 @BeforeAll void start()throws Exception{
  url=System.getenv("CATEGORY_TEST_URL");user=System.getenv("CATEGORY_TEST_USER");password=System.getenv("CATEGORY_TEST_PASSWORD");
  assertEquals("jdbc:postgresql://127.0.0.1:55436/task034_categories",url);assertEquals("task034_test",user);
  assertEquals(0,count("SELECT count(*) FROM pg_tables WHERE schemaname NOT IN ('pg_catalog','information_schema')"));
  Flyway.configure().dataSource(url,user,password).target("5").cleanDisabled(true).load().migrate();
  sql("INSERT INTO course_categories(name,active,created_at,updated_at) VALUES ('Legacy',false,now(),now())");
  Flyway.configure().dataSource(url,user,password).cleanDisabled(true).load().migrate();
  assertEquals(1,count("SELECT count(*) FROM course_categories WHERE name='Legacy' AND NOT active"));
  assertEquals(6,count("SELECT count(*) FROM flyway_schema_history WHERE success"));
  var props=new HashMap<String,Object>();props.put("spring.datasource.url",url);props.put("spring.datasource.username",user);props.put("spring.datasource.password",password);props.put("spring.flyway.enabled",false);props.put("spring.jpa.hibernate.ddl-auto","validate");props.put("spring.sql.init.mode","never");props.put("spring.datasource.hikari.maximum-pool-size",4);props.put("spring.datasource.hikari.connection-init-sql","SET lock_timeout TO '5s'");props.put("spring.jpa.open-in-view",false);
  byte[] key=new byte[32];new java.security.SecureRandom().nextBytes(key);props.put("app.security.password-reset.hmac-secret",Base64.getEncoder().encodeToString(key));
  context=new SpringApplicationBuilder(EnglishLearningApplication.class).web(WebApplicationType.NONE).initializers(c->c.getEnvironment().getPropertySources().addFirst(new MapPropertySource("isolated-category-test",props))).run();
  service=context.getBean(CategoryService.class);
  sql("INSERT INTO users(email,password_hash,full_name,role,email_verified,locked,created_at,updated_at) VALUES ('admin@test.invalid','unused','Admin','ADMIN',true,false,now(),now()),('other@test.invalid','unused','Admin','ADMIN',true,false,now(),now()),('teacher@test.invalid','unused','Teacher','TEACHER',true,false,now(),now())");
  admin=count("SELECT id FROM users WHERE email='admin@test.invalid'");other=count("SELECT id FROM users WHERE email='other@test.invalid'");
 }
 @AfterAll void close(){if(context!=null)context.close();}
 @BeforeEach void reset()throws Exception{sql("DELETE FROM courses");sql("DELETE FROM course_categories");}
 @Test void createFilterPaginationAndPartialUpdate(){var first=service.create(admin,Map.of("name","  École  "));assertEquals("École",first.name());assertTrue(first.active());service.create(admin,Map.of("name","Ecole","active",false));var page=service.list(admin,0,1,null);assertTrue(page.hasNext());assertEquals(first.id(),page.items().get(0).id());assertFalse(service.list(admin,1,1,null).hasNext());assertEquals(1,service.list(admin,0,20,false).items().size());var changed=service.update(admin,first.id().value(),Map.of("active",false));assertEquals(first.name(),changed.name());assertEquals(first.createdAt(),changed.createdAt());assertFalse(changed.active());}
 @Test void duplicateAndInactiveNamesReserveIdentity(){service.create(admin,Map.of("name","English","active",false));assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->service.create(other,Map.of("name"," ENGLISH ")));assertEquals(1,service.list(admin,0,20,null).items().size());}
 @Test void failedRenameRollsBackAllFields(){var a=service.create(admin,Map.of("name","A"));service.create(admin,Map.of("name","B"));assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->service.update(admin,a.id().value(),Map.of("name","b","active",false)));var row=service.list(admin,0,20,null).items().get(0);assertEquals("A",row.name());assertTrue(row.active());assertEquals(a.updatedAt(),row.updatedAt());}
 @Test void concurrentCreatesHaveOneWinner()throws Exception{var ready=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);try{var a=pool.submit(()->create(ready,admin,"Race"));var b=pool.submit(()->create(ready,other,"race"));ready.countDown();assertNotEquals(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));assertEquals(1,service.list(admin,0,20,null).items().size());}finally{pool.shutdownNow();}}
 boolean create(CountDownLatch ready,long actor,String name)throws Exception{ready.await();try{service.create(actor,Map.of("name",name));return true;}catch(org.springframework.dao.DataIntegrityViolationException e){return false;}}
 @Test void referencedCategoryRemainsAndPartialUpdatesSerialize()throws Exception{
  var cat=service.create(admin,Map.of("name","Referenced"));
  sql("INSERT INTO courses(teacher_id,category_id,name,description,tuition,currency,min_students,max_students,planned_start_date,timezone,session_count,status,created_at,updated_at) SELECT id,"+cat.id().value()+",'Fixture','Fixture',0,'VND',1,5,current_date,'Asia/Ho_Chi_Minh',1,'DRAFT',now(),now() FROM users WHERE role='TEACHER'");
  var ready=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);try{var a=pool.submit(()->{ready.await();return service.update(admin,cat.id().value(),Map.of("name","Renamed"));});var b=pool.submit(()->{ready.await();return service.update(other,cat.id().value(),Map.of("active",false));});ready.countDown();a.get(15,TimeUnit.SECONDS);b.get(15,TimeUnit.SECONDS);}finally{pool.shutdownNow();}
  var result=service.list(admin,0,20,null).items().get(0);assertEquals("Renamed",result.name());assertFalse(result.active());assertEquals(1,count("SELECT count(*) FROM courses WHERE category_id="+cat.id().value()));
 }
 @Test void missingAndInvalidUpdatesDoNotWrite(){var cat=service.create(admin,Map.of("name","Keep"));assertThrows(CategoryFailure.class,()->service.update(admin,Long.MAX_VALUE,Map.of("active",false)));assertThrows(CategoryFailure.class,()->service.update(admin,cat.id().value(),Collections.singletonMap("active",null)));assertTrue(service.list(admin,0,20,null).items().get(0).active());}
}
