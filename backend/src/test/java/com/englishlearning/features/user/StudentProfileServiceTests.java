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
        for(var account:List.of(new AccountSnapshot(1L,Role.TEACHER,false,true,false),new AccountSnapshot(1L,Role.ADMIN,false,true,false),new AccountSnapshot(1L,Role.STUDENT,true,true,false),new AccountSnapshot(1L,Role.STUDENT,false,false,false))){when(users.lockAccountById(1L)).thenReturn(Optional.of(account));assertThrows(ProfileFailure.class,()->service.update(1L,Map.of("fullName","Name")));}verifyNoInteractions(profiles);
    }
}
