package com.englishlearning.features.category.service;

import com.englishlearning.features.category.repository.CategoryRepository;
import com.englishlearning.features.teacher.service.TeacherBusinessEligibility;
import com.englishlearning.features.teacher.service.TeacherReviewFailure;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherCategoryService {
    private final CategoryRepository categories;
    private final TeacherBusinessEligibility eligibility;

    public TeacherCategoryService(CategoryRepository categories, TeacherBusinessEligibility eligibility) {
        this.categories = categories;
        this.eligibility = eligibility;
    }

    @Transactional(readOnly = true)
    public CategoryService.Page list(Long actor, int page, int size) {
        try {
            eligibility.requireEligible(actor);
        } catch (TeacherReviewFailure failure) {
            throw new CategoryFailure(CategoryFailure.Reason.DENIED);
        }
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE)
            throw new CategoryFailure(CategoryFailure.Reason.INVALID);
        var rows = categories.list(page, size, true);
        return new CategoryService.Page(rows.stream().limit(size).toList(), page, size, rows.size() > size);
    }
}
