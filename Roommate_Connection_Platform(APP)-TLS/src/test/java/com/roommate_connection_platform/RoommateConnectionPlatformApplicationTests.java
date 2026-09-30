package com.roommate_connection_platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roommate_connection_platform.filters.JwtTokenFilter;
import com.roommate_connection_platform.ratelimit.LoginRateLimitFilter;
import com.roommate_connection_platform.securitylog.SecurityEvent;
import com.roommate_connection_platform.securitylog.SecurityEventRepository;
import com.roommate_connection_platform.securitylog.SecurityEventService;
import com.roommate_connection_platform.securitylog.SecurityEventType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.show-sql=false",
        "logging.level.org.springframework.security=WARN"
})
class RoommateConnectionPlatformApplicationTests {

    // Context verification must not publish scheduled posts in the developer's database.
    @MockitoBean
    private TaskScheduler taskScheduler;

    @MockitoBean
    private SecurityEventService securityEvents;

    @Autowired
    private WebApplicationContext application;
    @Autowired
    private FilterChainProxy springSecurityFilterChain;
    @Autowired
    private FilterRegistrationBean<LoginRateLimitFilter> loginRateLimitFilterRegistration;
    @Autowired
    private SecurityEventRepository securityEventRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void contextLoads() {
    }

    @Test
    void loginFilterIsRegisteredOnceBeforeJwt() {
        var filters = springSecurityFilterChain.getFilterChains().getFirst().getFilters();
        var loginFilters = filters.stream().filter(LoginRateLimitFilter.class::isInstance).toList();
        assertThat(loginFilters).hasSize(1);
        var jwt = filters.stream().filter(JwtTokenFilter.class::isInstance).findFirst().orElseThrow();
        assertThat(filters.indexOf(loginFilters.getFirst())).isLessThan(filters.indexOf(jwt));
        assertThat(loginRateLimitFilterRegistration.isEnabled()).isFalse();
    }

    @Test
    void actualSecurityChainAndMvcReturnLoginRateLimitJson() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(application).apply(springSecurity()).build();
        // Longer than users.phone_number: guaranteed not to identify a real stored account.
        String body = new ObjectMapper().writeValueAsString(Map.of(
                "phone_number", "security-http-test-".repeat(4), "password", "test-only-password"));
        for (int i = 0; i < 5; i++) {
            var response = mvc.perform(post("/api/login").servletPath("/api/login")
                            .with(request -> { request.setRemoteAddr("192.0.2.70"); return request; })
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().is(i < 4 ? 401 : 429));
            if (i == 4) {
                response.andExpect(jsonPath("$.code").value("LOGIN_RATE_LIMITED"))
                        .andExpect(jsonPath("$.details.scope").value("IP_PHONE"))
                        .andExpect(header().exists("Retry-After"));
            }
        }
    }

    @Test
    void standaloneAuditEntityRoundTripsThroughMysqlAndTestInsertRollsBack() {
        long originalCount = securityEventRepository.count();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var event = securityEventRepository.saveAndFlush(SecurityEvent.login(
                    SecurityEventType.LOGIN_FAILED, "security-jpa-test", "192.0.2.71",
                    Instant.parse("2026-09-09T12:00:00.123456Z")));
            assertThat(event.getId()).isNotNull();
            assertThat(jdbc.queryForObject("SELECT event_type FROM security_events WHERE id = ?",
                    String.class, event.getId())).isEqualTo("LOGIN_FAILED");
            assertThat(jdbc.queryForObject("SELECT status FROM security_events WHERE id = ?",
                    String.class, event.getId())).isEqualTo("DENIED");
            status.setRollbackOnly();
        });
        assertThat(securityEventRepository.count()).isEqualTo(originalCount);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'security_events'
                  AND REFERENCED_TABLE_NAME IS NOT NULL
                """, Integer.class)).isZero();
    }

}
