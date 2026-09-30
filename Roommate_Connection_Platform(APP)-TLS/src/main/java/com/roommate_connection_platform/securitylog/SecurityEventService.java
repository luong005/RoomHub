package com.roommate_connection_platform.securitylog;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@Slf4j
public class SecurityEventService {
    private final SecurityEventRepository repository;
    private final Clock clock;
    private final TransactionTemplate transaction;

    public SecurityEventService(SecurityEventRepository repository,
                                PlatformTransactionManager transactionManager,
                                @Qualifier("loginSecurityClock") Clock clock) {
        this.repository = repository;
        this.clock = clock;
        this.transaction = new TransactionTemplate(transactionManager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void record(SecurityEventType type, String phoneNumber, String ipAddress) {
        record(List.of(type), phoneNumber, ipAddress);
    }

    public void record(List<SecurityEventType> types, String phoneNumber, String ipAddress) {
        Instant now = clock.instant();
        List<SecurityEvent> events = types.stream()
                .map(type -> SecurityEvent.login(type, phoneNumber, ipAddress, now)).toList();
        for (SecurityEvent event : events) {
            if (event.getEventType().isWarning()) {
                log.warn("event_type={} status={} ip_address={}",
                        event.getEventType(), event.getStatus(), event.getIpAddress());
            } else {
                log.info("event_type={} status={} ip_address={}",
                        event.getEventType(), event.getStatus(), event.getIpAddress());
            }
        }
        try {
            // The catch also covers commit failures, outside the transaction callback.
            transaction.executeWithoutResult(status -> repository.saveAll(events));
        } catch (RuntimeException ex) {
            // Never emit raw exception messages, SQL parameters or credential objects.
            log.warn("Security event persistence failed: event_types={} cause={}",
                    types, ex.getClass().getSimpleName());
        }
    }
}
