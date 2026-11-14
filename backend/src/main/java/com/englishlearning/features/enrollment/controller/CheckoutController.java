package com.englishlearning.features.enrollment.controller;

import com.englishlearning.features.auth.security.AccountPrincipal;
import com.englishlearning.features.enrollment.service.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class CheckoutController {
    private final CheckoutService checkout;
    public CheckoutController(CheckoutService checkout) { this.checkout=checkout; }
    @GetMapping("/api/v1/student/courses/{courseId}/checkout")
    public ResponseEntity<?> get(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable String courseId,HttpServletRequest request)throws IOException {
        long id;
        try {
            if(!courseId.matches("[1-9][0-9]*") || !request.getParameterMap().isEmpty() || request.getInputStream().read()!=-1)throw new NumberFormatException();
            id=Long.parseLong(courseId);
        } catch(NumberFormatException failure) {throw new EnrollmentFailure(EnrollmentFailure.Reason.INVALID_REQUEST);}
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(checkout.get(actor.id(),id));
    }
}
