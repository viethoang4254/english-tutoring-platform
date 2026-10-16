package com.englishlearning.features.auth.security;

import com.englishlearning.common.api.ApiError;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.*;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.cors.*;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

final class BrowserSecurityFilters {
    private BrowserSecurityFilters() {}
    static CorsFilter cors(BrowserAuthSettings browser, JsonMapper mapper) {
        var configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(browser.origins());
        configuration.setAllowCredentials(true);
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "Authorization", "X-XSRF-TOKEN"));
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        var filter = new CorsFilter(source);
        filter.setCorsProcessor(new DefaultCorsProcessor() {
            @Override protected void rejectRequest(ServerHttpResponse response) throws IOException {
                response.setStatusCode(HttpStatus.FORBIDDEN);
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                response.getHeaders().setCacheControl("no-store");
                response.getBody().write(mapper.writeValueAsString(
                        new ApiError("ACCESS_DENIED", "Access denied.")).getBytes(StandardCharsets.UTF_8));
                response.flush();
            }
        });
        return filter;
    }
    static OncePerRequestFilter transport(BrowserAuthSettings browser, JsonMapper mapper) {
        return new OncePerRequestFilter() {
            @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                    FilterChain chain) throws ServletException, IOException {
                response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
                boolean allowed = browser.localHttp()
                        ? !request.isSecure() && "http".equals(request.getScheme())
                            && BrowserAuthSettings.loopback(request.getServerName())
                            && BrowserAuthSettings.loopback(request.getRemoteAddr())
                        : request.isSecure();
                if (!allowed) {
                    response.setStatus(403);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write(mapper.writeValueAsString(new ApiError("ACCESS_DENIED", "Access denied.")));
                    return;
                }
                chain.doFilter(request, response);
            }
        };
    }
}
