package com.englishlearning.features.course.service;

import com.englishlearning.features.category.repository.CategoryRepository;
import com.englishlearning.features.course.dto.CourseDto;
import com.englishlearning.features.course.dto.ScheduleDto;
import com.englishlearning.features.course.dto.SessionDto;
import com.englishlearning.features.course.dto.ScheduleChangeDto;
import com.englishlearning.features.course.repository.CourseRepository;
import com.englishlearning.features.teacher.service.*;
import com.englishlearning.features.user.service.UserCredentialService;
import java.time.Clock;
import java.util.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import static com.englishlearning.features.course.service.CourseFailure.*;

@Service
public class CourseService {
    public static final Set<String> STATUSES = Set.of("DRAFT", "PUBLISHED", "COMPLETED", "CANCELLED", "ARCHIVED");
    private final CourseRepository courses;
    private final CategoryRepository categories;
    private final TeacherBusinessEligibility eligibility;
    private final UserCredentialService users;
    private final Clock clock;
    public CourseService(CourseRepository courses, CategoryRepository categories, TeacherBusinessEligibility eligibility,
            UserCredentialService users, @Qualifier("refreshSessionClock") Clock clock) {
        this.courses=courses; this.categories=categories; this.eligibility=eligibility; this.users=users; this.clock=clock;
    }
    public record CalendarPage(List<com.englishlearning.features.course.dto.CalendarSessionDto> items,int page,int size,boolean hasNext) {}
    @Transactional(readOnly=true)
    public CalendarPage calendar(long actor,String from,String to,int page,int size) {
        eligible(actor);
        java.time.Instant start,end;
        try {
            var startValue=java.time.OffsetDateTime.parse(from);
            var endValue=java.time.OffsetDateTime.parse(to);
            if(startValue.getYear()<1 || startValue.getYear()>9999 || endValue.getYear()<1 || endValue.getYear()>9999) throw invalid();
            start=startValue.toInstant();
            end=endValue.toInstant();
        } catch (java.time.DateTimeException | NullPointerException e) { throw invalid(); }
        if (!start.isBefore(end) || java.time.Duration.between(start,end).compareTo(java.time.Duration.ofDays(62))>0
                || page<0 || size<1 || size>100 || (long)page*size>Integer.MAX_VALUE) throw invalid();
        var rows=courses.calendar(actor,start,end,page,size);
        return new CalendarPage(rows.stream().limit(size).toList(),page,size,rows.size()>size);
    }
    public record Page(List<CourseDto.Summary> items, int page, int size, boolean hasNext) {}
    public record SessionPage(List<SessionDto.Summary> items,int page,int size,boolean hasNext) {}
    public record HistoryPage(List<ScheduleChangeDto> items,int page,int size,boolean hasNext) {}
    @Transactional(readOnly=true)
    public HistoryPage history(long actor,long course,long session,int page,int size) {
        eligible(actor);owned(actor,course,false);ownedSession(actor,course,session,false);
        if(page<0 || size<1 || size>100 || (long)page*size>Integer.MAX_VALUE)throw invalid();
        var rows=courses.history(actor,course,session,page,size);
        return new HistoryPage(rows.stream().limit(size).toList(),page,size,rows.size()>size);
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public SessionDto reschedule(long actor,long course,long session,Map<String,Object> fields) {
        lockActor(actor);var parent=owned(actor,course,true);var current=ownedSession(actor,course,session,true);
        var now=clock.instant();
        requireFutureSession(parent,current,now);
        var input=RescheduleInput.parse(fields,parent.timezone(),now);
        if(!input.expectedStartAt().equals(current.scheduledStartAt()) || !input.expectedEndAt().equals(current.scheduledEndAt()))
            throw new CourseFailure(Reason.SESSION_CHANGED);
        if(courses.hasCalendarEvents(session))throw new CourseFailure(Reason.CALENDAR_SYNC_REQUIRED);
        if(input.scheduledStartAt().equals(current.scheduledStartAt()) && input.scheduledEndAt().equals(current.scheduledEndAt()))return current;
        if(courses.overlaps(course,session,input.scheduledStartAt(),input.scheduledEndAt()))throw new CourseFailure(Reason.SCHEDULE_OVERLAP);
        return courses.reschedule(actor,course,current,input,now);
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public SessionDto cancel(long actor,long course,long session,Map<String,Object> fields) {
        lockActor(actor);var parent=owned(actor,course,true);var current=ownedSession(actor,course,session,true);
        String reason=RescheduleInput.cancellationReason(fields);
        // A retry of an already completed cancellation is a read, preserving its original metadata.
        if(current.status().equals("CANCELLED"))return current;
        var now=clock.instant();requireFutureSession(parent,current,now);
        if(courses.hasCalendarEvents(session))throw new CourseFailure(Reason.CALENDAR_SYNC_REQUIRED);
        return courses.cancel(actor,course,session,reason,now);
    }
    private void requireFutureSession(CourseDto parent,SessionDto current,java.time.Instant now) {
        if(!Set.of("DRAFT","PUBLISHED").contains(parent.status()) || !current.status().equals("SCHEDULED"))
            throw new CourseFailure(Reason.SESSION_NOT_EDITABLE);
        if(!current.scheduledStartAt().isAfter(now))throw new CourseFailure(Reason.SCHEDULE_TIME);
    }
    @Transactional(readOnly=true)
    public SessionPage sessions(long actor,long course,int page,int size,String status) {
        eligible(actor);owned(actor,course,false);
        if(page<0 || size<1 || size>100 || (long)page*size>Integer.MAX_VALUE
                || status!=null && !Set.of("SCHEDULED","CANCELLED").contains(status))throw invalid();
        var rows=courses.sessions(actor,course,page,size,status);
        return new SessionPage(rows.stream().limit(size).map(SessionDto::summary).toList(),page,size,rows.size()>size);
    }
    @Transactional(readOnly=true)
    public SessionDto session(long actor,long course,long id) {
        eligible(actor);owned(actor,course,false);
        return ownedSession(actor,course,id,false);
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public SessionDto updateSession(long actor,long course,long id,Map<String,Object> fields) {
        lockActor(actor);var parent=owned(actor,course,true);var current=ownedSession(actor,course,id,true);
        if(!Set.of("DRAFT","PUBLISHED").contains(parent.status()) || !current.status().equals("SCHEDULED"))
            throw new CourseFailure(Reason.SESSION_NOT_EDITABLE);
        var input=SessionInput.merge(fields,current);
        return courses.updateSession(actor,course,id,input,clock.instant());
    }
    private SessionDto ownedSession(long actor,long course,long id,boolean lock) {
        if(id<=0)throw invalid();
        return courses.session(actor,course,id,lock).orElseThrow(()->new CourseFailure(Reason.SESSION_MISSING));
    }
    @Transactional(readOnly=true)
    public Page list(long actor, int page, int size, String status) {
        eligible(actor);
        if (page < 0 || size < 1 || size > 100 || (long)page * size > Integer.MAX_VALUE
                || status != null && !STATUSES.contains(status)) throw invalid();
        var rows = courses.list(actor, page, size, status);
        return new Page(rows.stream().limit(size).map(CourseDto::summary).toList(), page, size, rows.size() > size);
    }
    @Transactional(readOnly=true)
    public CourseDto get(long actor, long id) { eligible(actor); return owned(actor, id, false); }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public CourseDto create(long actor, Map<String,Object> fields) {
        lockActor(actor);
        var input = CourseInput.merge(fields, null);
        activeCategory(input.categoryId());
        return courses.create(actor, input, clock.instant());
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public CourseDto update(long actor, long id, Map<String,Object> fields) {
        lockActor(actor);
        var current = owned(actor, id, true);
        if (!current.status().equals("DRAFT")) throw new CourseFailure(Reason.NOT_DRAFT);
        var input = CourseInput.merge(fields, current);
        if ((!input.plannedStartDate().equals(current.plannedStartDate()) || !input.timezone().equals(current.timezone())
                || input.sessionCount()!=current.sessionCount()) && courses.sessionCount(id)>0)
            throw new CourseFailure(Reason.SCHEDULE_LOCKED);
        if (input.categoryId() != current.category().id().value()) activeCategory(input.categoryId());
        return courses.update(actor, id, input, clock.instant());
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public ScheduleDto schedule(long actor, long id) {
        lockActor(actor);
        return schedule(owned(actor,id,true));
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public ScheduleDto replaceSchedule(long actor, long id, Map<String,Object> fields) {
        lockActor(actor);
        var course=owned(actor,id,true);
        if (!course.status().equals("DRAFT")) throw new CourseFailure(Reason.NOT_DRAFT);
        if (courses.sessionCount(id)>0) throw new CourseFailure(Reason.SCHEDULE_LOCKED);
        var rules=SchedulePlan.parse(fields);
        var existing=planRules(courses.rules(id));
        if (!existing.equals(rules)) courses.replaceRules(id,rules,clock.instant());
        return schedule(course);
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public ScheduleDto.Generated generate(long actor, long id) {
        lockActor(actor);
        var course=owned(actor,id,true);
        if (!course.status().equals("DRAFT")) throw new CourseFailure(Reason.NOT_DRAFT);
        if (courses.sessionCount(id)>0) throw new CourseFailure(Reason.ALREADY_GENERATED);
        var now=clock.instant();
        var occurrences=SchedulePlan.generate(planRules(courses.rules(id)),course.plannedStartDate(),course.timezone(),course.sessionCount(),now);
        courses.insertSessions(id,occurrences,now);
        return new ScheduleDto.Generated(course.id(),occurrences.size());
    }
    private List<SchedulePlan.Rule> planRules(List<ScheduleDto.Rule> rules) {
        return rules.stream().map(r -> new SchedulePlan.Rule(r.dayOfWeek(),java.time.LocalTime.parse(r.startTime()),java.time.LocalTime.parse(r.endTime()))).toList();
    }
    private ScheduleDto schedule(CourseDto course) {
        long id=course.id().value();
        return new ScheduleDto(course.id(),course.plannedStartDate(),course.timezone(),course.sessionCount(),courses.sessionCount(id),courses.rules(id));
    }
    private CourseDto owned(long actor, long id, boolean lock) {
        if (id <= 0) throw invalid();
        return courses.find(actor, id, lock).orElseThrow(() -> new CourseFailure(Reason.MISSING));
    }
    private void activeCategory(long id) {
        // FOR UPDATE also blocks Admin deactivation; a foreign-key/key-share lock does not.
        var category = categories.find(id, true).orElseThrow(() -> new CourseFailure(Reason.CATEGORY_MISSING));
        if (!category.active()) throw new CourseFailure(Reason.CATEGORY_INACTIVE);
    }
    private void lockActor(long actor) {
        var account = users.lockAccountById(actor).orElseThrow(() -> new CourseFailure(Reason.DENIED));
        // Use the locked database snapshot even if a request-scoped JPA identity was loaded earlier.
        if (account.locked() || !account.emailVerified()
                || account.role() != com.englishlearning.features.user.entity.User.Role.TEACHER)
            throw new CourseFailure(Reason.DENIED);
        eligible(actor);
    }
    private void eligible(long actor) {
        try { eligibility.requireEligible(actor); }
        catch (TeacherReviewFailure e) { throw new CourseFailure(Reason.DENIED); }
    }
}
