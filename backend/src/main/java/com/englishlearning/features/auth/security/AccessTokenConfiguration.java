package com.englishlearning.features.auth.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class AccessTokenConfiguration {
    @Bean
    public Clock jwtClock() { return Clock.systemUTC(); }

    @Bean
    public AccessTokenIssuer accessTokenIssuer(
            @Value("${app.security.jwt.private-key:}") String privateLocation,
            @Value("${app.security.jwt.public-key:}") String publicLocation,
            @Value("${app.security.jwt.issuer:english-tutoring-platform}") String issuer,
            @Value("${app.security.jwt.audience:english-tutoring-api}") String audience,
            @Value("${app.security.jwt.access-token-lifetime:15m}") Duration lifetime,
            @Qualifier("jwtClock") Clock clock) {
        RSAPrivateKey privateKey;
        RSAPublicKey publicKey;
        try (var privateInput = Files.newInputStream(externalFile(privateLocation));
             var publicInput = Files.newInputStream(externalFile(publicLocation))) {
            privateKey = RsaKeyConverters.pkcs8().convert(privateInput);
            publicKey = RsaKeyConverters.x509().convert(publicInput);
            if (privateKey == null || publicKey == null || privateKey.getModulus().bitLength() < 2048
                    || publicKey.getModulus().bitLength() < 2048
                    || !privateKey.getModulus().equals(publicKey.getModulus())) {
                throw new IllegalArgumentException();
            }
            byte[] challenge = "English Tutoring JWT key-pair check".getBytes(StandardCharsets.US_ASCII);
            var signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(challenge);
            byte[] proof = signature.sign();
            signature.initVerify(publicKey);
            signature.update(challenge);
            if (!signature.verify(proof)) throw new IllegalArgumentException();
        } catch (Exception failure) {
            // Do not attach converter/file exceptions: they may contain PEM data or secret locations.
            throw new IllegalStateException("Invalid JWT signing key configuration.");
        }
        var key = new RSAKey.Builder(publicKey).privateKey(privateKey).build();
        var encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
        return new AccessTokenIssuer(encoder, issuer, audience, lifetime, clock);
    }

    private static Path externalFile(String location) {
        URI uri = URI.create(location);
        if (!"file".equalsIgnoreCase(uri.getScheme())) throw new IllegalArgumentException();
        return Path.of(uri);
    }
}
