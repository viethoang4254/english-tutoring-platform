package com.englishlearning.features.course.service;

import com.englishlearning.features.course.dto.PublicCourseDto;
import com.englishlearning.features.course.repository.CourseRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.englishlearning.features.course.service.CourseFailure.*;

@Service
@Transactional(readOnly=true)
public class PublicCourseService {
    private final CourseRepository courses;
    public PublicCourseService(CourseRepository courses) { this.courses=courses; }
    public record Page(List<PublicCourseDto> items,int page,int size,boolean hasNext) {}
    public Page list(int page,int size) {
        if(page<0 || size<1 || size>100 || (long)page*size>Integer.MAX_VALUE)throw invalid();
        var rows=courses.publicList(page,size);
        return new Page(rows.stream().limit(size).toList(),page,size,rows.size()>size);
    }
    public PublicCourseDto.Detail get(long id) {
        var course=courses.publicFind(id).orElseThrow(()->new CourseFailure(Reason.MISSING));
        return new PublicCourseDto.Detail(course,courses.rules(id),courses.outline(id));
    }
}
