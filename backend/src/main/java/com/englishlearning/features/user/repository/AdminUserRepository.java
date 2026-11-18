package com.englishlearning.features.user.repository;
import com.englishlearning.features.user.dto.AdminUserDto;
import com.englishlearning.common.api.ApiId;
import jakarta.persistence.EntityManager;
import java.util.*;
import org.springframework.stereotype.Repository;
@Repository
public class AdminUserRepository {
 private final EntityManager em;
 public AdminUserRepository(EntityManager em){this.em=em;}
 private static final String COLUMNS="SELECT id,full_name,email,role,email_verified,locked FROM public.users ";
 public List<AdminUserDto> list(int page,int size,String search,String role,Boolean locked){
  var q=em.createNativeQuery(COLUMNS+"WHERE (strpos(lower(full_name),:search)>0 OR strpos(lower(email),:search)>0) "+(role==null?"":"AND role=:role ")+(locked==null?"":"AND locked=:locked ")+"ORDER BY id").setParameter("search",search.toLowerCase(Locale.ROOT));
  if(role!=null)q.setParameter("role",role);
  if(locked!=null)q.setParameter("locked",locked);
  return q.setFirstResult(page*size).setMaxResults(size+1).getResultList().stream().map(row->dto((Object[])row)).toList();
 }
 public Optional<AdminUserDto> find(long id){
  List<?> rows=em.createNativeQuery(COLUMNS+"WHERE id=:id").setParameter("id",id).getResultList();
  return rows.isEmpty()?Optional.empty():Optional.of(dto((Object[])rows.get(0)));
 }
 public void lockParticipants(long actor, long target) {
  em.createNativeQuery("SELECT id FROM public.users WHERE id IN (:actor,:target) ORDER BY id FOR UPDATE")
      .setParameter("actor",actor).setParameter("target",target).getResultList();
 }
 public void setLocked(long id, boolean locked) {
  em.createNativeQuery("UPDATE public.users SET locked=:locked, updated_at=now() WHERE id=:id")
      .setParameter("locked",locked).setParameter("id",id).executeUpdate();
  if (locked) {
   em.createNativeQuery("UPDATE public.refresh_sessions SET revoked_at=now() WHERE user_id=:id AND revoked_at IS NULL")
       .setParameter("id",id).executeUpdate();
  }
 }
 public void updateName(long id,String name) {
  em.createNativeQuery("UPDATE public.users SET full_name=:name,updated_at=now() WHERE id=:id").setParameter("name",name).setParameter("id",id).executeUpdate();
 }
 public boolean hasBusinessDependencies(long id) {
  // User row is locked by the service. FK inserts need a conflicting key-share lock.
  List<?> constraints=em.createNativeQuery("""
   SELECT n.nspname,t.relname,a.attname,cardinality(c.conkey),cardinality(c.confkey),ra.attname,
          quote_ident(n.nspname)||'.'||quote_ident(t.relname),quote_ident(a.attname)
   FROM pg_constraint c JOIN pg_class t ON t.oid=c.conrelid JOIN pg_namespace n ON n.oid=t.relnamespace
   JOIN pg_attribute a ON a.attrelid=c.conrelid AND a.attnum=c.conkey[1]
   JOIN pg_attribute ra ON ra.attrelid=c.confrelid AND ra.attnum=c.confkey[1]
   WHERE c.contype='f' AND c.confrelid='public.users'::regclass
   """).getResultList();
  if(constraints.isEmpty())return true;
  // The durable outbox deliberately has no FK; pending user work is not disposable.
  if((Boolean)em.createNativeQuery("SELECT EXISTS(SELECT 1 FROM public.calendar_sync_changes WHERE entity IN ('user','connection') AND entity_id=:id)").setParameter("id",id).getSingleResult())return true;
  // No unreviewed cascade may be reached through the disposable auth tables.
  if(((Number)em.createNativeQuery("SELECT count(*) FROM pg_constraint WHERE contype='f' AND confrelid IN ('public.refresh_sessions'::regclass,'public.email_verification_credentials'::regclass,'public.password_reset_credentials'::regclass)").getSingleResult()).longValue()!=0)return true;
  Set<String> disposable=Set.of("refresh_sessions","email_verification_credentials","password_reset_credentials");
  for(Object value:constraints) {
   Object[] r=(Object[])value;
   if(((Number)r[3]).intValue()!=1||((Number)r[4]).intValue()!=1||!"id".equals(r[5]))return true;
   if("public".equals(r[0])&&disposable.contains(r[1])&&"user_id".equals(r[2]))continue;
   // Identifiers are quoted by PostgreSQL, never supplied by the request.
   if((Boolean)em.createNativeQuery("SELECT EXISTS(SELECT 1 FROM "+r[6]+" WHERE "+r[7]+"=:id)").setParameter("id",id).getSingleResult())return true;
  }
  return false;
 }
 public void deleteDisposable(long id) {
  for(String table:List.of("refresh_sessions","email_verification_credentials","password_reset_credentials"))
   em.createNativeQuery("DELETE FROM public."+table+" WHERE user_id=:id").setParameter("id",id).executeUpdate();
  if(em.createNativeQuery("DELETE FROM public.users WHERE id=:id").setParameter("id",id).executeUpdate()!=1)throw new IllegalStateException("Deletion not confirmed");
 }
 public com.englishlearning.features.user.dto.AdminAnalyticsDto analytics(String range, java.time.Instant from, java.time.Instant now) {
  Object[] r=(Object[])em.createNativeQuery("""
   SELECT (SELECT count(*) FROM public.users),
     (SELECT count(*) FROM public.users WHERE role='STUDENT'),
     (SELECT count(*) FROM public.users WHERE role='TEACHER'),
     (SELECT count(*) FROM public.users u WHERE role='TEACHER' AND EXISTS
       (SELECT 1 FROM public.teacher_applications a WHERE a.teacher_id=u.id AND a.status='APPROVED')),
     (SELECT count(*) FROM public.teacher_applications WHERE status='PENDING'),
     (SELECT count(*) FROM public.courses),
     (SELECT count(*) FROM public.courses WHERE status='PUBLISHED'),
     (SELECT count(*) FROM public.courses WHERE status='DRAFT'),
     (SELECT count(*) FROM public.enrollments),
     count(*),count(*) FILTER(WHERE status='CONFIRMED'),count(*) FILTER(WHERE status='PENDING'),
     count(*) FILTER(WHERE status='EXPIRED'),count(*) FILTER(WHERE status='CANCELLED'),
     coalesce(sum(final_amount) FILTER(WHERE status='CONFIRMED'),0)
   FROM public.payments
   """).getSingleResult();
  var totals=new com.englishlearning.features.user.dto.AdminAnalyticsDto.Totals(n(r[0]),n(r[1]),n(r[2]),n(r[3]),n(r[4]),n(r[5]),n(r[6]),n(r[7]),n(r[8]),n(r[9]),n(r[10]),n(r[11]),n(r[12]),n(r[13]),money(r[14]));
  // Aggregate each event once before combining; never join payment attempts to application history.
  String activity="""
   SELECT created_at AS happened,1 AS users,0 AS enrollments,0 AS courses,0 AS payments,0::numeric AS tuition FROM public.users
   UNION ALL SELECT created_at,0,1,0,0,0::numeric FROM public.enrollments
   UNION ALL SELECT created_at,0,0,1,0,0::numeric FROM public.courses
   UNION ALL SELECT confirmed_at,0,0,0,1,final_amount FROM public.payments WHERE status='CONFIRMED'
   """;
  String boundary=" WHERE happened < :until"+(from==null?"":" AND happened >= :since");
  var periodQuery=em.createNativeQuery("SELECT coalesce(sum(users),0),coalesce(sum(enrollments),0),coalesce(sum(courses),0),coalesce(sum(payments),0),coalesce(sum(tuition),0) FROM ("+activity+") activity"+boundary);
  window(periodQuery,from,now);
  Object[] p=(Object[])periodQuery.getSingleResult();
  var period=new com.englishlearning.features.user.dto.AdminAnalyticsDto.Activity(n(p[0]),n(p[1]),n(p[2]),n(p[3]),money(p[4]));
  List<com.englishlearning.features.user.dto.AdminAnalyticsDto.Day> days=new ArrayList<>();
  // All-time totals remain exact; daily output is only offered for the bounded 7/30/90-day ranges.
  if(from!=null) {
   var q=em.createNativeQuery("SELECT cast(happened AT TIME ZONE 'Asia/Ho_Chi_Minh' AS date),sum(users),sum(enrollments),sum(courses),sum(payments),sum(tuition) FROM ("+activity+") activity"+boundary+" GROUP BY 1 ORDER BY 1");
   window(q,from,now);
   for(Object value:q.getResultList()) {Object[] d=(Object[])value;days.add(new com.englishlearning.features.user.dto.AdminAnalyticsDto.Day(d[0].toString(),n(d[1]),n(d[2]),n(d[3]),n(d[4]),money(d[5])));}
  }
  var q=em.createNativeQuery("""
   SELECT c.id,c.name,u.id,u.full_name,count(*),sum(p.final_amount)
   FROM public.payments p JOIN public.enrollments e ON e.id=p.enrollment_id
   JOIN public.courses c ON c.id=e.course_id JOIN public.users u ON u.id=c.teacher_id
   WHERE p.status='CONFIRMED' AND p.confirmed_at < :until
   """+(from==null?"":" AND p.confirmed_at >= :since")+" GROUP BY c.id,c.name,u.id,u.full_name ORDER BY sum(p.final_amount) DESC,c.id");
  window(q,from,now);
  List<?> rows=q.setMaxResults(21).getResultList();
  var courses=rows.stream().limit(20).map(value->{Object[] c=(Object[])value;return new com.englishlearning.features.user.dto.AdminAnalyticsDto.CourseTuition(new ApiId(n(c[0])),(String)c[1],new ApiId(n(c[2])),(String)c[3],n(c[4]),money(c[5]));}).toList();
  var teacherQuery=em.createNativeQuery("""
   SELECT u.id,u.full_name,count(*),sum(p.final_amount)
   FROM public.payments p JOIN public.enrollments e ON e.id=p.enrollment_id
   JOIN public.courses c ON c.id=e.course_id JOIN public.users u ON u.id=c.teacher_id
   WHERE p.status='CONFIRMED' AND p.confirmed_at < :until
   """+(from==null?"":" AND p.confirmed_at >= :since")+" GROUP BY u.id,u.full_name ORDER BY sum(p.final_amount) DESC,u.id");
  window(teacherQuery,from,now);
  List<?> teacherRows=teacherQuery.setMaxResults(21).getResultList();
  var teachers=teacherRows.stream().limit(20).map(value->{Object[] t=(Object[])value;return new com.englishlearning.features.user.dto.AdminAnalyticsDto.TeacherTuition(new ApiId(n(t[0])),(String)t[1],n(t[2]),money(t[3]));}).toList();
  return new com.englishlearning.features.user.dto.AdminAnalyticsDto(range,"Asia/Ho_Chi_Minh",from,now,totals,period,days,courses,rows.size()>20,teachers,teacherRows.size()>20);
 }
 private static long n(Object value){return ((Number)value).longValue();}
 private static com.englishlearning.common.api.ApiMoney money(Object value){return com.englishlearning.common.api.ApiMoney.from(((java.math.BigDecimal)value).setScale(2),"VND");}
 private static void window(jakarta.persistence.Query query,java.time.Instant from,java.time.Instant now){
  query.setParameter("until",now);if(from!=null)query.setParameter("since",from);
 }
 private static AdminUserDto dto(Object[] r){return new AdminUserDto(new ApiId(((Number)r[0]).longValue()),(String)r[1],(String)r[2],(String)r[3],(Boolean)r[4],(Boolean)r[5]);}
}
