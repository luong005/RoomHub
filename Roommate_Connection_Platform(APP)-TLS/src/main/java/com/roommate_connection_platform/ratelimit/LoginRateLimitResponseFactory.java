package com.roommate_connection_platform.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roommate_connection_platform.customexceptions.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Component
public class LoginRateLimitResponseFactory {
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public LoginRateLimitResponseFactory(@Qualifier("loginSecurityClock") Clock clock,
                                         ObjectMapper objectMapper) {
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    public ResponseEntity<ApiError> create(LoginRateLimitException exception) {
        Instant now = clock.instant();
        Duration remaining = Duration.between(now, exception.getBlockedUntil());
        long retryAfter = Math.max(1, remaining.getSeconds() + (remaining.getNano() > 0 ? 1 : 0));
        ApiError error = new ApiError(now, HttpStatus.TOO_MANY_REQUESTS.value(), exception.getCode(),
                exception.getMessage(), Map.of("retryAfterSeconds", retryAfter,
                "scope", exception.getScope().name()));
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(retryAfter))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(error);
    }

    public void write(HttpServletResponse response, LoginRateLimitException exception) throws IOException {
        ResponseEntity<ApiError> result = create(exception);
        ApiError error = result.getBody();
        // The project's Jackson 2 mapper has no Java time module. Use the ISO string
        // here without altering the mapper used by unrelated application features.
        byte[] json = objectMapper.writeValueAsBytes(Map.of(
                "timestamp", error.getTimestamp().toString(),
                "status", error.getStatus(), "code", error.getCode(),
                "message", error.getMessage(), "details", error.getDetails()));
        response.setStatus(result.getStatusCode().value());
        result.getHeaders().forEach((name, values) -> values.forEach(value -> response.addHeader(name, value)));
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getOutputStream().write(json);
    }
}
