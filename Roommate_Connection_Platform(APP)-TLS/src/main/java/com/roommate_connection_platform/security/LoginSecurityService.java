package com.roommate_connection_platform.security;

import com.roommate_connection_platform.customexceptions.BusinessException;
import com.roommate_connection_platform.ratelimit.LoginIdentityResolver;
import com.roommate_connection_platform.ratelimit.LoginRateLimitException;
import com.roommate_connection_platform.ratelimit.LoginRateLimitService;
import com.roommate_connection_platform.securitylog.SecurityEventService;
import com.roommate_connection_platform.securitylog.SecurityEventType;
import com.roommate_connection_platform.services.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LoginSecurityService {
    private final IUserService userService;
    private final LoginIdentityResolver identities;
    private final LoginRateLimitService rateLimits;
    private final SecurityEventService securityEvents;

    // No outer transaction: the existing login commits/rolls back before audit writes.
    public Map<String, String> login(String phoneNumber, String password, String ipAddress) {
        String identity = identities.resolve(phoneNumber);
        LoginOutcome outcome = rateLimits.withAttemptLock(ipAddress, identity,
                () -> authenticate(phoneNumber, password, ipAddress, identity));
        // Database audit IO happens after releasing the attempt lock.
        securityEvents.record(outcome.events(), phoneNumber, ipAddress);
        if (outcome.failure() != null) {
            throw outcome.failure();
        }
        return outcome.tokens();
    }

    private LoginOutcome authenticate(String phoneNumber, String password, String ipAddress, String identity) {
        LoginRateLimitService.Decision existingBlock = rateLimits.checkLogin(ipAddress, identity);
        if (existingBlock.blocked()) {
            throw new LoginRateLimitException(LoginRateLimitException.Scope.IP_PHONE, existingBlock.blockedUntil());
        }
        Map<String, String> tokens;
        try {
            tokens = userService.login(phoneNumber, password);
        } catch (BusinessException ex) {
            if (ex.getStatus() != HttpStatus.UNAUTHORIZED || !"LOGIN_FAILED".equals(ex.getCode())) {
                throw ex;
            }
            LoginRateLimitService.Decision decision = rateLimits.recordFailure(ipAddress, identity);
            if (decision.newlyBlocked()) {
                return new LoginOutcome(null,
                        new LoginRateLimitException(LoginRateLimitException.Scope.IP_PHONE, decision.blockedUntil()),
                        List.of(SecurityEventType.LOGIN_FAILED, SecurityEventType.LOGIN_RATE_LIMITED));
            }
            return new LoginOutcome(null, ex, List.of(SecurityEventType.LOGIN_FAILED));
        }
        rateLimits.recordSuccess(ipAddress, identity);
        return new LoginOutcome(tokens, null, List.of(SecurityEventType.LOGIN_SUCCESS));
    }

    private record LoginOutcome(Map<String, String> tokens, RuntimeException failure,
                                List<SecurityEventType> events) { }
}
