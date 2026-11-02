package com.englishlearning.features.course.controller;

import com.englishlearning.features.course.dto.PublicCourseDto;
import com.englishlearning.features.course.service.CourseInput;
import com.englishlearning.features.course.service.PublicCourseService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/courses")
public class PublicCourseController {
    private final PublicCourseService service;
    public PublicCourseController(PublicCourseService service) { this.service=service; }
    @GetMapping
    public ResponseEntity<PublicCourseService.Page> list(HttpServletRequest request) throws IOException {
        CourseController.query(request,Set.of("page","size")); CourseController.noBody(request);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.list(
                CourseController.number(request,"page",0),CourseController.number(request,"size",20)));
    }
    @GetMapping("/{courseId}")
    public ResponseEntity<PublicCourseDto.Detail> get(@PathVariable String courseId,HttpServletRequest request) throws IOException {
        CourseController.query(request,Set.of()); CourseController.noBody(request);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.get(CourseInput.id(courseId)));
    }
}
