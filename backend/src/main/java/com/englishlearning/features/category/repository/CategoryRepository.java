package com.englishlearning.features.category.repository;
import com.englishlearning.features.category.dto.CategoryDto;
import com.englishlearning.common.api.ApiId;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.*;
@Repository
public class CategoryRepository {
 private final EntityManager em;
 public CategoryRepository(EntityManager em){this.em=em;}
 private CategoryDto dto(Object value){Object[] r=(Object[])value;return new CategoryDto(new ApiId(((Number)r[0]).longValue()),(String)r[1],(Boolean)r[2],instant(r[3]),instant(r[4]));}
 private Instant instant(Object v){return v instanceof Instant i?i:v instanceof java.time.OffsetDateTime o?o.toInstant():((java.sql.Timestamp)v).toInstant();}
 public List<CategoryDto> list(int page,int size,Boolean active){
  var q=em.createNativeQuery("SELECT id,name,active,created_at,updated_at FROM public.course_categories"+(active==null?"":" WHERE active=:active")+" ORDER BY id ASC");
  if(active!=null)q.setParameter("active",active);
  return ((List<?>)q.setFirstResult(Math.multiplyExact(page,size)).setMaxResults(size+1).getResultList()).stream().map(this::dto).toList();
 }
 public Optional<CategoryDto> find(long id,boolean lock){
  var rows=em.createNativeQuery("SELECT id,name,active,created_at,updated_at FROM public.course_categories WHERE id=:id"+(lock?" FOR UPDATE":"")).setParameter("id",id).getResultList();
  return rows.isEmpty()?Optional.empty():Optional.of(dto(rows.get(0)));
 }
 public CategoryDto create(String name,boolean active,Instant now){
  Object row=em.createNativeQuery("INSERT INTO public.course_categories(name,active,created_at,updated_at) VALUES (:name,:active,:now,:now) RETURNING id,name,active,created_at,updated_at").setParameter("name",name).setParameter("active",active).setParameter("now",now).getSingleResult();return dto(row);
 }
 public CategoryDto update(long id,String name,boolean active,Instant now){
  em.createNativeQuery("UPDATE public.course_categories SET name=:name,active=:active,updated_at=:now WHERE id=:id").setParameter("name",name).setParameter("active",active).setParameter("now",now).setParameter("id",id).executeUpdate();return find(id,false).orElseThrow();
 }
}
