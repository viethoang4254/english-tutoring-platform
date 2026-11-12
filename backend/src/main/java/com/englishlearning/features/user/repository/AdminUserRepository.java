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
 public List<AdminUserDto> list(int page,int size,String search,String role){
  var q=em.createNativeQuery(COLUMNS+"WHERE (strpos(lower(full_name),:search)>0 OR strpos(lower(email),:search)>0) "+(role==null?"":"AND role=:role ")+"ORDER BY id").setParameter("search",search.toLowerCase(Locale.ROOT));
  if(role!=null)q.setParameter("role",role);
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
 private static AdminUserDto dto(Object[] r){return new AdminUserDto(new ApiId(((Number)r[0]).longValue()),(String)r[1],(String)r[2],(String)r[3],(Boolean)r[4],(Boolean)r[5]);}
}
