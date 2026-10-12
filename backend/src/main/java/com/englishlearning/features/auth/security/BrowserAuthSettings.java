package com.englishlearning.features.auth.security;

import java.net.URI;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public final class BrowserAuthSettings {
    public static final String PATH = "/api/v1/auth";
    public static final String REFRESH_COOKIE = "refresh_token";
    private final List<String> origins;
    private final boolean localHttp;

    public BrowserAuthSettings(
            @Value("${app.security.browser.allowed-origins:}") List<String> origins,
            @Value("${app.security.browser.local-http:false}") boolean localHttp) {
        this.origins = origins.stream().map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
        this.localHttp = localHttp;
        for (String origin : this.origins) {
            try {
                URI uri = URI.create(origin);
                if (origin.contains("*") || uri.getHost() == null || uri.getRawUserInfo() != null
                        || uri.getRawQuery() != null || uri.getRawFragment() != null
                        || (uri.getRawPath() != null && !uri.getRawPath().isEmpty())
                        || uri.getPort() < -1 || uri.getPort() > 65535
                        || (localHttp ? !("http".equals(uri.getScheme()) && loopback(uri.getHost()))
                                      : !"https".equals(uri.getScheme()))) {
                    throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException invalid) {
                throw new IllegalArgumentException("Invalid browser security configuration.");
            }
        }
    }
    public List<String> origins() { return origins; }
    public boolean localHttp() { return localHttp; }
    public boolean secure() { return !localHttp; }
    public static boolean loopback(String host) {
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)
                || "::1".equals(host) || "[::1]".equals(host) || "0:0:0:0:0:0:0:1".equals(host);
    }
}
