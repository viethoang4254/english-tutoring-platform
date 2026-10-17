package com.englishlearning.features.user;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.englishlearning.features.user.service.*;
import com.englishlearning.features.user.repository.StudentProfileRepository;
import com.englishlearning.features.user.entity.User.Role;
import java.time.Clock;
import java.util.*;
import org.junit.jupiter.api.Test;
class StudentProfileServiceTests {
    final StudentProfileRepository profiles=mock(StudentProfileRepository.class);
    final CurrentAccountService accounts=mock(CurrentAccountService.class);
    final UserCredentialService users=mock(UserCredentialService.class);
    final StudentProfileService service=new StudentProfileService(profiles,accounts,users,Clock.systemUTC());
    void allow(){when(users.lockAccountById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L,Role.STUDENT,false,false,true)));}
    @Test void rejectsInvalidFieldsWithoutWriting(){allow();
        var bad=new ArrayList<Map<String,Object>>();bad.add(Map.of());bad.add(Map.of("role","ADMIN"));bad.add(Map.of("fullName"," "));bad.add(Map.of("fullName","a\0b"));bad.add(Map.of("phone","123"));bad.add(Map.of("fullName",42));var nul=new HashMap<String,Object>();nul.put("phone",null);bad.add(nul);
        for(var fields:bad)assertEquals(ProfileFailure.Reason.INVALID,assertThrows(ProfileFailure.class,()->service.update(1L,fields)).reason());verifyNoInteractions(profiles);
    }
    @Test void preservesNameAndNormalizesPhone(){allow();service.update(1L,Map.of("fullName"," Student ","phone","0912345678"));verify(profiles).update(eq(1L),eq(" Student "),eq("+84912345678"),any());}
    @Test void checksRoleAndCurrentEligibility(){
        for(var account:List.of(new AccountSnapshot(1L,Role.TEACHER,false,false,false),new AccountSnapshot(1L,Role.TEACHER,true,true,false),new AccountSnapshot(1L,Role.ADMIN,false,true,false),new AccountSnapshot(1L,Role.STUDENT,true,true,false),new AccountSnapshot(1L,Role.STUDENT,false,false,false))){when(users.lockAccountById(1L)).thenReturn(Optional.of(account));when(accounts.findById(1L)).thenReturn(Optional.of(account));assertThrows(ProfileFailure.class,()->service.update(1L,Map.of("fullName","Name")));assertThrows(ProfileFailure.class,()->service.read(1L));}verifyNoInteractions(profiles);
    }
    @Test void verifiedTeacherUsesOwnAccountWithoutOnboardingDependency(){
        var account=new AccountSnapshot(7L,Role.TEACHER,false,true,false);
        when(accounts.findById(7L)).thenReturn(Optional.of(account));when(users.lockAccountById(7L)).thenReturn(Optional.of(account));
        service.read(7L);service.update(7L,Map.of("fullName"," Teacher ","phone","0912345678"));
        verify(profiles,times(2)).read(7L);verify(profiles).update(eq(7L),eq(" Teacher "),eq("+84912345678"),any());
        verify(profiles,never()).read(1L);
    }
    @Test void teacherCannotBindSecurityOrTeachingFields(){
        when(users.lockAccountById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L,Role.TEACHER,false,true,false)));
        for(String field:List.of("id","teacherId","role","locked","emailVerified","username","email","specialization","teachingExperience","introduction","avatarPath"))
            assertEquals(ProfileFailure.Reason.INVALID,assertThrows(ProfileFailure.class,()->service.update(1L,Map.of("fullName","Teacher",field,"forged"))).reason());
        verifyNoInteractions(profiles);
    }
}
