package com.englishlearning.features.auth.security;

import com.englishlearning.common.api.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.json.JsonMapper;

final class ApiSecurityErrors {
    private ApiSecurityErrors() {}

    static AuthenticationEntryPoint unauthorized(JsonMapper mapper) {
        return (request, response, exception) -> {
            if (exception instanceof AccountLookupFailure failure) {
                if (failure.isUnavailable()) {
                    write(mapper, response, 503, "AUTHENTICATION_UNAVAILABLE", "Authentication temporarily unavailable.");
                } else {
                    write(mapper, response, 500, "INTERNAL_ERROR", "An unexpected error occurred.");
                }
                return;
            }
            response.setHeader("WWW-Authenticate", "Bearer");
            write(mapper, response, 401, "AUTHENTICATION_REQUIRED", "Authentication required.");
        };
    }

    static AccessDeniedHandler forbidden(JsonMapper mapper) {
        return (request, response, exception) ->
                write(mapper, response, 403, "ACCESS_DENIED", "Access denied.");
    }

    private static void write(JsonMapper mapper, HttpServletResponse response, int status,
            String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(mapper.writeValueAsString(new ApiError(code, message)));
    }
}
