package com.englishlearning.features.course.controller;
import com.englishlearning.features.auth.security.AccountPrincipal;
import com.englishlearning.features.course.service.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/admin/courses")
public class AdminCourseController {
    private final AdminCourseService service;
    public AdminCourseController(AdminCourseService service){this.service=service;}
    @GetMapping public ResponseEntity<?> list(@AuthenticationPrincipal AccountPrincipal actor,HttpServletRequest request)throws IOException {
        CourseController.query(request,Set.of("page","size","search","teacherId","categoryId","status"));CourseController.noBody(request);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.list(actor.id(),CourseController.number(request,"page",0),CourseController.number(request,"size",20),
            request.getParameter("search")==null?"":request.getParameter("search"),optionalId(request,"teacherId"),optionalId(request,"categoryId"),request.getParameter("status")));
    }
    @GetMapping("/{id}") public ResponseEntity<?> detail(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable String id,HttpServletRequest request)throws IOException {
        CourseController.query(request,Set.of());CourseController.noBody(request);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.detail(actor.id(),CourseInput.id(id)));
    }
    private Long optionalId(HttpServletRequest request,String name){return request.getParameter(name)==null?null:CourseInput.id(request.getParameter(name));}
}
