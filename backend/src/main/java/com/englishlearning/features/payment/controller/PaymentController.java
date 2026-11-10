package com.englishlearning.features.payment.controller;

import com.englishlearning.features.auth.security.AccountPrincipal;
import com.englishlearning.features.payment.service.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/student")
public class PaymentController {
    private final PaymentService service;
    public PaymentController(PaymentService service) {this.service=service;}
    @PostMapping("/enrollments/{enrollmentId}/payments")
    public ResponseEntity<?> create(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable String enrollmentId,HttpServletRequest request) throws IOException {
        request(request);var result=service.create(actor.id(),id(enrollmentId));
        return (result.created()?ResponseEntity.created(URI.create("/api/v1/student/payments/"+result.payment().id().decimal())):ResponseEntity.ok())
            .cacheControl(CacheControl.noStore()).body(result.payment());
    }
    @GetMapping("/enrollments/{enrollmentId}/payments/latest")
    public ResponseEntity<?> latest(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable String enrollmentId,HttpServletRequest request) throws IOException {
        request(request);
        var latest=service.latestId(actor.id(),id(enrollmentId));
        return latest.isPresent()?ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(java.util.Map.of("id",latest.get().toString())):ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<?> get(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable String paymentId,HttpServletRequest request) throws IOException {
        request(request);return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.get(actor.id(),id(paymentId)));
    }
    @GetMapping(value="/payments/{paymentId}/qr-image",produces=MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> image(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable String paymentId,HttpServletRequest request) throws IOException {
        request(request);var payment=service.get(actor.id(),id(paymentId));
        if(!payment.status().equals("PENDING"))throw new PaymentFailure(PaymentFailure.Reason.ENROLLMENT_NOT_PAYABLE);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff")
            .contentType(MediaType.IMAGE_PNG).body(VietQr.png(payment.qrPayload()));
    }
    private static long id(String value) {
        try {if(!value.matches("[1-9][0-9]*"))throw invalid();return Long.parseLong(value);}catch(NumberFormatException e){throw invalid();}
    }
    private static void request(HttpServletRequest request)throws IOException {
        if(!request.getParameterMap().isEmpty() || request.getInputStream().read()!=-1)throw invalid();
    }
    private static PaymentFailure invalid(){return new PaymentFailure(PaymentFailure.Reason.INVALID_REQUEST);}
}
