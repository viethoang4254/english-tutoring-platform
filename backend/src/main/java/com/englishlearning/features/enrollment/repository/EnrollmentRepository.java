package com.englishlearning.features.enrollment.repository;

import com.englishlearning.common.api.ApiId;
import com.englishlearning.features.enrollment.dto.EnrollmentDto;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class EnrollmentRepository {
    private final EntityManager em;
    public EnrollmentRepository(EntityManager em) { this.em = em; }
    public record Course(long teacherId, String status, BigDecimal tuition, int capacity) {}
    public record RosterRow(ApiId enrollmentId, ApiId studentId, String studentDisplayName,
                            String status, Instant enrolledAt) {}
    public Optional<Course> course(long id, boolean lock) {
        var rows = em.createNativeQuery("SELECT teacher_id,status,tuition,max_students FROM public.courses WHERE id=:id" + (lock ? " FOR UPDATE" : ""))
                .setParameter("id", id).getResultList();
        if (rows.isEmpty()) return Optional.empty();
        Object[] r = (Object[]) rows.get(0);
        return Optional.of(new Course(((Number)r[0]).longValue(), (String)r[1], (BigDecimal)r[2], ((Number)r[3]).intValue()));
    }
    public Instant firstStart(long course) {
        return instant(em.createNativeQuery("SELECT min(scheduled_start_at) FROM public.sessions WHERE course_id=:course")
                .setParameter("course", course).getSingleResult());
    }
    public long occupied(long course, Instant now) {
        return ((Number)em.createNativeQuery("SELECT count(*) FROM public.enrollments WHERE course_id=:course AND (status='ACTIVE' OR (status='PENDING' AND seat_reserved_until>:now))")
                .setParameter("course",course).setParameter("now",now).getSingleResult()).longValue();
    }
    private static final String SELECT = "SELECT e.id,e.course_id,c.name,e.status,e.enrolled_at,e.seat_reserved_until,e.cancelled_at,e.cancellation_reason,e.created_at,e.updated_at FROM public.enrollments e JOIN public.courses c ON c.id=e.course_id WHERE e.student_id=:actor";
    public Optional<EnrollmentDto> own(long actor,long id,Instant now) {
        var rows=em.createNativeQuery(SELECT+" AND e.id=:id").setParameter("actor",actor).setParameter("id",id).getResultList();
        return rows.isEmpty()?Optional.empty():Optional.of(dto(rows.get(0),now));
    }
    public Optional<EnrollmentDto> forCourse(long actor,long course,Instant now) {
        var rows=em.createNativeQuery(SELECT+" AND e.course_id=:course FOR UPDATE OF e").setParameter("actor",actor).setParameter("course",course).getResultList();
        return rows.isEmpty()?Optional.empty():Optional.of(dto(rows.get(0),now));
    }
    public List<EnrollmentDto> list(long actor,int page,int size,String status,Instant now) {
        var q=em.createNativeQuery(SELECT+(status==null?"":" AND e.status=:status")+" ORDER BY e.id DESC").setParameter("actor",actor);
        if(status!=null) q.setParameter("status",status);
        return ((List<?>)q.setFirstResult(Math.multiplyExact(page,size)).setMaxResults(size+1).getResultList()).stream().map(r->dto(r,now)).toList();
    }
    public List<RosterRow> roster(long actor,long course,int page,int size,String status) {
        var q=em.createNativeQuery("SELECT e.id,e.student_id,u.full_name,e.status,e.enrolled_at FROM public.enrollments e JOIN public.courses c ON c.id=e.course_id JOIN public.users u ON u.id=e.student_id WHERE c.teacher_id=:actor AND c.id=:course"+(status==null?"":" AND e.status=:status")+" ORDER BY e.id DESC")
                .setParameter("actor",actor).setParameter("course",course);
        if(status!=null) q.setParameter("status",status);
        return ((List<?>)q.setFirstResult(Math.multiplyExact(page,size)).setMaxResults(size+1).getResultList()).stream().map(value->{Object[] r=(Object[])value;return new RosterRow(new ApiId(((Number)r[0]).longValue()),new ApiId(((Number)r[1]).longValue()),(String)r[2],(String)r[3],instant(r[4]));}).toList();
    }
    public long create(long actor,long course,Instant deadline,Instant now) {
        return ((Number)em.createNativeQuery("INSERT INTO public.enrollments(student_id,course_id,status,seat_reserved_until,created_at,updated_at) VALUES (:actor,:course,'PENDING',:deadline,:now,:now) RETURNING id")
                .setParameter("actor",actor).setParameter("course",course).setParameter("deadline",deadline).setParameter("now",now).getSingleResult()).longValue();
    }
    public void renew(long actor,long id,Instant deadline,Instant now) {
        int count=em.createNativeQuery("UPDATE public.enrollments SET seat_reserved_until=:deadline,updated_at=:now WHERE id=:id AND student_id=:actor AND status='PENDING'")
                .setParameter("actor",actor).setParameter("id",id).setParameter("deadline",deadline).setParameter("now",now).executeUpdate();
        if(count!=1) throw new IllegalStateException("Locked enrollment update failed.");
    }
    public void cancel(long actor,long id,Instant now) {
        int count=em.createNativeQuery("UPDATE public.enrollments SET status='CANCELLED',seat_reserved_until=NULL,cancelled_at=:now,updated_at=:now WHERE id=:id AND student_id=:actor AND status='PENDING'")
                .setParameter("actor",actor).setParameter("id",id).setParameter("now",now).executeUpdate();
        if(count!=1) throw new IllegalStateException("Locked enrollment cancellation failed.");
    }
    public boolean pendingPayment(long enrollment) {
        return (Boolean)em.createNativeQuery("SELECT EXISTS(SELECT 1 FROM public.payments WHERE enrollment_id=:id AND status='PENDING')").setParameter("id",enrollment).getSingleResult();
    }
    private EnrollmentDto dto(Object value,Instant now) {
        Object[] r=(Object[])value; Instant deadline=instant(r[5]);String status=(String)r[3];
        return new EnrollmentDto(new ApiId(((Number)r[0]).longValue()),new ApiId(((Number)r[1]).longValue()),(String)r[2],status,instant(r[4]),deadline,
                status.equals("PENDING")&&(deadline==null||!deadline.isAfter(now)),instant(r[6]),(String)r[7],instant(r[8]),instant(r[9]));
    }
    private static Instant instant(Object value) {
        if(value==null)return null;
        if(value instanceof Instant i)return i;
        if(value instanceof OffsetDateTime d)return d.toInstant();
        return ((java.sql.Timestamp)value).toInstant();
    }
}
