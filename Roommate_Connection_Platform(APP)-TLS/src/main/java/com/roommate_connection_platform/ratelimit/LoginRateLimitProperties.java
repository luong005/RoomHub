package com.roommate_connection_platform.ratelimit;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.security.login")
public record LoginRateLimitProperties(
        @DefaultValue("5") @Min(1) int maxFailures,
        @DefaultValue("300") @Min(1) int failureWindowSeconds,
        @DefaultValue("300") @Min(1) int blockDurationSeconds,
        @DefaultValue("20") @Min(1) int ipMaxRequests,
        @DefaultValue("300") @Min(1) int ipWindowSeconds) {
}
