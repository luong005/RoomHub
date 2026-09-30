package com.roommate_connection_platform.securitylog;

public enum SecurityEventType {
    LOGIN_SUCCESS("SUCCESS"),
    LOGIN_FAILED("DENIED"),
    LOGIN_RATE_LIMITED("BLOCKED"),
    LOGIN_IP_RATE_LIMITED("BLOCKED");

    private final String status;

    SecurityEventType(String status) {
        this.status = status;
    }

    public String status() {
        return status;
    }

    public boolean isWarning() {
        return "BLOCKED".equals(status);
    }
}
