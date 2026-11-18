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
 @GetMapping public ResponseEntity<?> list(@AuthenticationPrincipal AccountPrincipal actor,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="") String search,@RequestParam(required=false) String role,@RequestParam(required=false) Boolean locked){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.list(actor.id(),page,size,search,role,locked));}
 @GetMapping("/{id}") public ResponseEntity<?> detail(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable long id){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.detail(actor.id(),id));}
 @PatchMapping("/{id}/lock") public ResponseEntity<?> changeLock(@AuthenticationPrincipal AccountPrincipal actor,
     @PathVariable long id,@RequestBody java.util.Map<String,Object> body){
  if(body==null || body.size()!=1 || !(body.get("locked") instanceof Boolean locked))
   throw new AdminUserService.Failure(400);
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.changeLock(actor.id(),id,locked));
 }
 @PatchMapping("/{id}/profile") public ResponseEntity<?> profile(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable long id,@RequestBody java.util.Map<String,Object> fields){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.updateName(actor.id(),id,fields));}
 @DeleteMapping("/{id}") public ResponseEntity<?> delete(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable long id,jakarta.servlet.http.HttpServletRequest request) throws java.io.IOException {
  if(request.getQueryString()!=null||request.getInputStream().read()!=-1)throw new AdminUserService.Failure(400);
  service.delete(actor.id(),id);return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
 }
 @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class) public ResponseEntity<ApiError> conflict(Exception error){return ResponseEntity.status(409).cacheControl(CacheControl.noStore()).body(new ApiError("USER_DEPENDENCIES_EXIST","Account has protected dependencies; deletion was not performed."));}
 @ExceptionHandler({org.springframework.dao.DataAccessException.class,org.springframework.transaction.TransactionException.class})
 public ResponseEntity<ApiError> unavailable(Exception error){return ResponseEntity.status(503).cacheControl(CacheControl.noStore()).body(new ApiError("ADMIN_USERS_UNAVAILABLE","User management is temporarily unavailable."));}
 @ExceptionHandler(AdminUserService.Failure.class) public ResponseEntity<ApiError> failure(AdminUserService.Failure e){String code=switch(e.status){case 401->"AUTHENTICATION_REQUIRED";case 403->"ACCESS_DENIED";case 404->"NOT_FOUND";case 409->"USER_DEPENDENCIES_EXIST";default->"INVALID_REQUEST";};return ResponseEntity.status(e.status).cacheControl(CacheControl.noStore()).body(new ApiError(code,e.status==409?"Account has protected dependencies; deletion was not performed.":"Request could not be processed."));}
}
