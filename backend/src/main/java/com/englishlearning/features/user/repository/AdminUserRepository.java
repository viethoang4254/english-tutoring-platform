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
 private static AdminUserDto dto(Object[] r){return new AdminUserDto(new ApiId(((Number)r[0]).longValue()),(String)r[1],(String)r[2],(String)r[3],(Boolean)r[4],(Boolean)r[5]);}
}
