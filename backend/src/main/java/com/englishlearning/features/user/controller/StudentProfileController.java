package com.englishlearning.features.user.controller;
import com.englishlearning.features.user.service.StudentProfileService;
import com.englishlearning.features.user.service.ProfileFailure;
import com.englishlearning.features.user.dto.StudentProfile;
import com.englishlearning.features.auth.security.AccountPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController
public class StudentProfileController {
    private final StudentProfileService profiles;
    public StudentProfileController(StudentProfileService profiles){this.profiles=profiles;}
    @GetMapping("/api/v1/users/me/profile")
    public ResponseEntity<StudentProfile> read(@AuthenticationPrincipal AccountPrincipal account,HttpServletRequest request)throws IOException{
        noQuery(request);if(request.getInputStream().read()!=-1)throw new ProfileFailure(ProfileFailure.Reason.INVALID);
        return response(profiles.read(account.id()));
    }
    @PatchMapping(value="/api/v1/users/me/profile",consumes=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StudentProfile> update(@AuthenticationPrincipal AccountPrincipal account,@RequestBody Map<String,Object> fields,HttpServletRequest request){
        noQuery(request);return response(profiles.update(account.id(),fields));
    }
    private void noQuery(HttpServletRequest request){if(request.getQueryString()!=null)throw new ProfileFailure(ProfileFailure.Reason.INVALID);}
    private ResponseEntity<StudentProfile> response(StudentProfile profile){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(profile);}
}
