package com.englishlearning.features.auth.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.englishlearning.features.auth.service.VerificationFailure;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

@SpringBootTest(properties={
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        "spring.flyway.enabled=false", "app.security.browser.local-http=true",
        "app.security.browser.allowed-origins=http://localhost:3000"})
class TeacherRegistrationHttpTests extends AuthenticationHttpTestSupport {
    String origin(){return "http://localhost:3000";}
    String api(){return "http://localhost:8080";}
    boolean secure(){return false;}
    Map<String,Object> input() {
        return new HashMap<>(Map.of("email"," Teacher@example.invalid ","password"," test password ",
                "fullName","Teacher","specialization"," English ","teachingExperience"," Experience ","introduction"," Intro "));
    }
    @Test void exactResponsePreservesTextAndDoesNotIssueTokens() throws Exception {
        var response=mvc.perform(csrf(postAuth("register/teacher"),bootstrap()).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(input()))).andExpect(status().isCreated())
                .andExpect(content().json("{\"verificationRequired\":true}"))
                .andExpect(header().string("Cache-Control","no-store")).andReturn().getResponse();
        assertEquals(1,mapper.readTree(response.getContentAsString()).size());
        assertNull(refreshHeader(response.getHeaders("Set-Cookie")));
        verify(registration).registerTeacher("Teacher@example.invalid"," test password ","Teacher"," English "," Experience "," Intro ");
        verifyNoInteractions(workflows);
    }
    @ParameterizedTest @ValueSource(strings={"specialization","teachingExperience","introduction"})
    void applicationFieldsRejectMissingNullBlankNulAndWrongTypes(String field) throws Exception {
        for(Object value:new Object[]{null,""," ","bad\0text",12,true,List.of("English")}) {
            var body=input();body.put(field,value);
            mvc.perform(csrf(postAuth("register/teacher"),bootstrap()).contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(body))).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        }
        var body=input();body.remove(field);
        mvc.perform(csrf(postAuth("register/teacher"),bootstrap()).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body))).andExpect(status().isBadRequest());
        verifyNoInteractions(registration);
    }
    @ParameterizedTest @ValueSource(strings={"role","status","teacherId","reviewedBy","emailVerified","locked","unknown"})
    void protectedFieldsAreRejected(String field) throws Exception {
        var body=input();body.put(field,"APPROVED");
        mvc.perform(csrf(postAuth("register/teacher"),bootstrap()).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body))).andExpect(status().isBadRequest());
        verifyNoInteractions(registration);
    }
    @Test void studentAccountValidationIsPreserved() throws Exception {
        for(var change:List.of(Map.entry("email","bad"),Map.entry("password","short"),
                Map.entry("password","a".repeat(65)),Map.entry("password","é".repeat(37)),
                Map.entry("fullName"," "),Map.entry("fullName","bad\0name"))) {
            var body=input();body.put(change.getKey(),change.getValue());
            mvc.perform(csrf(postAuth("register/teacher"),bootstrap()).contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(body))).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(registration);
    }
    @Test void noUnapprovedTextLengthLimit() throws Exception {
        var body=input();body.put("introduction","a".repeat(10000));
        mvc.perform(csrf(postAuth("register/teacher"),bootstrap()).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body))).andExpect(status().isCreated());
    }
    @Test void csrfCorsAndQueryProtectionPrecedeMutation() throws Exception {
        mvc.perform(postAuth("register/teacher").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(postAuth("register/teacher").header("Origin","https://evil.invalid")
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        mvc.perform(csrf(postAuth("register/teacher").queryParam("role","ADMIN"),bootstrap())
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(input())))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(registration);
    }
    @Test void sharedConflictAndDeliveryErrorsRemainSafe() throws Exception {
        var reasons=new VerificationFailure.Reason[]{VerificationFailure.Reason.CONFLICT,VerificationFailure.Reason.DELIVERY};
        int[] statuses={409,503};String[] codes={"REGISTRATION_CONFLICT","VERIFICATION_DELIVERY_UNAVAILABLE"};
        for(int i=0;i<reasons.length;i++) {
            doThrow(new VerificationFailure(reasons[i])).when(registration).registerTeacher(any(),any(),any(),any(),any(),any());
            var response=mvc.perform(csrf(postAuth("register/teacher"),bootstrap()).contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(input()))).andExpect(status().is(statuses[i]))
                    .andExpect(jsonPath("$.code").value(codes[i])).andReturn().getResponse();
            assertFalse(response.getContentAsString().contains("example.invalid"));
            assertFalse(response.getContentAsString().contains("test password"));
        }
    }
}
