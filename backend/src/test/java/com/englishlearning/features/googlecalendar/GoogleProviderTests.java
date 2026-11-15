package com.englishlearning.features.googlecalendar;

import static org.junit.jupiter.api.Assertions.*;
import com.sun.net.httpserver.HttpServer;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import tools.jackson.databind.json.JsonMapper;

class GoogleProviderTests {
    @TempDir Path folder;
    HttpServer server;GoogleProvider provider;KeyPair keys;String response;String request;int status=200;
    final JsonMapper mapper=JsonMapper.builder().build();
    @BeforeEach void setup()throws Exception{
        var generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);keys=generator.generateKeyPair();
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",exchange->{request=new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);
            byte[] body=response.getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/json");
            exchange.sendResponseHeaders(status,body.length);try(var output=exchange.getResponseBody()){output.write(body);}});
        server.start();String endpoint="http://127.0.0.1:"+server.getAddress().getPort()+"/";
        provider=new GoogleProvider(GoogleTestSettings.create(folder),mapper,NimbusJwtDecoder.withPublicKey((RSAPublicKey)keys.getPublic()).build(),endpoint,endpoint);
    }
    @AfterEach void close(){if(server!=null)server.stop(0);}
    String jwt(String issuer,String audience,String nonce,boolean verified,Instant expiry)throws Exception{
        var claims=new JWTClaimsSet.Builder().issuer(issuer).audience(audience).subject("synthetic-subject").expirationTime(Date.from(expiry))
            .issueTime(Date.from(Instant.now())).claim("nonce",nonce).claim("email","synthetic@example.test").claim("email_verified",verified).build();
        var jwt=new SignedJWT(new JWSHeader(JWSAlgorithm.RS256),claims);jwt.sign(new RSASSASigner(keys.getPrivate()));return jwt.serialize();
    }
    void token(String id,String scopes)throws Exception{response=mapper.writeValueAsString(Map.of("id_token",id,"scope",scopes,"refresh_token","synthetic-refresh"));}
    @Test void offlinePkceAndVerifiedIdentity()throws Exception{
        String url=provider.authorize("state","verifier","nonce");
        assertTrue(url.startsWith("https://accounts.google.com/o/oauth2/v2/auth?"));
        assertTrue(url.contains("code_challenge_method=S256"));assertTrue(url.contains("access_type=offline"));assertFalse(url.contains("synthetic-secret"));
        token(jwt("https://accounts.google.com","synthetic-client","nonce",true,Instant.now().plusSeconds(300)),"openid email "+GoogleSettings.SCOPE);
        var grant=provider.exchange("synthetic-code","synthetic-verifier",GoogleCrypto.hash("nonce"));
        assertEquals("synthetic-subject",grant.subject());assertEquals("synthetic@example.test",grant.email());
        assertTrue(request.contains("code_verifier=synthetic-verifier"));assertFalse(grant.toString().contains("synthetic-refresh"));
    }
    @Test void rejectsWrongIssuerAudienceNonceExpiryAndUnverifiedEmail()throws Exception{
        for(int variant=0;variant<5;variant++){
            token(jwt(variant==0?"https://attacker.example":"https://accounts.google.com",variant==1?"other":"synthetic-client",
                variant==2?"other":"nonce",variant!=3,Instant.now().plusSeconds(variant==4?-300:300)),"openid email "+GoogleSettings.SCOPE);
            assertThrows(GoogleFailure.class,()->provider.exchange("code","verifier",GoogleCrypto.hash("nonce")));
        }
    }
    @Test void invalidSignatureAndMissingPermissionsAreDenied()throws Exception{
        token("not-a-signed-id-token","openid email "+GoogleSettings.SCOPE);
        assertThrows(GoogleFailure.class,()->provider.exchange("code","verifier",GoogleCrypto.hash("nonce")));
        token(jwt("https://accounts.google.com","synthetic-client","nonce",true,Instant.now().plusSeconds(300)),"openid email");
        assertEquals("MISSING_PERMISSIONS",assertThrows(GoogleFailure.class,()->provider.exchange("code","verifier",GoogleCrypto.hash("nonce"))).code);
    }
    @Test void revokedAndDowngradedRefreshCredentialsRequireReconnection(){
        response="{\"error\":\"invalid_grant\"}";status=400;
        assertEquals("RECONNECT_REQUIRED",assertThrows(GoogleFailure.class,()->provider.refresh("synthetic-refresh")).code);
        response="{\"access_token\":\"synthetic-access\",\"scope\":\"openid email\"}";status=200;
        assertEquals("RECONNECT_REQUIRED",assertThrows(GoogleFailure.class,()->provider.refresh("synthetic-refresh")).code);
        response="{}";assertTrue(provider.revoke("synthetic-refresh"));status=503;assertFalse(provider.revoke("synthetic-refresh"));
    }
}
