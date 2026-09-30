package com.roommate_connection_platform.security;

import com.roommate_connection_platform.customexceptions.BusinessException;
import com.roommate_connection_platform.ratelimit.LoginIdentityResolver;
import com.roommate_connection_platform.ratelimit.LoginRateLimitException;
import com.roommate_connection_platform.ratelimit.LoginRateLimitProperties;
import com.roommate_connection_platform.ratelimit.LoginRateLimitService;
import com.roommate_connection_platform.securitylog.SecurityEventService;
import com.roommate_connection_platform.securitylog.SecurityEventType;
import com.roommate_connection_platform.services.IUserService;
import com.roommate_connection_platform.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LoginSecurityServiceTest {
    private final IUserService users = mock(IUserService.class);
    private final LoginIdentityResolver identities = mock(LoginIdentityResolver.class);
    private final SecurityEventService events = mock(SecurityEventService.class);
    private final LoginRateLimitService limiter = new LoginRateLimitService(
            new LoginRateLimitProperties(5, 300, 300, 20, 300), new MutableClock());
    private final LoginSecurityService service = new LoginSecurityService(users, identities, limiter, events);

    @BeforeEach
    void resolveIdentity() {
        when(identities.resolve(anyString())).thenReturn("account:1");
    }

    @Test
    void successReturnsOriginalTokensAndClearsFailures() {
        Map<String, String> tokens = Map.of("accessToken", "test-access", "refreshToken", "test-refresh");
        for (int i = 0; i < 4; i++) {
            limiter.recordFailure("ip", "account:1");
        }
        when(users.login("phone", "correct")).thenReturn(tokens);
        assertThat(service.login("phone", "correct", "ip")).isSameAs(tokens);
        verify(events).record(List.of(SecurityEventType.LOGIN_SUCCESS), "phone", "ip");
        assertThat(limiter.recordFailure("ip", "account:1").blocked()).isFalse();
    }

    @Test
    void fifthFailureLogsBothEventsAndFurtherRequestsNeverAuthenticate() {
        when(users.login(anyString(), anyString())).thenThrow(badCredentials());
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> service.login("phone", "wrong", "ip")).isInstanceOf(BusinessException.class);
        }
        assertThatThrownBy(() -> service.login("phone", "wrong", "ip")).isInstanceOf(LoginRateLimitException.class);
        assertThatThrownBy(() -> service.login("phone", "correct", "ip")).isInstanceOf(LoginRateLimitException.class);
        verify(users, times(5)).login(anyString(), anyString());
        verify(events, times(4)).record(List.of(SecurityEventType.LOGIN_FAILED), "phone", "ip");
        verify(events).record(List.of(SecurityEventType.LOGIN_FAILED, SecurityEventType.LOGIN_RATE_LIMITED), "phone", "ip");
        verifyNoMoreInteractions(events);
    }

    @Test
    void caseVariantsResolvingToSameAccountShareTheBlock() {
        when(users.login(anyString(), anyString())).thenThrow(badCredentials());
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> service.login("Phone", "wrong", "ip")).isInstanceOf(BusinessException.class);
        }
        assertThatThrownBy(() -> service.login("PHONE", "wrong", "ip")).isInstanceOf(LoginRateLimitException.class);
        assertThatThrownBy(() -> service.login("phone", "correct", "ip")).isInstanceOf(LoginRateLimitException.class);
        verify(users, times(5)).login(anyString(), anyString());
    }

    @Test
    void systemErrorsDoNotIncreaseCredentialFailures() {
        when(users.login(anyString(), anyString())).thenThrow(new BusinessException(
                "INTERNAL_ERROR", "Test database unavailable", HttpStatus.INTERNAL_SERVER_ERROR));
        for (int i = 0; i < 6; i++) {
            assertThatThrownBy(() -> service.login("phone", "wrong", "ip"))
                    .isInstanceOf(BusinessException.class);
        }
        doThrow(badCredentials()).when(users).login(anyString(), anyString());
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> service.login("phone", "wrong", "ip"))
                    .isInstanceOf(BusinessException.class);
        }
        verify(events, times(4)).record(List.of(SecurityEventType.LOGIN_FAILED), "phone", "ip");
    }

    @Test
    void concurrentAttemptsAuthenticateAtMostFiveTimes() throws Exception {
        when(users.login(anyString(), anyString())).thenThrow(badCredentials());
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<RuntimeException>> futures = new ArrayList<>();
            for (int i = 0; i < 16; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    try {
                        service.login("phone", "wrong", "ip");
                        return null;
                    } catch (RuntimeException ex) {
                        return ex;
                    }
                }));
            }
            start.countDown();
            List<RuntimeException> outcomes = new ArrayList<>();
            for (var future : futures) {
                outcomes.add(future.get(5, TimeUnit.SECONDS));
            }
            assertThat(outcomes.stream().filter(BusinessException.class::isInstance).count()).isEqualTo(4);
            assertThat(outcomes.stream().filter(LoginRateLimitException.class::isInstance).count()).isEqualTo(12);
            verify(users, times(5)).login(anyString(), anyString());
            verify(events).record(List.of(SecurityEventType.LOGIN_FAILED, SecurityEventType.LOGIN_RATE_LIMITED), "phone", "ip");
        }
    }

    private BusinessException badCredentials() {
        return new BusinessException("LOGIN_FAILED", "Wrong phone number or password", HttpStatus.UNAUTHORIZED);
    }
}
