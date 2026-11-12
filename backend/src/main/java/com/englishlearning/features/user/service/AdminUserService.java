package com.englishlearning.features.user.service;
import com.englishlearning.features.user.dto.AdminUserDto;
import com.englishlearning.features.user.repository.AdminUserRepository;
import com.englishlearning.features.user.entity.User.Role;
import java.util.*;
import java.time.Clock;
import com.englishlearning.features.user.repository.StudentProfileRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional(readOnly=true)
public class AdminUserService {
 private final AdminUserRepository users; private final CurrentAccountService accounts;
 private final StudentProfileRepository profiles;
 private final Clock clock;
 public AdminUserService(AdminUserRepository users, CurrentAccountService accounts, StudentProfileRepository profiles,
                         @Qualifier("refreshSessionClock") Clock clock) {
  this.users=users; this.accounts=accounts; this.profiles=profiles; this.clock=clock;
 }
 public record Page(List<AdminUserDto> items,int page,int size,boolean hasNext){}
 public static class Failure extends RuntimeException {public final int status; public Failure(int status){this.status=status;}}
 private void authorize(Long actor){var a=accounts.findById(actor).orElseThrow(()->new Failure(401));if(a.locked())throw new Failure(401);if(a.role()!=Role.ADMIN||!a.emailVerified())throw new Failure(403);}
 public Page list(Long actor,int page,int size,String search,String role){
  authorize(actor);
  if(page<0||size<1||size>100||(long)page*size>Integer.MAX_VALUE||search.length()>100||search.codePoints().anyMatch(Character::isISOControl)||role!=null&&!Set.of("STUDENT","TEACHER","ADMIN").contains(role))throw new Failure(400);
  var rows=users.list(page,size,search.trim(),role);return new Page(rows.stream().limit(size).toList(),page,size,rows.size()>size);
 }
 @Transactional
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
  users.lockParticipants(actor,target);
  authorize(actor);
  var before=users.find(target).orElseThrow(()->new Failure(404));
  // Admin-to-Admin management is outside the approved Student/Teacher management boundary.
  if(actor==target || "ADMIN".equals(before.role()))throw new Failure(403);
  return before;
 }
 @Transactional
 public AdminUserDto updateProfile(Long actor,long target, Map<String,Object> fields) {
  var current = lockManagedAccount(actor,target);
  if (fields == null || !fields.keySet().equals(Set.of("fullName")) ||
      !(fields.get("fullName") instanceof String text) || text.isBlank() || text.codePoints().anyMatch(Character::isISOControl)) throw new Failure(400);
  String name = ((String) fields.get("fullName")).strip();
  if (current.fullName().equals(name)) return current;
  profiles.update(target,name,null,clock.instant());
  org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
      new org.springframework.transaction.support.TransactionSynchronization() {
       @Override public void afterCommit() {
        org.slf4j.LoggerFactory.getLogger(AdminUserService.class)
            .info("Account profile changed actor={} target={} field=fullName",actor,target);
       }
      });
  return users.find(target).orElseThrow(()->new Failure(404));
 }
 public AdminUserDto detail(Long actor,long id){authorize(actor);if(id<=0)throw new Failure(400);return users.find(id).orElseThrow(()->new Failure(404));}
}
