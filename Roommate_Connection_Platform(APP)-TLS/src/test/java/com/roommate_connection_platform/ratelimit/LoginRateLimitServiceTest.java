package com.roommate_connection_platform.ratelimit;

import com.roommate_connection_platform.support.MutableClock;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRateLimitServiceTest {
    private final MutableClock clock = new MutableClock();
    private final LoginRateLimitService limiter = new LoginRateLimitService(
            new LoginRateLimitProperties(5, 300, 300, 20, 300), clock);

    @Test
    void fifthFailureStartsItsOwnBlockWhichOutlivesObservationWindow() {
        Instant start = clock.instant();
        for (int i = 0; i < 4; i++) {
            assertThat(limiter.recordFailure("ip", "phone").blocked()).isFalse();
            clock.advance(Duration.ofMinutes(1));
        }
        var blocked = limiter.recordFailure("ip", "phone");
        assertThat(blocked.newlyBlocked()).isTrue();
        assertThat(blocked.blockedUntil()).isEqualTo(start.plusSeconds(540));

        clock.advance(Duration.ofMinutes(1));
        limiter.removeExpiredEntries();
        assertThat(limiter.checkLogin("ip", "phone").blockedUntil()).isEqualTo(start.plusSeconds(540));
        assertThat(limiter.recordFailure("ip", "phone").newlyBlocked()).isFalse();
        clock.advance(Duration.ofMinutes(4));
        assertThat(limiter.checkLogin("ip", "phone").blocked()).isFalse();
        assertThat(limiter.recordFailure("ip", "phone").blocked()).isFalse();
    }

    @Test
    void observationWindowResetsExactlyAtItsBoundary() {
        for (int i = 0; i < 4; i++) {
            limiter.recordFailure("ip", "phone");
        }
        clock.advance(Duration.ofSeconds(300));
        for (int i = 0; i < 4; i++) {
            assertThat(limiter.recordFailure("ip", "phone").blocked()).isFalse();
        }
        assertThat(limiter.recordFailure("ip", "phone").newlyBlocked()).isTrue();
    }

    @Test
    void ipAllowsTwentyThenBlocksUntilOriginalWindowEndsAndWarnsOnce() {
        Instant start = clock.instant();
        for (int i = 0; i < 20; i++) {
            assertThat(limiter.countIpRequest("ip").blocked()).isFalse();
        }
        clock.advance(Duration.ofSeconds(90));
        var blocked = limiter.countIpRequest("ip");
        assertThat(blocked.newlyBlocked()).isTrue();
        assertThat(blocked.blockedUntil()).isEqualTo(start.plusSeconds(300));
        for (int i = 0; i < 100; i++) {
            assertThat(limiter.countIpRequest("ip").newlyBlocked()).isFalse();
        }
        clock.advance(Duration.ofSeconds(210));
        assertThat(limiter.countIpRequest("ip").blocked()).isFalse();
    }

    @Test
    void successfulLoginResetsOnlyItsFailureCounter() {
        for (int i = 0; i < 4; i++) {
            limiter.recordFailure("ip", "phone");
        }
        for (int i = 0; i < 20; i++) {
            limiter.countIpRequest("ip");
        }
        limiter.recordSuccess("ip", "phone");
        assertThat(limiter.recordFailure("ip", "phone").blocked()).isFalse();
        assertThat(limiter.countIpRequest("ip").blocked()).isTrue();
    }

    @Test
    void differentIpsAndIdentitiesHaveSeparateFailureCounters() {
        for (int i = 0; i < 5; i++) {
            limiter.recordFailure("ip1", "phone1");
        }
        assertThat(limiter.checkLogin("ip1", "phone1").blocked()).isTrue();
        assertThat(limiter.checkLogin("ip2", "phone1").blocked()).isFalse();
        assertThat(limiter.checkLogin("ip1", "phone2").blocked()).isFalse();
    }

    @Test
    void concurrentIpRequestsAdmitExactlyTwentyAndStartOnlyOneBlock() throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<LoginRateLimitService.Decision>> futures = new ArrayList<>();
            for (int i = 0; i < 64; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return limiter.countIpRequest("ip");
                }));
            }
            start.countDown();
            List<LoginRateLimitService.Decision> decisions = new ArrayList<>();
            for (var future : futures) {
                decisions.add(future.get(5, TimeUnit.SECONDS));
            }
            assertThat(decisions.stream().filter(d -> !d.blocked()).count()).isEqualTo(20);
            assertThat(decisions.stream().filter(LoginRateLimitService.Decision::newlyBlocked).count()).isEqualTo(1);
        }
    }

    @Test
    void cleanupDoesNotEraseFreshWindowsAndAProcessRestartClearsCounters() {
        limiter.recordFailure("ip", "phone");
        limiter.countIpRequest("ip");
        clock.advance(Duration.ofSeconds(301));
        limiter.removeExpiredEntries();
        for (int i = 0; i < 5; i++) {
            limiter.recordFailure("ip", "phone");
        }
        limiter.removeExpiredEntries();
        assertThat(limiter.checkLogin("ip", "phone").blocked()).isTrue();
        var restarted = new LoginRateLimitService(new LoginRateLimitProperties(5, 300, 300, 20, 300), clock);
        assertThat(restarted.checkLogin("ip", "phone").blocked()).isFalse();
    }
}
