package com.roommate_connection_platform.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roommate_connection_platform.components.JwtTokenUtil;
import com.roommate_connection_platform.controller.UserController;
import com.roommate_connection_platform.customexceptions.BusinessException;
import com.roommate_connection_platform.customexceptions.GlobalExceptionHandler;
import com.roommate_connection_platform.filters.JwtTokenFilter;
import com.roommate_connection_platform.security.LoginSecurityService;
import com.roommate_connection_platform.securitylog.SecurityEventService;
import com.roommate_connection_platform.securitylog.SecurityEventType;
import com.roommate_connection_platform.services.IUserService;
import com.roommate_connection_platform.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LoginRateLimitHttpTest {
    private final MutableClock clock = new MutableClock();
    private final ObjectMapper mapper = new ObjectMapper();
    private final IUserService users = mock(IUserService.class);
    private final LoginIdentityResolver identities = mock(LoginIdentityResolver.class);
    private final SecurityEventService events = mock(SecurityEventService.class);
    private final LoginRateLimitService limiter = new LoginRateLimitService(
            new LoginRateLimitProperties(5, 300, 300, 20, 300), clock);
    private final LoginRateLimitResponseFactory responses = new LoginRateLimitResponseFactory(clock, mapper);
    private final LoginRateLimitFilter filter = new LoginRateLimitFilter(limiter, events, responses);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        when(identities.resolve(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(users.login(anyString(), anyString())).thenThrow(
                new BusinessException("LOGIN_FAILED", "Wrong phone number or password", HttpStatus.UNAUTHORIZED));
        JwtTokenFilter jwt = new JwtTokenFilter(mock(UserDetailsService.class), mock(JwtTokenUtil.class));
        ReflectionTestUtils.setField(jwt, "apiPrefix", "api");
        mvc = MockMvcBuilders.standaloneSetup(new UserController(users,
                        new LoginSecurityService(users, identities, limiter, events)))
                .setControllerAdvice(new GlobalExceptionHandler(responses))
                .addFilters(filter, jwt).build();
    }

    @Test
    void fifthFailureAndSubsequentCorrectPasswordReturn429Not401() throws Exception {
        for (int i = 0; i < 4; i++) {
            login("phone", "wrong").andExpect(status().isUnauthorized());
        }
        login("phone", "wrong").andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "300"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.code").value("LOGIN_RATE_LIMITED"))
                .andExpect(jsonPath("$.details.scope").value("IP_PHONE"))
                .andExpect(jsonPath("$.details.retryAfterSeconds").value(300));
        clock.advance(Duration.ofSeconds(120));
        login("phone", "correct").andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "180"));
        verify(users, times(5)).login(anyString(), anyString());
    }

    @Test
    void invalidBodiesCountTowardIpLimitBeforeValidation() throws Exception {
        for (int i = 0; i < 20; i++) {
            mvc.perform(request("/api/login").content("{\"phone_number\":\"\",\"password\":\"\"}")
                            .header("X-Forwarded-For", "203.0.113." + i))
                    .andExpect(status().isBadRequest());
        }
        // Even malformed JSON is blocked without reading/deserializing its body.
        mvc.perform(request("/api/login").content("{invalid"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "300"))
                .andExpect(jsonPath("$.code").value("LOGIN_IP_RATE_LIMITED"))
                .andExpect(jsonPath("$.details.scope").value("IP"))
                .andExpect(jsonPath("$.timestamp").value(clock.instant().toString()));
        login("another-phone", "correct").andExpect(status().isTooManyRequests());
        verify(events).record(SecurityEventType.LOGIN_IP_RATE_LIMITED, null, "127.0.0.1");
        verifyNoInteractions(users, identities);
    }

    @Test
    void successfulResponseStillContainsTheSameTokenFields() throws Exception {
        doReturn(Map.of("accessToken", "test-access", "refreshToken", "test-refresh"))
                .when(users).login("phone", "correct");
        login("phone", "correct").andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test-access"))
                .andExpect(jsonPath("$.refreshToken").value("test-refresh"));
    }

    @Test
    void pairBlockedTrafficStillConsumesIpAllowance() throws Exception {
        for (int i = 0; i < 20; i++) {
            login("phone", "wrong").andExpect(status().is(i < 4 ? 401 : 429));
        }
        login("phone", "wrong").andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.details.scope").value("IP"));
        verify(users, times(5)).login(anyString(), anyString());
    }

    @Test
    void registrationIsUnaffectedWhenLoginIpIsBlocked() throws Exception {
        for (int i = 0; i < 21; i++) {
            limiter.countIpRequest("127.0.0.1");
        }
        mvc.perform(request("/api/register").content(mapper.writeValueAsString(
                        Map.of("phone_number", "registration-test", "password", "test-password"))))
                .andExpect(status().isOk());
        verify(users).register(any());
    }

    @Test
    void expiredPairBlockAllowsAuthenticationAgain() throws Exception {
        for (int i = 0; i < 5; i++) {
            login("phone", "wrong");
        }
        clock.advance(Duration.ofSeconds(300));
        login("phone", "wrong").andExpect(status().isUnauthorized());
        verify(users, times(6)).login(anyString(), anyString());
    }

    @Test
    void preflightAndSimilarPathsDoNotConsumeLoginAllowance() throws Exception {
        AtomicInteger forwarded = new AtomicInteger();
        for (String path : new String[]{"/api/login-extra", "/prefix/api/login", "/api/register"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
            filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> forwarded.incrementAndGet());
        }
        MockHttpServletRequest preflight = new MockHttpServletRequest("OPTIONS", "/api/login");
        filter.doFilter(preflight, new MockHttpServletResponse(), (req, res) -> forwarded.incrementAndGet());
        assertThat(forwarded.get()).isEqualTo(4);
        for (int i = 0; i < 20; i++) {
            assertThat(limiter.countIpRequest("127.0.0.1").blocked()).isFalse();
        }
    }

    @Test
    void retryAfterRoundsUpFractionalSeconds() {
        var response = responses.create(new LoginRateLimitException(LoginRateLimitException.Scope.IP,
                clock.instant().plusMillis(1201)));
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("2");
    }

    private ResultActions login(String phone, String password) throws Exception {
        return mvc.perform(request("/api/login").content(mapper.writeValueAsString(
                Map.of("phone_number", phone, "password", password))));
    }

    private MockHttpServletRequestBuilder request(String path) {
        return post(path).servletPath(path).contentType(MediaType.APPLICATION_JSON);
    }
}
