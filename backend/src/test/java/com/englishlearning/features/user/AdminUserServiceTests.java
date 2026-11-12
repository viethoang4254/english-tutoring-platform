package com.englishlearning.features.user;
import com.englishlearning.features.user.service.*;
import com.englishlearning.features.user.repository.AdminUserRepository;
import com.englishlearning.features.user.entity.User.Role;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AdminUserServiceTests {
 private final com.englishlearning.features.user.repository.StudentProfileRepository profiles =
     mock(com.englishlearning.features.user.repository.StudentProfileRepository.class);
 private final java.time.Clock clock=java.time.Clock.fixed(java.time.Instant.EPOCH,java.time.ZoneOffset.UTC);
 @Test void deniesNonAdminBeforeReadingDirectory(){
  var accounts=mock(CurrentAccountService.class);var users=mock(AdminUserRepository.class);var actor=mock(AccountSnapshot.class);
  when(actor.role()).thenReturn(Role.STUDENT);when(accounts.findById(1L)).thenReturn(Optional.of(actor));
  assertEquals(403,assertThrows(AdminUserService.Failure.class,()->new AdminUserService(users,accounts,profiles,clock).list(1L,0,20,"",null)).status);
  verifyNoInteractions(users);
 }
 @Test void validatesPaginationBeforeQuery(){
  var accounts=mock(CurrentAccountService.class);var users=mock(AdminUserRepository.class);var actor=mock(AccountSnapshot.class);
  when(actor.role()).thenReturn(Role.ADMIN);when(actor.emailVerified()).thenReturn(true);when(accounts.findById(1L)).thenReturn(Optional.of(actor));
  assertEquals(400,assertThrows(AdminUserService.Failure.class,()->new AdminUserService(users,accounts,profiles,clock).list(1L,Integer.MAX_VALUE,100,"",null)).status);
  verifyNoInteractions(users);
 }
 @Test void missingDetailReturns404(){
  var accounts=mock(CurrentAccountService.class);var users=mock(AdminUserRepository.class);var actor=mock(AccountSnapshot.class);
  when(actor.role()).thenReturn(Role.ADMIN);when(actor.emailVerified()).thenReturn(true);when(accounts.findById(1L)).thenReturn(Optional.of(actor));
  when(users.find(9L)).thenReturn(Optional.empty());assertEquals(404,assertThrows(AdminUserService.Failure.class,()->new AdminUserService(users,accounts,profiles,clock).detail(1L,9L)).status);
 }

 @Test void teacherAndStudentCannotLockUsers(){
  for(Role role:List.of(Role.TEACHER,Role.STUDENT)){
   var accounts=mock(CurrentAccountService.class);var users=mock(AdminUserRepository.class);
   var actor=mock(AccountSnapshot.class);when(actor.role()).thenReturn(role);
   when(accounts.findById(1L)).thenReturn(Optional.of(actor));
   assertEquals(403,assertThrows(AdminUserService.Failure.class,()->new AdminUserService(users,accounts,profiles,clock).changeLock(1L,2L,true)).status);
   verify(users,never()).setLocked(anyLong(),anyBoolean());
  }
 }
 @Test void cannotLockSelfOrAnotherAdmin(){
  var accounts=mock(CurrentAccountService.class);var users=mock(AdminUserRepository.class);
  var actor=mock(AccountSnapshot.class);when(actor.role()).thenReturn(Role.ADMIN);when(actor.emailVerified()).thenReturn(true);
  when(accounts.findById(1L)).thenReturn(Optional.of(actor));
  var target=mock(com.englishlearning.features.user.dto.AdminUserDto.class);when(target.role()).thenReturn("ADMIN");
  when(users.find(anyLong())).thenReturn(Optional.of(target));
  for(long id:List.of(1L,2L))assertEquals(403,assertThrows(AdminUserService.Failure.class,()->new AdminUserService(users,accounts,profiles,clock).changeLock(1L,id,true)).status);
  verify(users,never()).setLocked(anyLong(),anyBoolean());
 }
 @Test void lockAndUnlockOnlyChangeRequestedStatus(){
  for(boolean locked:List.of(true,false)){
   var accounts=mock(CurrentAccountService.class);var users=mock(AdminUserRepository.class);
   var actor=mock(AccountSnapshot.class);when(actor.role()).thenReturn(Role.ADMIN);when(actor.emailVerified()).thenReturn(true);
   when(accounts.findById(1L)).thenReturn(Optional.of(actor));
   var target=mock(com.englishlearning.features.user.dto.AdminUserDto.class);when(target.role()).thenReturn("TEACHER");when(target.locked()).thenReturn(!locked);
   when(users.find(2L)).thenReturn(Optional.of(target));
   org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
   try {new AdminUserService(users,accounts,profiles,clock).changeLock(1L,2L,locked);verify(users).setLocked(2L,locked);}
   finally{org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();}
  }
 }
 @Test void profileEditsAllowOnlyValidFullNameForManagedAccounts(){
  var accounts=mock(CurrentAccountService.class); var users=mock(AdminUserRepository.class);
  var service=new AdminUserService(users,accounts,profiles,clock);
  var actor=mock(AccountSnapshot.class);when(accounts.findById(1L)).thenReturn(Optional.of(actor));
  var target=mock(com.englishlearning.features.user.dto.AdminUserDto.class);
  when(users.find(2L)).thenReturn(Optional.of(target));when(target.role()).thenReturn("TEACHER");
  for(Role role:List.of(Role.STUDENT,Role.TEACHER)){
   when(actor.role()).thenReturn(role);
   assertEquals(403,assertThrows(AdminUserService.Failure.class,()->service.updateProfile(1L,2,Map.of("fullName","New"))).status);
  }
  when(actor.role()).thenReturn(Role.ADMIN);when(actor.emailVerified()).thenReturn(true);
  for(String field:List.of("id","role","email","locked","passwordHash","emailVerified","approvalStatus")){
   assertEquals(400,assertThrows(AdminUserService.Failure.class,()->service.updateProfile(1L,2,Map.of("fullName","New",field,"x"))).status);
  }
  for(Object value:List.of("  ","a\0b",42)){
   assertEquals(400,assertThrows(AdminUserService.Failure.class,()->service.updateProfile(1L,2,Map.of("fullName",value))).status);
  }
  verifyNoInteractions(profiles);
  when(target.fullName()).thenReturn("Old");
  org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
  try{
   service.updateProfile(1L,2,Map.of("fullName"," New "));
   verify(profiles).update(2L,"New",null,clock.instant());
  }finally{org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();}
 }
}
