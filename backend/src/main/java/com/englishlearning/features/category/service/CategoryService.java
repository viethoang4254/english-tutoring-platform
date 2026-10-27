package com.englishlearning.features.category.service;
import com.englishlearning.features.category.dto.CategoryDto;
import com.englishlearning.features.category.repository.CategoryRepository;
import com.englishlearning.features.user.service.*;
import com.englishlearning.features.user.entity.User.Role;
import java.time.Clock;
import java.util.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service
public class CategoryService {
 private final CategoryRepository categories;private final CurrentAccountService accounts;private final UserCredentialService users;private final Clock clock;
 public CategoryService(CategoryRepository categories,CurrentAccountService accounts,UserCredentialService users,@Qualifier("refreshSessionClock") Clock clock){this.categories=categories;this.accounts=accounts;this.users=users;this.clock=clock;}
 public record Page(List<CategoryDto> items,int page,int size,boolean hasNext){}
 @Transactional(readOnly=true) public Page list(Long actor,int page,int size,Boolean active){
  check(accounts.findById(actor).orElse(null));
  if(page<0||size<1||size>100||(long)page*size>Integer.MAX_VALUE)throw invalid();
  var rows=categories.list(page,size,active);return new Page(rows.stream().limit(size).toList(),page,size,rows.size()>size);
 }
 @Transactional(isolation=Isolation.READ_COMMITTED) public CategoryDto create(Long actor,Map<String,Object> fields){
  check(users.lockAccountById(actor).orElse(null));validate(fields);if(!fields.containsKey("name"))throw invalid();
  return categories.create(name(fields.get("name")),fields.containsKey("active")?(Boolean)fields.get("active"):true,clock.instant());
 }
 @Transactional(isolation=Isolation.READ_COMMITTED) public CategoryDto update(Long actor,long id,Map<String,Object> fields){
  check(users.lockAccountById(actor).orElse(null));validate(fields);if(id<=0)throw invalid();
  var current=categories.find(id,true).orElseThrow(()->new CategoryFailure(CategoryFailure.Reason.MISSING));
  return categories.update(id,fields.containsKey("name")?name(fields.get("name")):current.name(),fields.containsKey("active")?(Boolean)fields.get("active"):current.active(),clock.instant());
 }
 static void validate(Map<String,Object> fields){
  if(fields==null||fields.isEmpty()||!Set.of("name","active").containsAll(fields.keySet()))throw invalid();
  if(fields.containsKey("name"))name(fields.get("name"));
  if(fields.containsKey("active")&&!(fields.get("active") instanceof Boolean))throw invalid();
 }
 static String name(Object value){
  if(!(value instanceof String text))throw invalid();
  int start=0,end=text.length();while(start<end&&text.charAt(start)==' ')start++;while(end>start&&text.charAt(end-1)==' ')end--;
  String name=text.substring(start,end);int length=name.codePointCount(0,name.length());
  if(length<1||length>100||name.codePoints().anyMatch(c->Character.isISOControl(c)||c>=0xD800&&c<=0xDFFF))throw invalid();return name;
 }
 private void check(AccountSnapshot a){if(a==null||a.locked())throw new CategoryFailure(CategoryFailure.Reason.UNAUTHENTICATED);if(a.role()!=Role.ADMIN||!a.emailVerified())throw new CategoryFailure(CategoryFailure.Reason.DENIED);}
 private static CategoryFailure invalid(){return new CategoryFailure(CategoryFailure.Reason.INVALID);}
}
