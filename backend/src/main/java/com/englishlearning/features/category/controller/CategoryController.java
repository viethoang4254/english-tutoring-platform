package com.englishlearning.features.category.controller;
import com.englishlearning.features.category.service.*;
import com.englishlearning.features.auth.security.AccountPrincipal;
import com.englishlearning.common.api.ApiId;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/admin/categories")
public class CategoryController {
 private final CategoryService service;
 public CategoryController(CategoryService service){this.service=service;}
 @GetMapping public ResponseEntity<CategoryService.Page> list(@AuthenticationPrincipal AccountPrincipal actor,HttpServletRequest r)throws IOException{
  if(!Set.of("page","size","active").containsAll(r.getParameterMap().keySet())||r.getInputStream().read()!=-1)throw invalid();
  int page=number(r,"page",0),size=number(r,"size",20);String a=parameter(r,"active");
  if(a!=null&&!a.equals("true")&&!a.equals("false"))throw invalid();
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.list(actor.id(),page,size,a==null?null:Boolean.valueOf(a)));
 }
 @PostMapping(consumes=MediaType.APPLICATION_JSON_VALUE) public ResponseEntity<?> create(@AuthenticationPrincipal AccountPrincipal actor,@RequestBody Map<String,Object> fields,HttpServletRequest r){
  noQuery(r);var dto=service.create(actor.id(),fields);return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(dto);
 }
 @PatchMapping(value="/{categoryId}",consumes=MediaType.APPLICATION_JSON_VALUE) public ResponseEntity<?> update(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable String categoryId,@RequestBody Map<String,Object> fields,HttpServletRequest r){
  noQuery(r);long id;try{id=ApiId.parse(categoryId).value();if(id<=0)throw invalid();}catch(IllegalArgumentException e){throw invalid();}
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.update(actor.id(),id,fields));
 }
 static String parameter(HttpServletRequest r,String name){String[] values=r.getParameterValues(name);if(values==null)return null;if(values.length!=1)throw invalid();return values[0];}
 static int number(HttpServletRequest r,String name,int fallback){String v=parameter(r,name);if(v==null)return fallback;if(!v.matches("[0-9]+"))throw invalid();try{return Integer.parseInt(v);}catch(NumberFormatException e){throw invalid();}}
 static void noQuery(HttpServletRequest r){if(r.getQueryString()!=null)throw invalid();}
 static CategoryFailure invalid(){return new CategoryFailure(CategoryFailure.Reason.INVALID);}
}
