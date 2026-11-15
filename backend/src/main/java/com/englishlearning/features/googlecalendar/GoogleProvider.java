package com.englishlearning.features.googlecalendar;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import tools.jackson.databind.json.JsonMapper;

@Component
public class GoogleProvider {
    private final GoogleSettings settings;
    private final JsonMapper mapper;
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
    private final JwtDecoder decoder;
    private final String tokenEndpoint,revokeEndpoint;
    @org.springframework.beans.factory.annotation.Autowired
    public GoogleProvider(GoogleSettings settings,JsonMapper mapper) {
        this.settings=settings;this.mapper=mapper;
        tokenEndpoint="https://oauth2.googleapis.com/token";revokeEndpoint="https://oauth2.googleapis.com/revoke";
        var factory=new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));factory.setReadTimeout(Duration.ofSeconds(10));
        decoder=NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
            .jwsAlgorithm(SignatureAlgorithm.RS256).restOperations(new org.springframework.web.client.RestTemplate(factory)).build();
    }
    // Package-private transport seam: hosted tests use synthetic, loopback-only OAuth responses.
    GoogleProvider(GoogleSettings settings,JsonMapper mapper,JwtDecoder decoder,String tokenEndpoint,String revokeEndpoint){
        this.settings=settings;this.mapper=mapper;this.decoder=decoder;this.tokenEndpoint=tokenEndpoint;this.revokeEndpoint=revokeEndpoint;
    }
    public record Grant(String subject,String email,String scopes,String refreshToken) {
        @Override public String toString(){return "GoogleGrant[redacted]";}
    }
    static String form(Map<String,String> values){return values.entrySet().stream().map(e->encode(e.getKey())+"="+encode(e.getValue())).collect(java.util.stream.Collectors.joining("&"));}
    static String encode(String s){return URLEncoder.encode(s,StandardCharsets.UTF_8);}
    public String authorize(String state,String verifier,String nonce) {
        return "https://accounts.google.com/o/oauth2/v2/auth?"+form(Map.ofEntries(
            Map.entry("client_id",settings.clientId),Map.entry("redirect_uri",settings.redirectUri),Map.entry("response_type","code"),
            Map.entry("scope","openid email "+GoogleSettings.SCOPE),Map.entry("access_type","offline"),Map.entry("prompt","consent select_account"),
            Map.entry("state",state),Map.entry("nonce",nonce),Map.entry("code_challenge",GoogleCrypto.hash(verifier)),Map.entry("code_challenge_method","S256")));
    }
    public Grant exchange(String code,String verifier,String nonceHash) {
        var result=post(tokenEndpoint,Map.of("client_id",settings.clientId,"client_secret",settings.clientSecret,
            "redirect_uri",settings.redirectUri,"grant_type","authorization_code","code",code,"code_verifier",verifier));
        try {
            if(result.statusCode()!=200)throw new GoogleFailure("RECONNECT_REQUIRED");
            var json=mapper.readTree(result.body());
            String scope=json.path("scope").asText("");
            var scopes=new HashSet<>(Arrays.asList(scope.split(" +")));
            if(!scopes.contains(GoogleSettings.SCOPE)||!scopes.contains("openid")
                ||!(scopes.contains("email")||scopes.contains("https://www.googleapis.com/auth/userinfo.email")))throw new GoogleFailure("MISSING_PERMISSIONS");
            Jwt jwt=decoder.decode(json.path("id_token").asText(""));
            if(!Set.of("https://accounts.google.com","accounts.google.com").contains(jwt.getClaimAsString("iss"))
                ||!jwt.getAudience().contains(settings.clientId)||jwt.getExpiresAt()==null||!jwt.getExpiresAt().isAfter(java.time.Instant.now())
                ||(jwt.getAudience().size()>1&&!settings.clientId.equals(jwt.getClaimAsString("azp")))
                ||(jwt.hasClaim("azp")&&!settings.clientId.equals(jwt.getClaimAsString("azp")))
                ||!nonceHash.equals(GoogleCrypto.hash(Objects.requireNonNull(jwt.getClaimAsString("nonce"))))
                ||!Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")))throw new GoogleFailure("INVALID_RESPONSE");
            String subject=jwt.getSubject(),email=jwt.getClaimAsString("email");
            if(subject==null||subject.isBlank()||subject.length()>255||email==null||email.isBlank()||email.length()>320)throw new GoogleFailure("INVALID_RESPONSE");
            String refresh=json.path("refresh_token").asText("");
            return new Grant(subject,email,scope,refresh.isBlank()?null:refresh);
        }catch(GoogleFailure e){throw e;}catch(Exception e){throw new GoogleFailure("INVALID_RESPONSE");}
    }
    public String refresh(String token){
        var reply=post(tokenEndpoint,Map.of("grant_type","refresh_token","refresh_token",token,"client_id",settings.clientId,"client_secret",settings.clientSecret));
        try{
            var json=mapper.readTree(reply.body());
            if(reply.statusCode()==400&&"invalid_grant".equals(json.path("error").asText()))throw new GoogleFailure("RECONNECT_REQUIRED");
            if(reply.statusCode()!=200||json.path("access_token").asText("").isBlank())throw new GoogleFailure("UNAVAILABLE");
            if(json.has("scope")&&!Arrays.asList(json.path("scope").asText("").split(" +")).contains(GoogleSettings.SCOPE))throw new GoogleFailure("RECONNECT_REQUIRED");
            return json.path("access_token").asText();
        }catch(GoogleFailure e){throw e;}catch(Exception e){throw new GoogleFailure("UNAVAILABLE");}
    }
    public boolean revoke(String token){try{return post(revokeEndpoint,Map.of("token",token)).statusCode()==200;}catch(GoogleFailure e){return false;}}
    private record Reply(int statusCode,byte[] body){}
    private Reply post(String url,Map<String,String> values){
        try {
            var request=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).header("Content-Type","application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form(values))).build();
            var response=http.send(request,HttpResponse.BodyHandlers.ofInputStream());
            try(var body=response.body()){byte[] bytes=body.readNBytes(65537);if(bytes.length>65536)throw new GoogleFailure("UNAVAILABLE");return new Reply(response.statusCode(),bytes);}
        }catch(InterruptedException e){Thread.currentThread().interrupt();throw new GoogleFailure("UNAVAILABLE");}
        catch(Exception e){throw new GoogleFailure("UNAVAILABLE");}
    }
}
