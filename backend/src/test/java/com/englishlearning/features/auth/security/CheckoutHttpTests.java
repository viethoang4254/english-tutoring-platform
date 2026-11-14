package com.englishlearning.features.auth.security;

import com.englishlearning.common.api.*;
import com.englishlearning.features.course.dto.*;
import com.englishlearning.features.enrollment.dto.EnrollmentDto;
import com.englishlearning.features.enrollment.repository.EnrollmentRepository;
import com.englishlearning.features.payment.repository.PaymentRepository;
import com.englishlearning.features.payment.dto.PaymentDto;
import com.englishlearning.features.user.entity.User.Role;
import com.englishlearning.features.user.service.AccountSnapshot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.springframework.boot.test.context.SpringBootTest(properties={"spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
    "spring.flyway.enabled=false","app.security.browser.local-http=false","app.security.browser.allowed-origins=https://frontend.example"})
class CheckoutHttpTests extends AuthenticationHttpTestSupport {
    String origin(){return "https://frontend.example";} String api(){return "https://backend.example";} boolean secure(){return true;}
    void actor(Role role,boolean locked) {
        when(decoder.decode("access")).thenReturn(Jwt.withTokenValue("access").header("alg","RS256").subject("1").build());
        when(accounts.findById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L,role,locked,false,true)));
    }
    void fixture() {
        actor(Role.STUDENT,false);
        when(courseRepository.publicFind(2)).thenReturn(Optional.of(new PublicCourseDto(new ApiId(2),"Course","Description",new ApiMoney("100.50","VND"),
            new PublicCourseDto.Category(new ApiId(3),"Category"),new PublicCourseDto.Teacher(new ApiId(9),"Teacher"),LocalDate.of(2030,1,1),"Asia/Ho_Chi_Minh",12,false)));
        when(courseRepository.rules(2)).thenReturn(List.of(new ScheduleDto.Rule(new ApiId(4),1,"19:00","20:00")));
        when(enrollmentRepository.course(2,false)).thenReturn(Optional.of(new EnrollmentRepository.Course(9,"PUBLISHED",new BigDecimal("100.50"),1)));
        when(enrollmentRepository.firstStart(2)).thenReturn(now.plusSeconds(3600));
        when(paymentRepository.price(2)).thenReturn(new PaymentRepository.Price(new BigDecimal("100.50"),"VND",null,null,null));
        when(paymentRepository.destination(9,false)).thenReturn(Optional.of(new PaymentRepository.Destination(8,"970436","Bank","123456","Teacher")));
    }
    EnrollmentDto row(String status,boolean expired) {return new EnrollmentDto(new ApiId(7),new ApiId(2),"Course",status,null,now.plusSeconds(900),expired,null,null,now,now);}
    org.springframework.test.web.servlet.ResultActions checkout()throws Exception {
        return mvc.perform(get("/api/v1/student/courses/2/checkout").header("Authorization","Bearer access"));
    }
    @Test void previewIsStudentOnlyAndNeverExposesBankOrMeet()throws Exception {
        fixture();checkout().andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.available").value(true)).andExpect(jsonPath("$.price.finalAmount").value("101"))
            .andExpect(jsonPath("$.rules[0].startTime").value("19:00")).andExpect(jsonPath("$.calculatedAt").exists())
            .andExpect(jsonPath("$.accountNumber").doesNotExist()).andExpect(jsonPath("$.course.meetUrl").doesNotExist())
            .andExpect(jsonPath("$.paymentId").isEmpty()).andExpect(jsonPath("$.enrollment").isEmpty());
        verify(enrollmentRepository,never()).create(anyLong(),anyLong(),any(),any());
        verify(enrollmentRepository,never()).expirePayments(anyLong(),any());
        verify(enrollmentRepository,never()).forCourse(anyLong(),anyLong(),any());
        verify(paymentRepository,never()).destination(anyLong());
        verifyNoInteractions(credentials);
    }
    @Test void anonymousWrongRoleAndLockedAccountsCannotPreview()throws Exception {
        mvc.perform(get("/api/v1/student/courses/2/checkout")).andExpect(status().isUnauthorized());
        for(Role role:List.of(Role.ADMIN,Role.TEACHER)){actor(role,false);checkout().andExpect(status().isForbidden());}
        actor(Role.STUDENT,true);checkout().andExpect(status().isUnauthorized());
        verifyNoInteractions(enrollmentRepository,paymentRepository,courseRepository);
    }
    @Test void requestCannotSelectAnotherStudentOrSupplyAmounts()throws Exception {
        actor(Role.STUDENT,false);
        for(String query:List.of("studentId=2","amount=1","paymentId=99"))
            mvc.perform(get("/api/v1/student/courses/2/checkout?"+query).header("Authorization","Bearer access")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/student/courses/2/checkout").header("Authorization","Bearer access").content("{}" )).andExpect(status().isBadRequest());
        checkout().andExpect(status().isNotFound());
        verifyNoInteractions(paymentRepository);
    }
    @Test void discountUsesServerWindowAndRounding()throws Exception {
        fixture();when(paymentRepository.price(2)).thenReturn(new PaymentRepository.Price(new BigDecimal("123.45"),"VND",new BigDecimal("12.34"),now,now.plusSeconds(1)));
        checkout().andExpect(status().isOk()).andExpect(jsonPath("$.price.exactDiscountAmount").value("15.23373"))
            .andExpect(jsonPath("$.price.originalAmount").value("123.45"))
            .andExpect(jsonPath("$.price.roundingAdjustment").value("-0.21627"))
            .andExpect(jsonPath("$.price.finalAmount").value("108"));
        when(clock.instant()).thenReturn(now.plusSeconds(1));
        checkout().andExpect(status().isOk()).andExpect(jsonPath("$.price.discountPercent").value("0"))
            .andExpect(jsonPath("$.price.finalAmount").value("123"));
    }
    @Test void validPendingUsesOwnedSnapshotsEvenWhenPriceOrSelectedAccountChanges()throws Exception {
        fixture();when(enrollmentRepository.readForCourse(1,2,now)).thenReturn(Optional.of(row("PENDING",false)));
        when(enrollmentRepository.occupied(2,now)).thenReturn(1L);
        when(paymentRepository.latestId(1,7)).thenReturn(Optional.of(8L));
        when(paymentRepository.own(1,8)).thenReturn(Optional.of(new PaymentDto(new ApiId(8),new ApiId(7),"PENDING","VND","50","0","0","0","50",
            "ELPTEST",new ApiId(9),"970436","Bank","123456","Teacher","payload",now.plusSeconds(900),now)));
        checkout().andExpect(status().isOk()).andExpect(jsonPath("$.available").value(true))
            .andExpect(jsonPath("$.paymentId").value("8")).andExpect(jsonPath("$.price.finalAmount").value("50"));
        verify(paymentRepository,never()).price(anyLong());verify(paymentRepository,never()).destination(anyLong(),anyBoolean());
        verify(paymentRepository).own(1,8);verify(enrollmentRepository).readForCourse(1,2,now);
    }
    @Test void fullStartedAndReceivingUnavailableAreBlocked()throws Exception {
        fixture();when(enrollmentRepository.occupied(2,now)).thenReturn(1L);
        checkout().andExpect(jsonPath("$.available").value(false)).andExpect(jsonPath("$.blockedReason").value("COURSE_FULL"));
        when(enrollmentRepository.occupied(2,now)).thenReturn(0L);when(enrollmentRepository.firstStart(2)).thenReturn(now);
        checkout().andExpect(jsonPath("$.blockedReason").value("COURSE_NOT_ENROLLABLE"));
        when(enrollmentRepository.firstStart(2)).thenReturn(now.plusSeconds(10));when(paymentRepository.destination(9,false)).thenReturn(Optional.empty());
        checkout().andExpect(jsonPath("$.blockedReason").value("RECEIVING_ACCOUNT_UNAVAILABLE"));
    }
    @Test void existingTerminalAndExpiredEnrollmentsRemainReadOnly()throws Exception {
        fixture();for(String state:List.of("ACTIVE","COMPLETED","CANCELLED")) {
            when(enrollmentRepository.readForCourse(1,2,now)).thenReturn(Optional.of(row(state,false)));
            checkout().andExpect(jsonPath("$.available").value(false)).andExpect(jsonPath("$.blockedReason").value("ENROLLMENT_"+state));
        }
        when(enrollmentRepository.readForCourse(1,2,now)).thenReturn(Optional.of(row("PENDING",true)));
        checkout().andExpect(jsonPath("$.blockedReason").value("ENROLLMENT_RENEWAL_REQUIRED"));
        verify(enrollmentRepository,never()).renew(anyLong(),anyLong(),any(),any());
    }
    @Test void persistenceErrorsStaySanitized()throws Exception {
        fixture();when(paymentRepository.price(2)).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("private database details"));
        checkout().andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("ENROLLMENT_UNAVAILABLE"))
            .andExpect(jsonPath("$.message").value("Enrollment request could not be completed."));
    }
}
