package com.englishlearning.features.payment.controller;

import com.englishlearning.features.auth.security.AccountPrincipal;
import com.englishlearning.features.payment.dto.ReconciliationEvidence;
import com.englishlearning.features.payment.service.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/teacher/courses/{courseId}/payments")
public class ReconciliationController {
    private final ReconciliationService service;
    public ReconciliationController(ReconciliationService service){this.service=service;}
    @GetMapping
    public ResponseEntity<?> queue(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable String courseId,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return ok(service.queue(actor.id(),id(courseId),page,size));
    }
    @GetMapping("/{paymentId}")
    public ResponseEntity<?> detail(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable String courseId,@PathVariable String paymentId) {
        return ok(service.detail(actor.id(),id(courseId),id(paymentId)));
    }
    @GetMapping("/{paymentId}/attempts")
    public ResponseEntity<?> history(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable String courseId,@PathVariable String paymentId,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return ok(service.history(actor.id(),id(courseId),id(paymentId),page,size));
    }
    @PostMapping("/{paymentId}/confirm-manually")
    public ResponseEntity<?> confirm(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable String courseId,@PathVariable String paymentId,
            @RequestBody ReconciliationEvidence evidence) {
        return decide(actor.id(),courseId,paymentId,evidence,false);
    }
    @PostMapping("/{paymentId}/reconcile-late")
    public ResponseEntity<?> late(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable String courseId,@PathVariable String paymentId,
            @RequestBody ReconciliationEvidence evidence) {
        return decide(actor.id(),courseId,paymentId,evidence,true);
    }
    private ResponseEntity<?> decide(long actor,String course,String payment,ReconciliationEvidence evidence,boolean late) {
        var result=service.decide(actor,id(course),id(payment),evidence,late);
        return ResponseEntity.status(result.outcome().equals("SUCCESS")?200:409).cacheControl(CacheControl.noStore()).body(result);
    }
    private static ResponseEntity<?> ok(Object body){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);}
    private static long id(String value) {
        try {if(!value.matches("[1-9][0-9]*"))throw new NumberFormatException();return Long.parseLong(value);}
        catch(NumberFormatException e){throw new PaymentFailure(PaymentFailure.Reason.INVALID_REQUEST);}
    }
}
