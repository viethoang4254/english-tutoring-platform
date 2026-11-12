package com.englishlearning.features.user.controller;
import com.englishlearning.features.user.service.AdminUserService;
import com.englishlearning.features.auth.security.AccountPrincipal;
import com.englishlearning.common.api.ApiError;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {
 private final AdminUserService service;
 public AdminUserController(AdminUserService service){this.service=service;}
 @GetMapping public ResponseEntity<?> list(@AuthenticationPrincipal AccountPrincipal actor,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="") String search,@RequestParam(required=false) String role){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.list(actor.id(),page,size,search,role));}
 @GetMapping("/{id}") public ResponseEntity<?> detail(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable long id){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.detail(actor.id(),id));}
 @PatchMapping(value="/{id}/profile",consumes=MediaType.APPLICATION_JSON_VALUE)
 public ResponseEntity<?> updateProfile(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable long id,
                                        @RequestBody java.util.Map<String,Object> fields) {
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.updateProfile(actor.id(),id,fields));
 }
 @PatchMapping("/{id}/lock") public ResponseEntity<?> changeLock(@AuthenticationPrincipal AccountPrincipal actor,
     @PathVariable long id,@RequestBody java.util.Map<String,Object> body){
  if(body==null || body.size()!=1 || !(body.get("locked") instanceof Boolean locked))
   throw new AdminUserService.Failure(400);
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.changeLock(actor.id(),id,locked));
 }
 @ExceptionHandler(AdminUserService.Failure.class) public ResponseEntity<ApiError> failure(AdminUserService.Failure e){String code=switch(e.status){case 401->"AUTHENTICATION_REQUIRED";case 403->"ACCESS_DENIED";case 404->"NOT_FOUND";default->"INVALID_REQUEST";};return ResponseEntity.status(e.status).cacheControl(CacheControl.noStore()).body(new ApiError(code,"Request could not be processed."));}
}
