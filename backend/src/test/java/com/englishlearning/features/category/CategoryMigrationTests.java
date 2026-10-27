package com.englishlearning.features.category;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.flywaydb.core.Flyway;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;
@EnabledIfSystemProperty(named="categoryPersistence",matches="true")
class CategoryMigrationTests {
 String user=System.getenv("CATEGORY_TEST_USER"),password=System.getenv("CATEGORY_TEST_PASSWORD");
 String target(String db)throws Exception{assertEquals("task034_test",user);assertEquals("jdbc:postgresql://127.0.0.1:55436/task034_categories",System.getenv("CATEGORY_TEST_URL"));String url="jdbc:postgresql://127.0.0.1:55436/"+db;try(var c=DriverManager.getConnection(url,user,password);var s=c.createStatement();var r=s.executeQuery("SELECT count(*) FROM pg_tables WHERE schemaname NOT IN ('pg_catalog','information_schema')")){r.next();assertEquals(0,r.getInt(1));}return url;}
 @Test void freshSchemaAppliesAllSixMigrations()throws Exception{String url=target("task034_migration_clean");var f=Flyway.configure().dataSource(url,user,password).cleanDisabled(true).load();assertEquals(6,f.migrate().migrationsExecuted);f.validate();}
 @Test void conflictingLegacyNamesFailWithoutDataChanges()throws Exception{
  String url=target("task034_migration_conflict");Flyway.configure().dataSource(url,user,password).target("5").cleanDisabled(true).load().migrate();
  try(var c=DriverManager.getConnection(url,user,password);var s=c.createStatement()){s.execute("INSERT INTO course_categories(name,active,created_at,updated_at) VALUES ('Test',true,now(),now()),(' test ',false,now(),now())");}
  assertThrows(org.flywaydb.core.api.FlywayException.class,()->Flyway.configure().dataSource(url,user,password).cleanDisabled(true).load().migrate());
  try(var c=DriverManager.getConnection(url,user,password);var s=c.createStatement();var r=s.executeQuery("SELECT (SELECT count(*) FROM course_categories),(SELECT count(*) FROM flyway_schema_history WHERE success)")){r.next();assertEquals(2,r.getInt(1));assertEquals(5,r.getInt(2));}
 }
}
