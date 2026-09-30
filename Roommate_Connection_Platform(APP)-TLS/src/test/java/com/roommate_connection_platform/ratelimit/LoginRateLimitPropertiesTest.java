package com.roommate_connection_platform.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRateLimitPropertiesTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(LoginRateLimitConfiguration.class);

    @Test
    void defaultsMatchTheDemoPolicy() {
        context.run(application -> {
            assertThat(application).hasNotFailed();
            assertThat(application.getBean(LoginRateLimitProperties.class))
                    .isEqualTo(new LoginRateLimitProperties(5, 300, 300, 20, 300));
        });
    }

    @Test
    void nonPositiveConfigurationFailsStartup() {
        context.withPropertyValues("app.security.login.max-failures=0",
                        "app.security.login.ip-window-seconds=-1")
                .run(application -> assertThat(application).hasFailed());
    }
}
