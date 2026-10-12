package com.englishlearning.features.auth.security;

import static org.junit.jupiter.api.Assertions.*;
import com.nimbusds.jwt.SignedJWT;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.oauth2.jwt.*;

class AccessTokenIssuerTests {
    static final KeyPair KEYS = keys("RSA", 2048);
    static final Instant NOW = Instant.parse("2026-10-12T00:00:00.987654Z");
    static final String ISSUER = "english-tutoring-platform";
    static final String AUDIENCE = "english-tutoring-api";
    @TempDir Path directory;

    static KeyPair keys(String algorithm, int size) {
        try {
            var generator = KeyPairGenerator.getInstance(algorithm);
            generator.initialize(size);
            return generator.generateKeyPair();
        } catch (Exception failure) { throw new IllegalStateException("Test key generation failed."); }
    }

    String pem(String name, String label, byte[] bytes) throws Exception {
        Path path = directory.resolve(name);
        Files.writeString(path, "-----BEGIN " + label + "-----\n"
                + Base64.getMimeEncoder(64, new byte[]{10}).encodeToString(bytes)
                + "\n-----END " + label + "-----\n");
        return path.toUri().toString();
    }
    String privateFile(KeyPair pair) throws Exception {
        return pem(UUID.randomUUID()+".pem", "PRIVATE KEY", pair.getPrivate().getEncoded());
    }
    String publicFile(KeyPair pair) throws Exception {
        return pem(UUID.randomUUID()+".pem", "PUBLIC KEY", pair.getPublic().getEncoded());
    }
    AccessTokenIssuer issuer(Duration lifetime, Instant now) throws Exception {
        return new AccessTokenConfiguration().accessTokenIssuer(privateFile(KEYS), publicFile(KEYS),
                ISSUER, AUDIENCE, lifetime, Clock.fixed(now, ZoneOffset.UTC));
    }
    JwtDecoder decoder(Instant now) throws Exception {
        return new SecurityConfiguration().jwtDecoder(
                new FileSystemResource(Path.of(java.net.URI.create(publicFile(KEYS)))),
                ISSUER, AUDIENCE, Duration.ofSeconds(60), Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test void actualValidatorAcceptsExactClaimsAndHeader() throws Exception {
        AccessToken token = issuer(Duration.ofMinutes(15), NOW).issue(Long.MAX_VALUE);
        var decoded = decoder(NOW).decode(token.value());
        var parsed = SignedJWT.parse(token.value());
        assertEquals(Set.of("sub", "iss", "aud", "iat", "exp"), parsed.getJWTClaimsSet().getClaims().keySet());
        assertEquals(Set.of("alg", "typ"), parsed.getHeader().toJSONObject().keySet());
        assertEquals("RS256", parsed.getHeader().getAlgorithm().getName());
        assertEquals("JWT", parsed.getHeader().getType().toString());
        assertEquals(Long.toString(Long.MAX_VALUE), decoded.getSubject());
        assertEquals(ISSUER, decoded.getClaimAsString("iss"));
        assertEquals(List.of(AUDIENCE), decoded.getAudience());
        assertEquals(AUDIENCE, parsed.getPayload().toJSONObject().get("aud"));
        assertEquals(Instant.parse("2026-10-12T00:00:00Z"), token.issuedAt());
        assertEquals(token.issuedAt().plusSeconds(900), token.expiresAt());
        assertEquals(token.issuedAt(), decoded.getIssuedAt());
        assertEquals(token.expiresAt(), decoded.getExpiresAt());
        assertEquals("AccessToken[REDACTED]", token.toString());
    }

    @Test void defaultAndConfiguredLifetimeBindAtStartup() throws Exception {
        var runner = new WebApplicationContextRunner()
                .withInitializer(c -> c.getBeanFactory().setConversionService(
                        org.springframework.boot.convert.ApplicationConversionService.getSharedInstance()))
                .withUserConfiguration(AccessTokenConfiguration.class)
                .withPropertyValues("app.security.jwt.private-key=" + privateFile(KEYS),
                        "app.security.jwt.public-key=" + publicFile(KEYS));
        runner.run(context -> {
            assertNull(context.getStartupFailure());
            var token = context.getBean(AccessTokenIssuer.class).issue(1L);
            assertEquals(Duration.ofMinutes(15), Duration.between(token.issuedAt(), token.expiresAt()));
        });
        runner.withPropertyValues("app.security.jwt.access-token-lifetime=5m",
                "app.security.jwt.issuer=custom-issuer", "app.security.jwt.audience=custom-audience")
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    var token = context.getBean(AccessTokenIssuer.class).issue(1L);
                    assertEquals(Duration.ofMinutes(5), Duration.between(token.issuedAt(), token.expiresAt()));
                    var claims = SignedJWT.parse(token.value()).getJWTClaimsSet();
                    assertEquals("custom-issuer", claims.getIssuer());
                    assertEquals(List.of("custom-audience"), claims.getAudience());
                });
    }

    @Test void validationClockToleranceIsUnchanged() throws Exception {
        var token = issuer(Duration.ofMinutes(15), NOW).issue(1L);
        assertDoesNotThrow(() -> decoder(token.expiresAt().plusSeconds(59)).decode(token.value()));
        assertThrows(JwtException.class, () -> decoder(token.expiresAt().plusSeconds(61)).decode(token.value()));
        assertDoesNotThrow(() -> decoder(token.issuedAt().minusSeconds(60)).decode(token.value()));
        assertThrows(JwtException.class, () -> decoder(token.issuedAt().minusSeconds(61)).decode(token.value()));
    }

    @Test void invalidIdentityRejected() throws Exception {
        var issuer = issuer(Duration.ofMinutes(15), NOW);
        for (Long id : Arrays.asList(null, 0L, -1L)) {
            assertEquals("Invalid User identity.", assertThrows(IllegalArgumentException.class, () -> issuer.issue(id)).getMessage());
        }
    }

    @Test void safeSigningFailuresDoNotExposeClaimsOrTokens() {
        var issuer = new AccessTokenIssuer(parameters -> {
            throw new JwtEncodingException("PRIVATE key token claims");
        }, ISSUER, AUDIENCE, Duration.ofMinutes(15), Clock.fixed(NOW, ZoneOffset.UTC));
        var failure = assertThrows(IllegalStateException.class, () -> issuer.issue(1L));
        assertEquals("Access token issuance failed.", failure.getMessage());
        assertNull(failure.getCause());
    }

    @Test void startupRejectsMissingMalformedWeakNonRsaAndMismatchedKeys() throws Exception {
        String validPrivate = privateFile(KEYS), validPublic = publicFile(KEYS);
        String malformed = directory.resolve("malformed.pem").toUri().toString();
        Files.writeString(Path.of(java.net.URI.create(malformed)), "PRIVATE invalid PEM");
        KeyPair weak = keys("RSA", 1024), ec = keys("EC", 256), other = keys("RSA", 2048);
        var publicRsa = (java.security.interfaces.RSAPublicKey) KEYS.getPublic();
        var wrongExponent = KeyFactory.getInstance("RSA").generatePublic(
                new java.security.spec.RSAPublicKeySpec(publicRsa.getModulus(),
                        publicRsa.getPublicExponent().add(java.math.BigInteger.TWO)));
        String sameModulusWrongKey = pem("wrong-exponent.pem", "PUBLIC KEY", wrongExponent.getEncoded());
        for (String[] pair : List.of(
                new String[]{"", validPublic},
                new String[]{directory.resolve("absent.pem").toUri().toString(), validPublic},
                new String[]{validPrivate, ""},
                new String[]{malformed, validPublic},
                new String[]{validPrivate, malformed},
                new String[]{privateFile(weak), publicFile(weak)},
                new String[]{privateFile(ec), publicFile(ec)},
                new String[]{privateFile(KEYS), publicFile(other)},
                new String[]{validPrivate, sameModulusWrongKey},
                new String[]{"classpath:private.pem", validPublic})) {
            new WebApplicationContextRunner()
                .withInitializer(c -> c.getBeanFactory().setConversionService(
                        org.springframework.boot.convert.ApplicationConversionService.getSharedInstance()))
                .withUserConfiguration(AccessTokenConfiguration.class)
                    .withPropertyValues("app.security.jwt.private-key=" + pair[0], "app.security.jwt.public-key=" + pair[1])
                    .run(context -> {
                        assertNotNull(context.getStartupFailure());
                        Throwable root = context.getStartupFailure();
                        while (root.getCause() != null) root = root.getCause();
                        assertEquals("Invalid JWT signing key configuration.", root.getMessage());
                    });
        }
    }

    @Test void invalidLifetimeAndNamesFailStartup() throws Exception {
        var runner = new WebApplicationContextRunner()
                .withInitializer(c -> c.getBeanFactory().setConversionService(
                        org.springframework.boot.convert.ApplicationConversionService.getSharedInstance()))
                .withUserConfiguration(AccessTokenConfiguration.class)
                .withPropertyValues("app.security.jwt.private-key=" + privateFile(KEYS),
                        "app.security.jwt.public-key=" + publicFile(KEYS));
        for (String property : List.of("app.security.jwt.access-token-lifetime=0s",
                "app.security.jwt.access-token-lifetime=-1s", "app.security.jwt.access-token-lifetime=1500ms",
                "app.security.jwt.issuer=", "app.security.jwt.audience=")) {
            runner.withPropertyValues(property).run(context -> assertNotNull(context.getStartupFailure()));
        }
    }

    @Test void replacingKeyPairInvalidatesPreviousTokens() throws Exception {
        var old = issuer(Duration.ofMinutes(15), NOW).issue(1L);
        KeyPair replacement = keys("RSA", 2048);
        var validator = new SecurityConfiguration().jwtDecoder(
                new FileSystemResource(Path.of(java.net.URI.create(publicFile(replacement)))),
                ISSUER, AUDIENCE, Duration.ofSeconds(60), Clock.fixed(NOW, ZoneOffset.UTC));
        assertThrows(JwtException.class, () -> validator.decode(old.value()));
    }
}
