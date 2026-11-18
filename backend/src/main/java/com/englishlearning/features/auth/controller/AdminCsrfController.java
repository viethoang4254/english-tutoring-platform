package com.englishlearning.features.auth.controller;
@org.springframework.web.bind.annotation.RestController
public class AdminCsrfController {
 @org.springframework.web.bind.annotation.GetMapping("/api/v1/admin/csrf")
 public org.springframework.http.ResponseEntity<?> token(org.springframework.security.web.csrf.CsrfToken token) {
  return org.springframework.http.ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(java.util.Map.of("token",token.getToken()));
 }
}
