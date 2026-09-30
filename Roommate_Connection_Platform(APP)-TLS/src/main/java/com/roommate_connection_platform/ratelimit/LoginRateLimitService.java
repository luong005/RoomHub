package com.roommate_connection_platform.ratelimit;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Service
public class LoginRateLimitService {
    private static final int LOCK_COUNT = 256;
    private final LoginRateLimitProperties properties;
    private final Clock clock;
    private final ConcurrentHashMap<String, IpWindow> ipWindows = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<LoginKey, FailureWindow> failures = new ConcurrentHashMap<>();
    private final ReentrantLock[] attemptLocks = new ReentrantLock[LOCK_COUNT];

    public LoginRateLimitService(LoginRateLimitProperties properties,
                                 @Qualifier("loginSecurityClock") Clock clock) {
        this.properties = properties;
        this.clock = clock;
        for (int i = 0; i < LOCK_COUNT; i++) {
            attemptLocks[i] = new ReentrantLock();
        }
    }

    public record Decision(Instant blockedUntil, boolean newlyBlocked) {
        public boolean blocked() {
            return blockedUntil != null;
        }

        static Decision allowed() {
            return new Decision(null, false);
        }
    }

    private record IpWindow(Instant expiresAt, long requests) { }
    private record LoginKey(String ipAddress, String identity) { }
    private record FailureWindow(Instant expiresAt, int count, Instant blockedUntil) {
        boolean expiredAt(Instant now) {
            // An active block outlives the original observation window.
            return !now.isBefore(blockedUntil != null ? blockedUntil : expiresAt);
        }
    }

    public Decision countIpRequest(String ipAddress) {
        AtomicReference<Decision> decision = new AtomicReference<>(Decision.allowed());
        ipWindows.compute(ipAddress, (key, window) -> {
            Instant now = clock.instant();
            if (window == null || !now.isBefore(window.expiresAt())) {
                return new IpWindow(now.plusSeconds(properties.ipWindowSeconds()), 1);
            }
            if (window.requests() >= properties.ipMaxRequests()) {
                boolean newlyBlocked = window.requests() == properties.ipMaxRequests();
                decision.set(new Decision(window.expiresAt(), newlyBlocked));
                // Saturation avoids overflow; blocked traffic never extends the window.
                return new IpWindow(window.expiresAt(), (long) properties.ipMaxRequests() + 1);
            }
            return new IpWindow(window.expiresAt(), window.requests() + 1);
        });
        return decision.get();
    }

    /** Serializes check, authentication and update for an identity, with bounded lock storage. */
    public <T> T withAttemptLock(String ipAddress, String identity, Supplier<T> attempt) {
        ReentrantLock lock = lockFor(new LoginKey(ipAddress, identity));
        lock.lock();
        try {
            return attempt.get();
        } finally {
            lock.unlock();
        }
    }

    public Decision checkLogin(String ipAddress, String identity) {
        return withAttemptLock(ipAddress, identity, () -> {
            FailureWindow window = currentWindow(new LoginKey(ipAddress, identity), clock.instant());
            return window != null && window.blockedUntil() != null
                    ? new Decision(window.blockedUntil(), false) : Decision.allowed();
        });
    }

    public Decision recordFailure(String ipAddress, String identity) {
        return withAttemptLock(ipAddress, identity, () -> {
            Instant now = clock.instant();
            LoginKey key = new LoginKey(ipAddress, identity);
            FailureWindow previous = currentWindow(key, now);
            if (previous != null && previous.blockedUntil() != null) {
                return new Decision(previous.blockedUntil(), false);
            }
            int count = previous == null ? 1 : previous.count() + 1;
            Instant expiresAt = previous == null
                    ? now.plusSeconds(properties.failureWindowSeconds()) : previous.expiresAt();
            Instant blockedUntil = count >= properties.maxFailures()
                    ? now.plusSeconds(properties.blockDurationSeconds()) : null;
            failures.put(key, new FailureWindow(expiresAt, count, blockedUntil));
            return new Decision(blockedUntil, blockedUntil != null);
        });
    }

    public void recordSuccess(String ipAddress, String identity) {
        withAttemptLock(ipAddress, identity, () -> {
            failures.remove(new LoginKey(ipAddress, identity));
            return null;
        });
    }

    @Scheduled(fixedDelay = 60_000)
    public void removeExpiredEntries() {
        Instant now = clock.instant();
        ipWindows.forEach((ip, window) -> {
            if (!now.isBefore(window.expiresAt())) {
                ipWindows.remove(ip, window);
            }
        });
        failures.forEach((key, window) -> {
            ReentrantLock lock = lockFor(key);
            // Never remove a counter while its authentication attempt is in flight.
            if (lock.tryLock()) {
                try {
                    currentWindow(key, clock.instant());
                } finally {
                    lock.unlock();
                }
            }
        });
    }

    private FailureWindow currentWindow(LoginKey key, Instant now) {
        FailureWindow window = failures.get(key);
        if (window != null && window.expiredAt(now)) {
            failures.remove(key, window);
            return null;
        }
        return window;
    }

    private ReentrantLock lockFor(LoginKey key) {
        return attemptLocks[Math.floorMod(key.hashCode(), LOCK_COUNT)];
    }
}
