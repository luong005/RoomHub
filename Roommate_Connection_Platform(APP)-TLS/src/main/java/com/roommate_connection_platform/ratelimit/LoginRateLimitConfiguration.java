package com.roommate_connection_platform.ratelimit;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LoginRateLimitProperties.class)
public class LoginRateLimitConfiguration {
    @Bean
    public Clock loginSecurityClock() {
        return Clock.systemUTC();
    }
}
