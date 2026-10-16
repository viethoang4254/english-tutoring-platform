package com.englishlearning.features.user.repository;
import com.englishlearning.features.user.dto.StudentProfile;
import com.englishlearning.common.api.ApiId;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.time.Instant;
@Repository
public class StudentProfileRepository {
    private final EntityManager em;
    public StudentProfileRepository(EntityManager em) { this.em=em; }
    public StudentProfile read(Long id) {
        Object[] row=(Object[])em.createNativeQuery("SELECT id,full_name,username,email,phone,email_verified FROM public.users WHERE id=:id").setParameter("id",id).getSingleResult();
        return new StudentProfile(new ApiId(((Number)row[0]).longValue()),(String)row[1],(String)row[2],(String)row[3],(String)row[4],(Boolean)row[5]);
    }
    public void update(Long id,String name,String phone,Instant now) {
        em.createNativeQuery("UPDATE public.users SET full_name=COALESCE(:name,full_name),phone=COALESCE(:phone,phone),updated_at=:now WHERE id=:id")
            .setParameter("name",name).setParameter("phone",phone).setParameter("now",now).setParameter("id",id).executeUpdate();
    }
}
