package com.englishlearning.features.course.controller;
import com.englishlearning.features.auth.security.AccountPrincipal;
import com.englishlearning.features.course.service.CourseService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController
public class TeacherCalendarController {
    private final CourseService service;
    public TeacherCalendarController(CourseService service) { this.service=service; }
    @GetMapping("/api/v1/teacher/calendar/sessions")
    public ResponseEntity<CourseService.CalendarPage> list(@AuthenticationPrincipal AccountPrincipal actor,
            HttpServletRequest request) throws IOException {
        CourseController.query(request,Set.of("from","to","page","size"));
        CourseController.noBody(request);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.calendar(actor.id(),
            request.getParameter("from"),request.getParameter("to"),
            CourseController.number(request,"page",0),CourseController.number(request,"size",20)));
    }
}
