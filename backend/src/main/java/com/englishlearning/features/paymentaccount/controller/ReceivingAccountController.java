package com.englishlearning.features.paymentaccount.controller;
import com.englishlearning.features.auth.security.AccountPrincipal;
import com.englishlearning.features.paymentaccount.service.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
@RestController
public class ReceivingAccountController {
    private final ReceivingAccountService service;
    public ReceivingAccountController(ReceivingAccountService service){this.service=service;}
    @PostMapping(value="/api/v1/teacher/payment-accounts",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> submit(@AuthenticationPrincipal AccountPrincipal actor,MultipartHttpServletRequest request) throws IOException {
        if(request.getQueryString()!=null || !request.getParameterMap().keySet().equals(Set.of("bankBin","holderName","accountNumber"))
            || request.getParameterMap().values().stream().anyMatch(v->v.length!=1)
            || !request.getMultiFileMap().keySet().equals(Set.of("qrImage")) || request.getFiles("qrImage").size()!=1
            || Collections.list(request.getHeaders("Idempotency-Key")).size()!=1)throw invalid();
        MultipartFile file=request.getFile("qrImage");
        if(file==null||file.isEmpty()||file.getSize()>QrImage.MAX||!Set.of("image/png","image/jpeg").contains(Objects.toString(file.getContentType(),"")))throw invalid();
        byte[] bytes;try(var in=file.getInputStream()){bytes=in.readNBytes(QrImage.MAX+1);}
        var r=service.submit(actor.id(),request.getParameter("bankBin"),request.getParameter("holderName"),request.getParameter("accountNumber"),request.getHeader("Idempotency-Key"),bytes);
        return ResponseEntity.status(r.created()?201:200).location(URI.create("/api/v1/teacher/payment-accounts/"+r.account().id().decimal())).cacheControl(CacheControl.noStore()).body(r.account());
    }
    @GetMapping("/api/v1/teacher/payment-accounts")
    public ResponseEntity<?> list(@AuthenticationPrincipal AccountPrincipal actor,HttpServletRequest r)throws IOException {
        query(r,Set.of("page","size"));return ok(service.list(actor.id(),number(r,"page",0),number(r,"size",20)));
    }
    @GetMapping("/api/v1/teacher/payment-accounts/{id}")
    public ResponseEntity<?> detail(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable String id,HttpServletRequest r)throws IOException {
        query(r,Set.of());return ok(service.detail(actor.id(),id(id)));
    }
    @GetMapping("/api/v1/teacher/payment-accounts/{id}/qr-image")
    public ResponseEntity<byte[]> image(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable String id,HttpServletRequest r)throws IOException {
        query(r,Set.of());return image(service.image(actor.id(),id(id),false));
    }
    @GetMapping("/api/v1/admin/payment-accounts/{id}/qr-image")
    public ResponseEntity<byte[]> reviewImage(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable String id,HttpServletRequest r)throws IOException {
        query(r,Set.of());return image(service.image(actor.id(),id(id),true));
    }
    private ResponseEntity<byte[]> image(byte[] bytes){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG).header("X-Content-Type-Options","nosniff").header("Content-Disposition","inline; filename=bank-qr.png").body(bytes);}
    private ResponseEntity<?> ok(Object body){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);}
    private void query(HttpServletRequest r,Set<String> names)throws IOException{if(!names.containsAll(r.getParameterMap().keySet())||r.getParameterMap().values().stream().anyMatch(v->v.length!=1)||r.getInputStream().read()!=-1)throw invalid();}
    private int number(HttpServletRequest r,String name,int fallback){String v=r.getParameter(name);if(v==null)return fallback;try{if(!v.matches("[0-9]+"))throw invalid();return Integer.parseInt(v);}catch(NumberFormatException e){throw invalid();}}
    private long id(String v){try{if(!v.matches("[1-9][0-9]*"))throw invalid();return Long.parseLong(v);}catch(NumberFormatException e){throw invalid();}}
    private static ReceivingFailure invalid(){return new ReceivingFailure(ReceivingFailure.Reason.INVALID);}
}
