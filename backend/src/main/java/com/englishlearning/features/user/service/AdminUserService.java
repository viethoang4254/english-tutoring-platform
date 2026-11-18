package com.englishlearning.features.user.service;
import com.englishlearning.features.user.dto.AdminUserDto;
import com.englishlearning.features.user.repository.AdminUserRepository;
import com.englishlearning.features.user.entity.User.Role;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional(readOnly=true)
public class AdminUserService {
 private final AdminUserRepository users; private final CurrentAccountService accounts;

 public AdminUserService(AdminUserRepository users, CurrentAccountService accounts) {
  this.users=users; this.accounts=accounts;
 }
 public record Page(List<AdminUserDto> items,int page,int size,boolean hasNext){}
 public static class Failure extends RuntimeException {public final int status; public Failure(int status){this.status=status;}}
 private void authorize(Long actor){var a=accounts.findById(actor).orElseThrow(()->new Failure(401));if(a.locked())throw new Failure(401);if(a.role()!=Role.ADMIN||!a.emailVerified())throw new Failure(403);}
 public Page list(Long actor,int page,int size,String search,String role,Boolean locked){
  authorize(actor);
  if(page<0||size<1||size>100||(long)page*size>Integer.MAX_VALUE||search.length()>100||search.codePoints().anyMatch(Character::isISOControl)||role!=null&&!Set.of("STUDENT","TEACHER","ADMIN").contains(role))throw new Failure(400);
  var rows=users.list(page,size,search.trim(),role,locked);return new Page(rows.stream().limit(size).toList(),page,size,rows.size()>size);
 }
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
 public AdminUserDto changeLock(Long actor,long target,boolean locked){
  var before = lockManagedAccount(actor,target);
  if(before.locked()!=locked){
   users.setLocked(target,locked);
   org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
     new org.springframework.transaction.support.TransactionSynchronization(){
      @Override public void afterCommit(){
       org.slf4j.LoggerFactory.getLogger(AdminUserService.class).info("Account status changed actor={} target={} locked={}",actor,target,locked);
      }
     });
  }
  return users.find(target).orElseThrow(()->new Failure(404));
 }
 private AdminUserDto lockManagedAccount(Long actor,long target) {
  if(target<=0)throw new Failure(400);
  authorize(actor);
  users.lockParticipants(actor,target);
  var reviewer=users.find(actor).orElseThrow(()->new Failure(401));
  if(reviewer.locked()||!reviewer.emailVerified()||!"ADMIN".equals(reviewer.role()))throw new Failure(403);
  var before=users.find(target).orElseThrow(()->new Failure(404));
  // Admin-to-Admin management is outside the approved Student/Teacher management boundary.
  if(actor==target || "ADMIN".equals(before.role()))throw new Failure(403);
  return before;
 }
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
 public AdminUserDto updateName(Long actor,long target,Map<String,Object> fields) {
  lockManagedAccount(actor,target);
  if(fields==null||!fields.keySet().equals(Set.of("fullName"))||!(fields.get("fullName") instanceof String name)||name.isBlank()||name.length()>200||name.codePoints().anyMatch(Character::isISOControl))throw new Failure(400);
  users.updateName(target,name.strip());audit(actor,target,"profile-updated");
  return users.find(target).orElseThrow(()->new Failure(404));
 }
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
 public void delete(Long actor,long target) {
  lockManagedAccount(actor,target);
  if(users.hasBusinessDependencies(target))throw new Failure(409);
  users.deleteDisposable(target);audit(actor,target,"permanently-deleted");
 }
 private void audit(long actor,long target,String action) {
  org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization(){
   @Override public void afterCommit(){org.slf4j.LoggerFactory.getLogger(AdminUserService.class).info("Account management actor={} target={} action={}",actor,target,action);}
  });
 }
 public AdminUserDto detail(Long actor,long id){authorize(actor);if(id<=0)throw new Failure(400);return users.find(id).orElseThrow(()->new Failure(404));}
}
