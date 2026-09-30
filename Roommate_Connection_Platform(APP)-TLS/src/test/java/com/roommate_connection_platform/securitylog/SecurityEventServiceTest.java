package com.roommate_connection_platform.securitylog;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.roommate_connection_platform.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SecurityEventServiceTest {
    private final SecurityEventRepository repository = mock(SecurityEventRepository.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final MutableClock clock = new MutableClock();
    private final SecurityEventService service = new SecurityEventService(repository, transactions, clock);

    @BeforeEach
    void transactionStatus() {
        when(transactions.getTransaction(any())).thenAnswer(invocation -> new SimpleTransactionStatus());
    }

    @Test
    void failureAndBlockAreSavedInOneIndependentTransaction() {
        service.record(List.of(SecurityEventType.LOGIN_FAILED, SecurityEventType.LOGIN_RATE_LIMITED), "phone", "127.0.0.1");
        ArgumentCaptor<TransactionDefinition> definition = ArgumentCaptor.forClass(TransactionDefinition.class);
        verify(transactions).getTransaction(definition.capture());
        assertThat(definition.getValue().getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        verify(transactions).commit(any());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<SecurityEvent>> saved = ArgumentCaptor.forClass(Iterable.class);
        verify(repository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(SecurityEvent::getEventType)
                .containsExactly(SecurityEventType.LOGIN_FAILED, SecurityEventType.LOGIN_RATE_LIMITED);
    }

    @Test
    void databaseFailureDoesNotEscapeOrExposeItsRawMessage() {
        Logger logger = (Logger) LoggerFactory.getLogger(SecurityEventService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            when(repository.saveAll(any())).thenThrow(new DataAccessResourceFailureException("password=do-not-print"));
            assertThatCode(() -> service.record(SecurityEventType.LOGIN_FAILED, "phone", "127.0.0.1"))
                    .doesNotThrowAnyException();
            verify(transactions).rollback(any());
            assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage)
                    .noneMatch(message -> message.contains("do-not-print"));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void commitFailureIsCaughtOutsideTransactionCallback() {
        doThrow(new TransactionSystemException("test commit failure")).when(transactions).commit(any());
        assertThatCode(() -> service.record(SecurityEventType.LOGIN_SUCCESS, "phone", "127.0.0.1"))
                .doesNotThrowAnyException();
        verify(repository).saveAll(any());
    }

    @Test
    void auditValuesAreSanitizedBoundedAndRecordedInUtc() {
        SecurityEvent event = SecurityEvent.login(SecurityEventType.LOGIN_FAILED,
                "phone\r\n" + "😀".repeat(60), "127.0.0.1\r\nforged", clock.instant());
        assertThat(event.getPhoneNumber()).doesNotContain("\r", "\n");
        assertThat(event.getPhoneNumber().codePointCount(0, event.getPhoneNumber().length())).isEqualTo(50);
        assertThat(event.getIpAddress()).doesNotContain("\r", "\n");
        assertThat(event.getCreatedAt()).isEqualTo(LocalDateTime.parse("2026-09-09T12:00:00"));
        assertThat(event.getMessage()).isEqualTo("Invalid phone number or password");
        assertThat(SecurityEvent.login(SecurityEventType.LOGIN_IP_RATE_LIMITED, null, "::1", clock.instant())
                .getPhoneNumber()).isNull();
    }
}
