package com.englishlearning.features.category.service;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.englishlearning.features.category.repository.CategoryRepository;
import com.englishlearning.features.user.service.*;
import com.englishlearning.features.user.entity.User.Role;
import java.util.*;
import java.time.*;
class CategoryServiceTests {
 CategoryRepository repo=mock(CategoryRepository.class);CurrentAccountService accounts=mock(CurrentAccountService.class);UserCredentialService users=mock(UserCredentialService.class);
 CategoryService service=new CategoryService(repo,accounts,users,Clock.systemUTC());
 @Test void validatesUnicodeAndAsciiTrimming(){assertEquals("École",CategoryService.name("  École  "));assertEquals("😀".repeat(100),CategoryService.name("😀".repeat(100)));for(Object n:List.of("", "   ","a\tb","a\u007fb","a\u0085b","x".repeat(101),"\ud800",true))assertThrows(CategoryFailure.class,()->CategoryService.name(n));assertThrows(CategoryFailure.class,()->CategoryService.name(null));}
 @Test void rejectsUnknownNullAndEmptyUpdates(){for(Map<String,Object> m:List.<Map<String,Object>>of(Map.of(),Map.of("id",1),Map.of("active","true"),Collections.singletonMap("name",null)))assertThrows(CategoryFailure.class,()->CategoryService.validate(m));}
 @Test void serviceRechecksRoleVerificationAndLock(){for(var a:List.of(new AccountSnapshot(1L,Role.STUDENT,false,true,false),new AccountSnapshot(1L,Role.TEACHER,false,true,false),new AccountSnapshot(1L,Role.ADMIN,false,false,false),new AccountSnapshot(1L,Role.ADMIN,true,true,false))){when(accounts.findById(1L)).thenReturn(Optional.of(a));when(users.lockAccountById(1L)).thenReturn(Optional.of(a));assertThrows(CategoryFailure.class,()->service.list(1L,0,20,null));assertThrows(CategoryFailure.class,()->service.create(1L,Map.of("name","Valid")));}verifyNoInteractions(repo);}
 @Test void paginationAndMissingCategory(){var admin=new AccountSnapshot(1L,Role.ADMIN,false,true,false);when(accounts.findById(1L)).thenReturn(Optional.of(admin));when(users.lockAccountById(1L)).thenReturn(Optional.of(admin));for(int size:new int[]{0,101})assertThrows(CategoryFailure.class,()->service.list(1L,0,size,null));assertThrows(CategoryFailure.class,()->service.list(1L,Integer.MAX_VALUE,100,null));when(repo.find(9,true)).thenReturn(Optional.empty());assertEquals(CategoryFailure.Reason.MISSING,assertThrows(CategoryFailure.class,()->service.update(1L,9,Map.of("active",false))).reason());}
}
