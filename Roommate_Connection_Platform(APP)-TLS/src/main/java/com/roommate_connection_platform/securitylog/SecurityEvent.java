package com.roommate_connection_platform.securitylog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "security_events", indexes = {
        @Index(name = "idx_security_events_created_at", columnList = "created_at"),
        @Index(name = "idx_security_events_ip_created", columnList = "ip_address,created_at"),
        @Index(name = "idx_security_events_phone_created", columnList = "phone_number,created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SecurityEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "event_type", nullable = false, length = 32)
    private SecurityEventType eventType;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(nullable = false, length = 100)
    private String endpoint;

    @Column(name = "created_at", nullable = false, columnDefinition = "datetime(6)")
    private LocalDateTime createdAt;

    @Column(length = 255)
    private String message;

    public static SecurityEvent login(SecurityEventType type, String phoneNumber,
                                      String ipAddress, Instant occurredAt) {
        SecurityEvent event = new SecurityEvent();
        event.eventType = type;
        event.status = type.status();
        event.phoneNumber = sanitize(phoneNumber, 50);
        event.ipAddress = sanitize(ipAddress == null ? "unknown" : ipAddress, 45);
        event.endpoint = "/api/login";
        event.createdAt = LocalDateTime.ofInstant(occurredAt, ZoneOffset.UTC);
        event.message = switch (type) {
            case LOGIN_SUCCESS -> "Login completed";
            case LOGIN_FAILED -> "Invalid phone number or password";
            case LOGIN_RATE_LIMITED -> "Login failure limit reached for IP and phone number";
            case LOGIN_IP_RATE_LIMITED -> "Login request limit reached for IP";
        };
        return event;
    }

    private static String sanitize(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll("[\\p{Cntrl}\\p{Zl}\\p{Zp}]", " ").strip();
        return cleaned.codePointCount(0, cleaned.length()) <= maxLength ? cleaned
                : cleaned.substring(0, cleaned.offsetByCodePoints(0, maxLength));
    }
}
