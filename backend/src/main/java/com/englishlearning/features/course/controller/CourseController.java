package com.englishlearning.features.course.controller;

import com.englishlearning.features.auth.security.AccountPrincipal;
import com.englishlearning.features.course.dto.CourseDto;
import com.englishlearning.features.course.dto.ScheduleDto;
import com.englishlearning.features.course.dto.SessionDto;
import com.englishlearning.features.course.service.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import static com.englishlearning.features.course.service.CourseFailure.invalid;

@RestController
@RequestMapping("/api/v1/teacher/courses")
public class CourseController {
    private final CourseService service;
    public CourseController(CourseService service) { this.service = service; }
    @PostMapping("/{courseId}/sessions/{sessionId}/reschedule")
    public ResponseEntity<SessionDto> reschedule(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable String courseId,
            @PathVariable String sessionId,@RequestBody Map<String,Object> fields,HttpServletRequest request) {
        query(request,Set.of());return ok(service.reschedule(actor.id(),CourseInput.id(courseId),CourseInput.id(sessionId),fields));
    }
    @PostMapping("/{courseId}/sessions/{sessionId}/cancel")
    public ResponseEntity<SessionDto> cancel(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable String courseId,
            @PathVariable String sessionId,@RequestBody Map<String,Object> fields,HttpServletRequest request) {
        query(request,Set.of());return ok(service.cancel(actor.id(),CourseInput.id(courseId),CourseInput.id(sessionId),fields));
    }
    @GetMapping("/{courseId}/sessions/{sessionId}/schedule-changes")
    public ResponseEntity<CourseService.HistoryPage> history(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable String courseId,
            @PathVariable String sessionId,HttpServletRequest request) throws IOException {
        query(request,Set.of("page","size"));noBody(request);
        return ok(service.history(actor.id(),CourseInput.id(courseId),CourseInput.id(sessionId),number(request,"page",0),number(request,"size",20)));
    }
    @GetMapping("/{courseId}/sessions")
    public ResponseEntity<CourseService.SessionPage> sessions(@AuthenticationPrincipal AccountPrincipal actor,
            @PathVariable String courseId,HttpServletRequest request) throws IOException {
        query(request,Set.of("page","size","status"));noBody(request);
        return ok(service.sessions(actor.id(),CourseInput.id(courseId),number(request,"page",0),number(request,"size",20),request.getParameter("status")));
    }
    @GetMapping("/{courseId}/sessions/{sessionId}")
    public ResponseEntity<SessionDto> session(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable String courseId,
            @PathVariable String sessionId,HttpServletRequest request) throws IOException {
        query(request,Set.of());noBody(request);
        return ok(service.session(actor.id(),CourseInput.id(courseId),CourseInput.id(sessionId)));
    }
    @PatchMapping("/{courseId}/sessions/{sessionId}")
    public ResponseEntity<SessionDto> updateSession(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable String courseId,
            @PathVariable String sessionId,@RequestBody Map<String,Object> fields,HttpServletRequest request) {
        query(request,Set.of());
        return ok(service.updateSession(actor.id(),CourseInput.id(courseId),CourseInput.id(sessionId),fields));
    }
    @GetMapping("/{courseId}/schedule")
    public ResponseEntity<ScheduleDto> schedule(@AuthenticationPrincipal AccountPrincipal actor,
            @PathVariable String courseId, HttpServletRequest request) throws IOException {
        query(request,Set.of()); noBody(request);
        return ok(service.schedule(actor.id(),CourseInput.id(courseId)));
    }
    @PutMapping("/{courseId}/schedule")
    public ResponseEntity<ScheduleDto> replaceSchedule(@AuthenticationPrincipal AccountPrincipal actor,
            @PathVariable String courseId, @RequestBody Map<String,Object> fields, HttpServletRequest request) {
        query(request,Set.of());
        return ok(service.replaceSchedule(actor.id(),CourseInput.id(courseId),fields));
    }
    @PostMapping("/{courseId}/sessions/generate")
    public ResponseEntity<ScheduleDto.Generated> generate(@AuthenticationPrincipal AccountPrincipal actor,
            @PathVariable String courseId, HttpServletRequest request) throws IOException {
        query(request,Set.of()); noBody(request);
        var result=service.generate(actor.id(),CourseInput.id(courseId));
        return ResponseEntity.created(URI.create("/api/v1/teacher/courses/"+result.courseId().decimal()+"/sessions"))
                .cacheControl(CacheControl.noStore()).body(result);
    }
    @GetMapping
    public ResponseEntity<CourseService.Page> list(@AuthenticationPrincipal AccountPrincipal actor,
            HttpServletRequest request) throws IOException {
        query(request, Set.of("page", "size", "status")); noBody(request);
        return ok(service.list(actor.id(), number(request, "page", 0), number(request, "size", 20), request.getParameter("status")));
    }
    @GetMapping("/{courseId}")
    public ResponseEntity<CourseDto> get(@AuthenticationPrincipal AccountPrincipal actor,
            @PathVariable String courseId, HttpServletRequest request) throws IOException {
        query(request, Set.of()); noBody(request);
        return ok(service.get(actor.id(), CourseInput.id(courseId)));
    }
    @PostMapping
    public ResponseEntity<CourseDto> create(@AuthenticationPrincipal AccountPrincipal actor,
            @RequestBody Map<String,Object> fields, HttpServletRequest request) {
        query(request, Set.of());
        var dto = service.create(actor.id(), fields);
        return ResponseEntity.created(URI.create("/api/v1/teacher/courses/" + dto.id().decimal()))
                .cacheControl(CacheControl.noStore()).body(dto);
    }
    @PatchMapping("/{courseId}")
    public ResponseEntity<CourseDto> update(@AuthenticationPrincipal AccountPrincipal actor,
            @PathVariable String courseId, @RequestBody Map<String,Object> fields, HttpServletRequest request) {
        query(request, Set.of());
        return ok(service.update(actor.id(), CourseInput.id(courseId), fields));
    }
    private static <T> ResponseEntity<T> ok(T body) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body); }
    static void query(HttpServletRequest request, Set<String> keys) {
        if (!keys.containsAll(request.getParameterMap().keySet())
                || request.getParameterMap().values().stream().anyMatch(v -> v.length != 1)) throw invalid();
    }
    static void noBody(HttpServletRequest request) throws IOException {
        if (request.getInputStream().read() != -1) throw invalid();
    }
    static int number(HttpServletRequest request, String key, int fallback) {
        String value = request.getParameter(key); if (value == null) return fallback;
        if (!value.matches("[0-9]+")) throw invalid();
        try { return Integer.parseInt(value); } catch (NumberFormatException e) { throw invalid(); }
    }
}
