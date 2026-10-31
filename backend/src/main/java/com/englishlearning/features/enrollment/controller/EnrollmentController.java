package com.englishlearning.features.enrollment.controller;

import com.englishlearning.features.auth.security.AccountPrincipal;
import com.englishlearning.features.enrollment.service.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.util.Set;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class EnrollmentController {
    private final EnrollmentService service;
    public EnrollmentController(EnrollmentService service) { this.service=service; }
    @PostMapping("/student/courses/{courseId}/enrollment")
    public ResponseEntity<?> create(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable String courseId,HttpServletRequest request) throws IOException {
        request(request,Set.of());var result=service.create(actor.id(),id(courseId));
        return result.created()?ResponseEntity.created(URI.create("/api/v1/student/enrollments/"+result.enrollment().id().decimal()))
                .cacheControl(CacheControl.noStore()).body(result.enrollment()):ok(result.enrollment());
    }
    @GetMapping("/student/enrollments")
    public ResponseEntity<?> list(@AuthenticationPrincipal AccountPrincipal actor,HttpServletRequest request) throws IOException {
        request(request,Set.of("page","size","status"));return ok(service.list(actor.id(),number(request,"page",0),number(request,"size",20),request.getParameter("status")));
    }
    @GetMapping("/student/enrollments/{enrollmentId}")
    public ResponseEntity<?> get(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable String enrollmentId,HttpServletRequest request) throws IOException {
        request(request,Set.of());return ok(service.get(actor.id(),id(enrollmentId)));
    }
    @PostMapping("/student/enrollments/{enrollmentId}/renew")
    public ResponseEntity<?> renew(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable String enrollmentId,HttpServletRequest request) throws IOException {
        request(request,Set.of());return ok(service.renew(actor.id(),id(enrollmentId)));
    }
    @PostMapping("/student/enrollments/{enrollmentId}/cancel")
    public ResponseEntity<?> cancel(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable String enrollmentId,HttpServletRequest request) throws IOException {
        request(request,Set.of());return ok(service.cancel(actor.id(),id(enrollmentId)));
    }
    @GetMapping("/teacher/courses/{courseId}/enrollments")
    public ResponseEntity<?> roster(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable String courseId,HttpServletRequest request) throws IOException {
        request(request,Set.of("page","size","status"));return ok(service.roster(actor.id(),id(courseId),number(request,"page",0),number(request,"size",20),request.getParameter("status")));
    }
    private static ResponseEntity<?> ok(Object body) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body); }
    private static void request(HttpServletRequest request,Set<String> keys) throws IOException {
        if(!keys.containsAll(request.getParameterMap().keySet())||request.getParameterMap().values().stream().anyMatch(v->v.length!=1)||request.getInputStream().read()!=-1)throw invalid();
    }
    private static long id(String value) {
        try { if(!value.matches("[1-9][0-9]*"))throw invalid();return Long.parseLong(value); }
        catch(NumberFormatException e) { throw invalid(); }
    }
    private static int number(HttpServletRequest request,String key,int fallback) {
        var value=request.getParameter(key);if(value==null)return fallback;
        try { if(!value.matches("[0-9]+"))throw invalid();return Integer.parseInt(value); }
        catch(NumberFormatException e) { throw invalid(); }
    }
    private static EnrollmentFailure invalid() { return new EnrollmentFailure(EnrollmentFailure.Reason.INVALID_REQUEST); }
}
