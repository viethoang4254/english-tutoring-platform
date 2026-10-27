package com.englishlearning.features.category.controller;
import com.englishlearning.features.category.service.CategoryFailure;
import com.englishlearning.common.api.ApiError;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.TransactionException;
@RestControllerAdvice(assignableTypes={CategoryController.class, TeacherCategoryController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CategoryExceptionHandler {
 @ExceptionHandler(CategoryFailure.class) ResponseEntity<ApiError> rejected(CategoryFailure e){return switch(e.reason()){
 case INVALID->error(400,"INVALID_REQUEST");case UNAUTHENTICATED->error(401,"AUTHENTICATION_REQUIRED");case DENIED->error(403,"ACCESS_DENIED");case MISSING->error(404,"CATEGORY_NOT_FOUND");};}
 @ExceptionHandler({DataAccessException.class,TransactionException.class}) ResponseEntity<ApiError> database(Exception e){
  for(Throwable cause=e;cause!=null;cause=cause.getCause())if(cause instanceof org.hibernate.exception.ConstraintViolationException sql && "23505".equals(sql.getSQLState()) && java.util.Set.of("uq_course_categories_name","uq_course_categories_normalized_name").contains(sql.getConstraintName()==null?"":sql.getConstraintName()))return error(409,"CATEGORY_NAME_CONFLICT");
  return error(503,"CATEGORY_UNAVAILABLE");
 }
 private ResponseEntity<ApiError> error(int status,String code){return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(new ApiError(code,"Category request could not be completed."));}
}
