package com.roommate_connection_platform.ratelimit;

import com.roommate_connection_platform.securitylog.SecurityEventService;
import com.roommate_connection_platform.securitylog.SecurityEventType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LoginRateLimitFilter extends OncePerRequestFilter {
    private final LoginRateLimitService rateLimits;
    private final SecurityEventService securityEvents;
    private final LoginRateLimitResponseFactory responses;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path.isEmpty()) {
            path = request.getRequestURI().substring(request.getContextPath().length());
        }
        return !"POST".equals(request.getMethod()) || !"/api/login".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Forwarded headers supplied by the client are deliberately not used.
        String ipAddress = request.getRemoteAddr();
        LoginRateLimitService.Decision decision = rateLimits.countIpRequest(ipAddress);
        if (decision.blocked()) {
            if (decision.newlyBlocked()) {
                securityEvents.record(SecurityEventType.LOGIN_IP_RATE_LIMITED, null, ipAddress);
            }
            responses.write(response,
                    new LoginRateLimitException(LoginRateLimitException.Scope.IP, decision.blockedUntil()));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
