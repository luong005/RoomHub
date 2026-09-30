package com.roommate_connection_platform.ratelimit;

import java.time.Instant;

public class LoginRateLimitException extends RuntimeException {
    public enum Scope { IP, IP_PHONE }

    private final Scope scope;
    private final Instant blockedUntil;

    public LoginRateLimitException(Scope scope, Instant blockedUntil) {
        super("Bạn đã thử đăng nhập quá nhiều lần. Vui lòng thử lại sau.");
        this.scope = scope;
        this.blockedUntil = blockedUntil;
    }

    public Scope getScope() {
        return scope;
    }

    public Instant getBlockedUntil() {
        return blockedUntil;
    }

    public String getCode() {
        return scope == Scope.IP ? "LOGIN_IP_RATE_LIMITED" : "LOGIN_RATE_LIMITED";
    }
}
