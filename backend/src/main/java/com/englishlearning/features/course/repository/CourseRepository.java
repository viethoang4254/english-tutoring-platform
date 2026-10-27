package com.englishlearning.features.course.repository;

import com.englishlearning.common.api.*;
import com.englishlearning.features.course.dto.CourseDto;
import com.englishlearning.features.course.dto.SessionDto;
import com.englishlearning.features.course.dto.ScheduleChangeDto;
import com.englishlearning.features.course.service.RescheduleInput;
import com.englishlearning.features.course.service.SessionInput;
import com.englishlearning.features.course.service.CourseInput;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class CourseRepository {
    private final EntityManager em;
    public CourseRepository(EntityManager em) { this.em = em; }
    public List<com.englishlearning.features.course.dto.CalendarSessionDto> calendar(long actor,Instant from,Instant to,int page,int size) {
        var rows=em.createNativeQuery("""
            SELECT s.id,c.id,c.name,s.session_number,s.title,s.scheduled_start_at,s.scheduled_end_at,c.timezone,s.status
            FROM public.sessions s JOIN public.courses c ON c.id=s.course_id
            WHERE c.teacher_id=:actor AND s.scheduled_start_at<:to AND s.scheduled_end_at>:from
            ORDER BY s.scheduled_start_at,s.id
            """).setParameter("actor",actor).setParameter("from",from).setParameter("to",to)
            .setFirstResult(page*size).setMaxResults(size+1).getResultList();
        return ((List<Object[]>)rows).stream().map(r -> new com.englishlearning.features.course.dto.CalendarSessionDto(
            new ApiId(((Number)r[0]).longValue()),new ApiId(((Number)r[1]).longValue()),(String)r[2],
            ((Number)r[3]).intValue(),(String)r[4],instant(r[5]),instant(r[6]),(String)r[7],(String)r[8])).toList();
    }
    public boolean hasCalendarEvents(long session) {
        return (Boolean)em.createNativeQuery("SELECT EXISTS(SELECT 1 FROM public.session_calendar_events WHERE session_id=:id)").setParameter("id",session).getSingleResult();
    }
    public boolean overlaps(long course,long session,Instant start,Instant end) {
        return (Boolean)em.createNativeQuery("SELECT EXISTS(SELECT 1 FROM public.sessions WHERE course_id=:course AND id<>:id AND status='SCHEDULED' AND scheduled_start_at<:end AND scheduled_end_at>:start)")
                .setParameter("course",course).setParameter("id",session).setParameter("start",start).setParameter("end",end).getSingleResult();
    }
    public List<ScheduleChangeDto> history(long actor,long course,long session,int page,int size) {
        var rows=em.createNativeQuery("""
            SELECT h.id,h.session_id,h.old_start_at,h.old_end_at,h.new_start_at,h.new_end_at,h.changed_at
            FROM public.session_schedule_changes h JOIN public.sessions s ON s.id=h.session_id
            JOIN public.courses c ON c.id=s.course_id
            WHERE c.teacher_id=:actor AND s.course_id=:course AND s.id=:id ORDER BY h.id
            """).setParameter("actor",actor).setParameter("course",course).setParameter("id",session)
                .setFirstResult(Math.multiplyExact(page,size)).setMaxResults(size+1).getResultList();
        return ((List<?>)rows).stream().map(value->{Object[] r=(Object[])value;return new ScheduleChangeDto(
                new ApiId(((Number)r[0]).longValue()),new ApiId(((Number)r[1]).longValue()),instant(r[2]),instant(r[3]),instant(r[4]),instant(r[5]),instant(r[6]));}).toList();
    }
    public SessionDto reschedule(long actor,long course,SessionDto current,RescheduleInput input,Instant now) {
        int updated=em.createNativeQuery("""
            UPDATE public.sessions s SET scheduled_start_at=:start,scheduled_end_at=:end,updated_at=:now
            FROM public.courses c WHERE c.id=s.course_id AND c.teacher_id=:actor AND s.course_id=:course AND s.id=:id
            AND s.status='SCHEDULED' AND c.status IN ('DRAFT','PUBLISHED')
            """).setParameter("actor",actor).setParameter("course",course).setParameter("id",current.id().value())
                .setParameter("start",input.scheduledStartAt()).setParameter("end",input.scheduledEndAt()).setParameter("now",now).executeUpdate();
        if(updated!=1)throw new IllegalStateException("Locked Session reschedule failed.");
        em.createNativeQuery("""
            INSERT INTO public.session_schedule_changes(session_id,old_start_at,old_end_at,new_start_at,new_end_at,changed_at)
            VALUES (:id,:oldStart,:oldEnd,:newStart,:newEnd,:now)
            """).setParameter("id",current.id().value()).setParameter("oldStart",current.scheduledStartAt()).setParameter("oldEnd",current.scheduledEndAt())
                .setParameter("newStart",input.scheduledStartAt()).setParameter("newEnd",input.scheduledEndAt()).setParameter("now",now).executeUpdate();
        return session(actor,course,current.id().value(),false).orElseThrow();
    }
    public SessionDto cancel(long actor,long course,long session,String reason,Instant now) {
        var q=em.createNativeQuery("""
            UPDATE public.sessions s SET status='CANCELLED',cancellation_reason=:reason,updated_at=:now
            FROM public.courses c WHERE c.id=s.course_id AND c.teacher_id=:actor AND s.course_id=:course AND s.id=:id
            AND s.status='SCHEDULED' AND c.status IN ('DRAFT','PUBLISHED')
            """).setParameter("actor",actor).setParameter("course",course).setParameter("id",session).setParameter("now",now);
        q.unwrap(org.hibernate.query.NativeQuery.class).setParameter("reason",reason,String.class);
        if(q.executeUpdate()!=1)throw new IllegalStateException("Locked Session cancellation failed.");
        return session(actor,course,session,false).orElseThrow();
    }
    private static final String SESSION_SELECT = """
        SELECT s.id,s.course_id,s.session_number,s.title,s.scheduled_start_at,s.scheduled_end_at,s.status,
               s.content,s.cancellation_reason,s.created_at,s.updated_at
        FROM public.sessions s JOIN public.courses c ON c.id=s.course_id
        WHERE c.teacher_id=:actor AND s.course_id=:course
        """;
    public List<SessionDto> sessions(long actor,long course,int page,int size,String status) {
        var q=em.createNativeQuery(SESSION_SELECT+(status==null?"":" AND s.status=:status")+" ORDER BY s.session_number")
                .setParameter("actor",actor).setParameter("course",course);
        if(status!=null)q.setParameter("status",status);
        return ((List<?>)q.setFirstResult(Math.multiplyExact(page,size)).setMaxResults(size+1).getResultList())
                .stream().map(this::sessionDto).toList();
    }
    public Optional<SessionDto> session(long actor,long course,long id,boolean lock) {
        var rows=em.createNativeQuery(SESSION_SELECT+" AND s.id=:id"+(lock?" FOR UPDATE OF s":""))
                .setParameter("actor",actor).setParameter("course",course).setParameter("id",id).getResultList();
        return rows.isEmpty()?Optional.empty():Optional.of(sessionDto(rows.get(0)));
    }
    public SessionDto updateSession(long actor,long course,long id,SessionInput value,Instant now) {
        var q=em.createNativeQuery("""
            UPDATE public.sessions s SET title=:title,content=:content,updated_at=:now
            FROM public.courses c WHERE c.id=s.course_id AND c.teacher_id=:actor AND s.course_id=:course
            AND s.id=:id AND s.status='SCHEDULED' AND c.status IN ('DRAFT','PUBLISHED')
            """).setParameter("actor",actor).setParameter("course",course).setParameter("id",id).setParameter("now",now);
        var typed=q.unwrap(org.hibernate.query.NativeQuery.class);
        typed.setParameter("title",value.title(),String.class);typed.setParameter("content",value.content(),String.class);
        if(q.executeUpdate()!=1)throw new IllegalStateException("Locked Session update failed.");
        return session(actor,course,id,false).orElseThrow();
    }
    private SessionDto sessionDto(Object value) {
        Object[] r=(Object[])value;
        return new SessionDto(new ApiId(((Number)r[0]).longValue()),new ApiId(((Number)r[1]).longValue()),
                ((Number)r[2]).intValue(),(String)r[3],instant(r[4]),instant(r[5]),(String)r[6],
                (String)r[7],(String)r[8],instant(r[9]),instant(r[10]));
    }
    public long sessionCount(long id) {
        return ((Number) em.createNativeQuery("SELECT count(*) FROM public.sessions WHERE course_id=:id")
                .setParameter("id", id).getSingleResult()).longValue();
    }
    public List<com.englishlearning.features.course.dto.ScheduleDto.Rule> rules(long id) {
        return ((List<?>) em.createNativeQuery("SELECT id,day_of_week,start_time,end_time FROM public.course_schedule_rules WHERE course_id=:id ORDER BY day_of_week,start_time,id")
                .setParameter("id",id).getResultList()).stream().map(value -> {
                    Object[] r=(Object[])value;
                    return new com.englishlearning.features.course.dto.ScheduleDto.Rule(new ApiId(((Number)r[0]).longValue()),
                            ((Number)r[1]).intValue(), localTime(r[2]).toString(), localTime(r[3]).toString());
                }).toList();
    }
    private LocalTime localTime(Object value) { return value instanceof LocalTime t ? t : ((java.sql.Time)value).toLocalTime(); }
    public void replaceRules(long id, List<com.englishlearning.features.course.service.SchedulePlan.Rule> rules, Instant now) {
        em.createNativeQuery("DELETE FROM public.course_schedule_rules WHERE course_id=:id").setParameter("id",id).executeUpdate();
        for (var r:rules) em.createNativeQuery("INSERT INTO public.course_schedule_rules(course_id,day_of_week,start_time,end_time,created_at,updated_at) VALUES (:id,:day,:start,:end,:now,:now)")
                .setParameter("id",id).setParameter("day",r.dayOfWeek()).setParameter("start",r.startTime())
                .setParameter("end",r.endTime()).setParameter("now",now).executeUpdate();
    }
    public void insertSessions(long id, List<com.englishlearning.features.course.service.SchedulePlan.Occurrence> occurrences, Instant now) {
        for (var s:occurrences) em.createNativeQuery("""
            INSERT INTO public.sessions(course_id,session_number,scheduled_start_at,scheduled_end_at,status,created_at,updated_at)
            VALUES (:id,:number,:start,:end,'SCHEDULED',:now,:now)
            """).setParameter("id",id).setParameter("number",s.number()).setParameter("start",s.start())
                .setParameter("end",s.end()).setParameter("now",now).executeUpdate();
    }
    private static final String SELECT = """
        SELECT c.id,c.name,c.status,k.id,k.name,k.active,c.tuition,c.currency,c.planned_start_date,
               c.timezone,c.session_count,c.min_students,c.max_students,c.created_at,c.updated_at,
               c.description,c.discount_percent,c.discount_start_at,c.discount_end_at
        FROM public.courses c JOIN public.course_categories k ON k.id=c.category_id
        WHERE c.teacher_id=:actor
        """;
    public Optional<CourseDto> find(long actor, long id, boolean lock) {
        var rows = em.createNativeQuery(SELECT + " AND c.id=:id" + (lock ? " FOR UPDATE OF c" : ""))
                .setParameter("actor", actor).setParameter("id", id).getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(dto(rows.get(0)));
    }
    public List<CourseDto> list(long actor, int page, int size, String status) {
        var q = em.createNativeQuery(SELECT + (status == null ? "" : " AND c.status=:status") + " ORDER BY c.id")
                .setParameter("actor", actor);
        if (status != null) q.setParameter("status", status);
        return ((List<?>) q.setFirstResult(Math.multiplyExact(page, size)).setMaxResults(size + 1).getResultList())
                .stream().map(this::dto).toList();
    }
    public CourseDto create(long actor, CourseInput value, Instant now) {
        var q = em.createNativeQuery("""
            INSERT INTO public.courses(teacher_id,category_id,name,description,tuition,currency,min_students,
                max_students,planned_start_date,timezone,session_count,discount_percent,discount_start_at,
                discount_end_at,status,created_at,updated_at)
            VALUES (:actor,:category,:name,:description,:tuition,'VND',:min,:max,:date,:zone,:count,
                :percent,:start,:end,'DRAFT',:now,:now) RETURNING id
            """).setParameter("actor", actor);
        bind(q, value, now);
        long id = ((Number) q.getSingleResult()).longValue();
        return find(actor, id, false).orElseThrow();
    }
    public CourseDto update(long actor, long id, CourseInput value, Instant now) {
        var q = em.createNativeQuery("""
            UPDATE public.courses SET category_id=:category,name=:name,description=:description,tuition=:tuition,
                min_students=:min,max_students=:max,planned_start_date=:date,timezone=:zone,session_count=:count,
                discount_percent=:percent,discount_start_at=:start,discount_end_at=:end,updated_at=:now
            WHERE id=:id AND teacher_id=:actor AND status='DRAFT'
            """).setParameter("id", id).setParameter("actor", actor);
        bind(q, value, now);
        if (q.executeUpdate() != 1) throw new IllegalStateException("Locked Course update failed.");
        return find(actor, id, false).orElseThrow();
    }
    private void bind(Query q, CourseInput v, Instant now) {
        q.setParameter("category", v.categoryId()).setParameter("name", v.name()).setParameter("description", v.description())
                .setParameter("tuition", v.tuition()).setParameter("min", v.minStudents()).setParameter("max", v.maxStudents())
                .setParameter("date", v.plannedStartDate()).setParameter("zone", v.timezone()).setParameter("count", v.sessionCount())
                .setParameter("now", now);
        var typed = q.unwrap(org.hibernate.query.NativeQuery.class);
        typed.setParameter("percent", v.discount() == null ? null : new BigDecimal(v.discount().percent()), BigDecimal.class);
        typed.setParameter("start", v.discount() == null ? null : v.discount().startAt(), Instant.class);
        typed.setParameter("end", v.discount() == null ? null : v.discount().endAt(), Instant.class);
    }
    private CourseDto dto(Object value) {
        Object[] r = (Object[]) value;
        return new CourseDto(new ApiId(((Number)r[0]).longValue()), (String)r[1], (String)r[2],
                new CourseDto.Category(new ApiId(((Number)r[3]).longValue()), (String)r[4], (Boolean)r[5]),
                ApiMoney.from(((BigDecimal)r[6]).setScale(2), (String)r[7]),
                r[8] instanceof LocalDate d ? d : ((java.sql.Date)r[8]).toLocalDate(), (String)r[9],
                ((Number)r[10]).intValue(), ((Number)r[11]).intValue(), ((Number)r[12]).intValue(),
                instant(r[13]), instant(r[14]), (String)r[15], r[16] == null ? null :
                new CourseDto.Discount(((BigDecimal)r[16]).setScale(2).toPlainString(), instant(r[17]), instant(r[18])));
    }
    private Instant instant(Object v) {
        return v instanceof Instant i ? i : v instanceof OffsetDateTime o ? o.toInstant() : ((java.sql.Timestamp)v).toInstant();
    }
}
