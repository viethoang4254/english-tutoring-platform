package com.englishlearning.features.category.controller;

import com.englishlearning.features.auth.security.AccountPrincipal;
import com.englishlearning.features.category.service.CategoryService;
import com.englishlearning.features.category.service.TeacherCategoryService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TeacherCategoryController {
    private final TeacherCategoryService service;

    public TeacherCategoryController(TeacherCategoryService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/teacher/categories")
    public ResponseEntity<CategoryService.Page> list(
            @AuthenticationPrincipal AccountPrincipal actor, HttpServletRequest request) throws IOException {
        if (!Set.of("page", "size").containsAll(request.getParameterMap().keySet())
                || request.getInputStream().read() != -1) throw CategoryController.invalid();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.list(actor.id(),
                CategoryController.number(request, "page", 0), CategoryController.number(request, "size", 20)));
    }
}
