package com.englishlearning.features.course.service;
import com.englishlearning.features.course.dto.AdminCourseDto;
import com.englishlearning.features.course.repository.CourseRepository;
import com.englishlearning.features.user.service.CurrentAccountService;
import com.englishlearning.features.user.entity.User.Role;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional(readOnly=true)
public class AdminCourseService {
    private final CourseRepository courses;
    private final CurrentAccountService accounts;
    public AdminCourseService(CourseRepository courses,CurrentAccountService accounts){this.courses=courses;this.accounts=accounts;}
    public record Page(List<AdminCourseDto> items,int page,int size,boolean hasNext){}
    private void authorize(Long actor){
        var account=accounts.findById(actor).orElseThrow(()->new CourseFailure(CourseFailure.Reason.DENIED));
        if(account.locked()||account.role()!=Role.ADMIN||!account.emailVerified())throw new CourseFailure(CourseFailure.Reason.DENIED);
    }
    public Page list(Long actor,int page,int size,String search,Long teacher,Long category,String status){
        authorize(actor);
        if(page<0||size<1||size>100||(long)page*size>Integer.MAX_VALUE||search.length()>100||search.codePoints().anyMatch(Character::isISOControl)
            ||teacher!=null&&teacher<=0||category!=null&&category<=0||status!=null&&!Set.of("DRAFT","PUBLISHED","COMPLETED","CANCELLED","ARCHIVED").contains(status))throw CourseFailure.invalid();
        var rows=courses.oversight(page,size,search.strip(),teacher,category,status,null);
        return new Page(rows.stream().limit(size).toList(),page,size,rows.size()>size);
    }
    public AdminCourseDto detail(Long actor,long id){authorize(actor);if(id<=0)throw CourseFailure.invalid();
        return courses.oversight(0,1,"",null,null,null,id).stream().findFirst().orElseThrow(()->new CourseFailure(CourseFailure.Reason.MISSING));}
}
