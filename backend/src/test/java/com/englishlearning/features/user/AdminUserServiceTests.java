package com.englishlearning.features.user;
import com.englishlearning.features.user.service.*;
import com.englishlearning.features.user.repository.AdminUserRepository;
import com.englishlearning.features.user.entity.User.Role;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AdminUserServiceTests {
 @Test void deniesNonAdminBeforeReadingDirectory(){
  var accounts=mock(CurrentAccountService.class);var users=mock(AdminUserRepository.class);var actor=mock(AccountSnapshot.class);
  when(actor.role()).thenReturn(Role.STUDENT);when(accounts.findById(1L)).thenReturn(Optional.of(actor));
  assertEquals(403,assertThrows(AdminUserService.Failure.class,()->new AdminUserService(users,accounts).list(1L,0,20,"",null,null)).status);
  verifyNoInteractions(users);
 }
 @Test void validatesPaginationBeforeQuery(){
  var accounts=mock(CurrentAccountService.class);var users=mock(AdminUserRepository.class);var actor=mock(AccountSnapshot.class);
  when(actor.role()).thenReturn(Role.ADMIN);when(actor.emailVerified()).thenReturn(true);when(accounts.findById(1L)).thenReturn(Optional.of(actor));
  assertEquals(400,assertThrows(AdminUserService.Failure.class,()->new AdminUserService(users,accounts).list(1L,Integer.MAX_VALUE,100,"",null,null)).status);
  verifyNoInteractions(users);
 }
 @Test void missingDetailReturns404(){
  var accounts=mock(CurrentAccountService.class);var users=mock(AdminUserRepository.class);var actor=mock(AccountSnapshot.class);
  when(actor.role()).thenReturn(Role.ADMIN);when(actor.emailVerified()).thenReturn(true);when(accounts.findById(1L)).thenReturn(Optional.of(actor));
  when(users.find(9L)).thenReturn(Optional.empty());assertEquals(404,assertThrows(AdminUserService.Failure.class,()->new AdminUserService(users,accounts).detail(1L,9L)).status);
 }

 @Test void teacherAndStudentCannotLockUsers(){
  for(Role role:List.of(Role.TEACHER,Role.STUDENT)){
   var accounts=mock(CurrentAccountService.class);var users=mock(AdminUserRepository.class);
   var actor=mock(AccountSnapshot.class);when(actor.role()).thenReturn(role);
   when(accounts.findById(1L)).thenReturn(Optional.of(actor));
   assertEquals(403,assertThrows(AdminUserService.Failure.class,()->new AdminUserService(users,accounts).changeLock(1L,2L,true)).status);
   verify(users,never()).setLocked(anyLong(),anyBoolean());
  }
 }
 @Test void cannotLockSelfOrAnotherAdmin(){
  var accounts=mock(CurrentAccountService.class);var users=mock(AdminUserRepository.class);
  var actor=mock(AccountSnapshot.class);when(actor.role()).thenReturn(Role.ADMIN);when(actor.emailVerified()).thenReturn(true);
  when(accounts.findById(1L)).thenReturn(Optional.of(actor));
  var target=mock(com.englishlearning.features.user.dto.AdminUserDto.class);when(target.role()).thenReturn("ADMIN");
  when(users.find(anyLong())).thenReturn(Optional.of(target));
  when(users.find(1L)).thenReturn(Optional.of(new com.englishlearning.features.user.dto.AdminUserDto(new com.englishlearning.common.api.ApiId(1),"Admin","admin@example.invalid","ADMIN",true,false)));
  for(long id:List.of(1L,2L))assertEquals(403,assertThrows(AdminUserService.Failure.class,()->new AdminUserService(users,accounts).changeLock(1L,id,true)).status);
  verify(users,never()).setLocked(anyLong(),anyBoolean());
 }
 @Test void lockAndUnlockOnlyChangeRequestedStatus(){
  for(boolean locked:List.of(true,false)){
   var accounts=mock(CurrentAccountService.class);var users=mock(AdminUserRepository.class);
   var actor=mock(AccountSnapshot.class);when(actor.role()).thenReturn(Role.ADMIN);when(actor.emailVerified()).thenReturn(true);
   when(accounts.findById(1L)).thenReturn(Optional.of(actor));
   var target=mock(com.englishlearning.features.user.dto.AdminUserDto.class);when(target.role()).thenReturn("TEACHER");when(target.locked()).thenReturn(!locked);
   when(users.find(2L)).thenReturn(Optional.of(target));
   when(users.find(1L)).thenReturn(Optional.of(new com.englishlearning.features.user.dto.AdminUserDto(new com.englishlearning.common.api.ApiId(1),"Admin","admin@example.invalid","ADMIN",true,false)));
   org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
   try {new AdminUserService(users,accounts).changeLock(1L,2L,locked);verify(users).setLocked(2L,locked);}
   finally{org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();}
  }
 }

 @Test void deletionChecksDependenciesBeforeRemovingAnyRecords(){
  var accounts=mock(CurrentAccountService.class);var users=mock(AdminUserRepository.class);
  var actor=mock(AccountSnapshot.class);when(actor.role()).thenReturn(Role.ADMIN);when(actor.emailVerified()).thenReturn(true);
  when(accounts.findById(1L)).thenReturn(Optional.of(actor));
  when(users.find(1L)).thenReturn(Optional.of(new com.englishlearning.features.user.dto.AdminUserDto(new com.englishlearning.common.api.ApiId(1),"Admin","admin@example.invalid","ADMIN",true,false)));
  when(users.find(2L)).thenReturn(Optional.of(new com.englishlearning.features.user.dto.AdminUserDto(new com.englishlearning.common.api.ApiId(2),"Student","student@example.invalid","STUDENT",false,false)));
  var service=new AdminUserService(users,accounts);
  when(users.hasBusinessDependencies(2L)).thenReturn(true);
  assertEquals(409,assertThrows(AdminUserService.Failure.class,()->service.delete(1L,2L)).status);
  verify(users,never()).deleteDisposable(anyLong());
  assertEquals(403,assertThrows(AdminUserService.Failure.class,()->service.delete(1L,1L)).status);
  assertEquals(400,assertThrows(AdminUserService.Failure.class,()->service.updateName(1L,2L,Map.of("role","ADMIN"))).status);
  verify(users,never()).updateName(anyLong(),anyString());
  when(users.hasBusinessDependencies(2L)).thenReturn(false);
  org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
  try {service.delete(1L,2L);var order=inOrder(users);order.verify(users,atLeastOnce()).lockParticipants(1L,2L);order.verify(users,atLeastOnce()).hasBusinessDependencies(2L);order.verify(users).deleteDisposable(2L);}
  finally{org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();}
 }
}
