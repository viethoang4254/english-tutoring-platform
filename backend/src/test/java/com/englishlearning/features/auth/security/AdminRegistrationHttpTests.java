package com.englishlearning.features.auth.security;

import com.englishlearning.features.auth.service.VerificationFailure;
import com.englishlearning.features.user.entity.User.Role;
import com.englishlearning.features.user.service.AccountSnapshot;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminRegistrationHttpTests extends AuthenticationHttpTestSupport {
    String origin(){return "https://frontend.example";}
    String api(){return "https://backend.example";}
    boolean secure(){return true;}
    void actor(Role role){
        when(decoder.decode("access")).thenReturn(Jwt.withTokenValue("access").header("alg","RS256").subject("2").build());
        when(accounts.findById(2L)).thenReturn(Optional.of(new AccountSnapshot(2L,role,false,true,true)));
    }
    Map<String,String> student(){
        return new HashMap<>(Map.of("email","user@example.invalid","fullName","Test","role","STUDENT","password","fixture-pass","confirmPassword","fixture-pass","username","test_user","phone","0912345678"));
    }
    org.springframework.test.web.servlet.ResultActions create(Map<String,String> input)throws Exception{
        return mvc.perform(post("/api/v1/admin/users").header("Authorization","Bearer access").contentType("application/json").content(mapper.writeValueAsString(input)));
    }
    @Test void createsStudentWithMinimalResponse()throws Exception{
        actor(Role.ADMIN);create(student()).andExpect(status().isCreated()).andExpect(content().string("{\"verificationRequired\":false}"));
        verify(registration).register(2L,"user@example.invalid","fixture-pass","Test","test_user","+84912345678");
    }
    @Test void teacherRequiresOnboardingApproval()throws Exception{
        actor(Role.ADMIN);var data=student();data.put("role","TEACHER");data.remove("username");data.remove("phone");
        data.put("specialization","English");data.put("teachingExperience","Teaching");data.put("introduction","Hello");
        create(data).andExpect(status().isCreated()).andExpect(jsonPath("$.verificationRequired").value(false));
        verify(registration).registerTeacher(2L,"user@example.invalid","fixture-pass","Test","English","Teaching","Hello");
    }
    @Test void rejectsStudentsAndTeachers()throws Exception{
        for(var role:List.of(Role.STUDENT,Role.TEACHER)){actor(role);create(student()).andExpect(status().isForbidden());}
        verifyNoInteractions(registration);
    }
    @Test void rejectsEscalationAndInvalidPasswords()throws Exception{
        actor(Role.ADMIN);for(var pair:Map.of("role","ADMIN","password","short","confirmPassword","different").entrySet()){
            var data=student();data.put(pair.getKey(),pair.getValue());create(data).andExpect(status().isBadRequest());
        }verifyNoInteractions(registration);
    }
    @Test void duplicateIsConflict()throws Exception{
        actor(Role.ADMIN);doThrow(new VerificationFailure(VerificationFailure.Reason.CONFLICT)).when(registration)
                .register(2L,"user@example.invalid","fixture-pass","Test","test_user","+84912345678");
        create(student()).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("REGISTRATION_CONFLICT"));
    }
}
