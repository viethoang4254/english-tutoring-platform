package com.englishlearning.features.googlecalendar;

import java.util.*;
import jakarta.servlet.http.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import com.englishlearning.features.auth.security.AccountPrincipal;

@RestController
@RequestMapping(GoogleSettings.PATH)
public class GoogleController {
    private final CalendarSyncStore sync;
    @org.springframework.beans.factory.annotation.Value("${APP_SESSION_REMINDERS_ENABLED:false}")
    private boolean remindersEnabled;
    private final GoogleSettings settings;private final GoogleService service;
    public GoogleController(GoogleSettings settings,GoogleService service,CalendarSyncStore sync){this.sync=sync;this.settings=settings;this.service=service;}
    @GetMapping public ResponseEntity<?> status(@AuthenticationPrincipal AccountPrincipal actor,CsrfToken csrf){
        var connection=service.status(actor.id());
        var body=new HashMap<String,Object>();
        body.put("connection",connection);body.put("csrfToken",csrf.getToken());
        if(settings.syncEnabled && Set.of("CONNECTED","CLEANUP_PENDING").contains(connection.status())) body.put("sync",sync.status(actor.id()));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
    @PostMapping("/sync") public ResponseEntity<?> retry(@AuthenticationPrincipal AccountPrincipal actor,HttpServletRequest request){
        noInput(request);
        if(!Set.of("CONNECTED","CLEANUP_PENDING").contains(service.status(actor.id()).status()) || !settings.syncEnabled)throw new GoogleFailure("UNAVAILABLE");
        sync.retry(actor.id());
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).build();
    }
    @PostMapping("/courses/{course}/invitations")
    public ResponseEntity<?> invite(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable long course,HttpServletRequest request) {
        noInput(request);
        if(!settings.invitationsEnabled || !service.status(actor.id()).synchronizationAvailable()) throw new GoogleFailure("UNAVAILABLE");
        sync.requestInvitations(actor.id(),course);
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(sync.invitationStatus(actor.id(),course));
    }
    @GetMapping("/courses/{course}/overview")
    public ResponseEntity<?> overview(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable long course) {
        var overview=sync.courseOverview(actor.id(),course);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of(
            "eligibleStudents",overview.eligibleStudents(),"sessions",overview.sessions(),
            "invitationsEnabled",settings.invitationsEnabled,"remindersEnabled",remindersEnabled));
    }
    @GetMapping("/courses/{course}/invitations")
    public ResponseEntity<?> invitations(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable long course) {
        if(!settings.invitationsEnabled || !service.status(actor.id()).synchronizationAvailable()) throw new GoogleFailure("UNAVAILABLE");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(sync.invitationStatus(actor.id(),course));
    }
    @PostMapping("/connect") public ResponseEntity<?> connect(@AuthenticationPrincipal AccountPrincipal actor,HttpServletRequest request){
        noInput(request);var start=service.start(actor.id());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header(HttpHeaders.SET_COOKIE,binding(start.browserBinding(),600)).body(Map.of("authorizationUrl",start.authorizationUrl()));
    }
    @DeleteMapping public ResponseEntity<?> disconnect(@AuthenticationPrincipal AccountPrincipal actor,HttpServletRequest request){
        noInput(request);return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header(HttpHeaders.SET_COOKIE,binding("",0)).body(Map.of("disconnected",true,"remoteRevoked",service.disconnect(actor.id())));
    }
    @GetMapping("/callback") public ResponseEntity<?> callback(HttpServletRequest request){
        String result="CONNECTED";
        try{
            if(!Set.of("state","code","error","error_description","error_uri","scope","authuser","prompt","hd","iss").containsAll(request.getParameterMap().keySet())
                ||request.getParameterMap().values().stream().anyMatch(v->v.length!=1)
                ||(request.getParameter("iss")!=null && !"https://accounts.google.com".equals(request.getParameter("iss"))))throw new GoogleFailure("INVALID_RESPONSE");
            var cookies=request.getCookies()==null?List.<Cookie>of():Arrays.asList(request.getCookies());
            var bindings=cookies.stream().filter(c->c.getName().equals("google_oauth_binding")).toList();
            service.complete(request.getParameter("state"),bindings.size()==1?bindings.get(0).getValue():null,request.getParameter("code"),request.getParameter("error"));
        }catch(GoogleFailure e){result=e.code;}catch(Exception e){result="UNAVAILABLE";}
        if(settings.frontend.isBlank())return ResponseEntity.status(503).cacheControl(CacheControl.noStore()).body(Map.of("code","GOOGLE_UNAVAILABLE"));
        return ResponseEntity.status(303).cacheControl(CacheControl.noStore()).header("Referrer-Policy","no-referrer")
            .header(HttpHeaders.SET_COOKIE,binding("",0)).header(HttpHeaders.LOCATION,settings.returnUrl(result)).build();
    }
    @ExceptionHandler(GoogleFailure.class) ResponseEntity<?> failure(GoogleFailure error){
        return ResponseEntity.status(error.code.equals("DENIED")?403:error.code.equals("UNAVAILABLE")?503:409)
            .cacheControl(CacheControl.noStore()).body(Map.of("code","GOOGLE_"+error.code,"message","Google Calendar connection unavailable."));
    }
    private String binding(String value,int age){return ResponseCookie.from("google_oauth_binding",value).httpOnly(true).secure(settings.secure).sameSite("Lax").path(GoogleSettings.PATH+"/callback").maxAge(age).build().toString();}
    private void noInput(HttpServletRequest r){try{if(!r.getParameterMap().isEmpty()||r.getInputStream().read()!=-1)throw new GoogleFailure("INVALID_RESPONSE");}catch(java.io.IOException e){throw new GoogleFailure("INVALID_RESPONSE");}}
}
