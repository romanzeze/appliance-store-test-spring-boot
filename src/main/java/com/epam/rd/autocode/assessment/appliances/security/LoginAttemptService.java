package com.epam.rd.autocode.assessment.appliances.security;

import com.epam.rd.autocode.assessment.appliances.service.support.EmailNormalizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final int CLEANUP_THRESHOLD = 10_000;

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int maxAttempts;
    private final Duration lockDuration;

    public LoginAttemptService(Clock clock,
                               @Value("${app.security.max-login-attempts:5}") int maxAttempts,
                               @Value("${app.security.lockout-minutes:15}") long lockoutMinutes) {
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.lockDuration = Duration.ofMinutes(lockoutMinutes);
    }

    public boolean isLocked(String email) {
        Attempt attempt = attempts.get(key(email));
        return attempt != null && attempt.isLockedAt(clock.instant());
    }

    public void recordFailure(String email) {
        if (attempts.size() > CLEANUP_THRESHOLD) {
            Instant now = clock.instant();
            attempts.values().removeIf(attempt -> !attempt.isLockedAt(now));
        }
        attempts.compute(key(email), (key, current) -> next(current));
    }

    public void reset(String email) {
        attempts.remove(key(email));
    }

    private Attempt next(Attempt current) {
        Instant now = clock.instant();
        boolean fresh = current == null || (current.lockedUntil() != null && !current.lockedUntil().isAfter(now));
        int failures = fresh ? 1 : current.failures() + 1;
        return new Attempt(failures, failures >= maxAttempts ? now.plus(lockDuration) : null);
    }

    private String key(String email) {
        String normalized = EmailNormalizer.normalize(email);
        return normalized == null ? "" : normalized;
    }

    private record Attempt(int failures, Instant lockedUntil) {

        boolean isLockedAt(Instant now) {
            return lockedUntil != null && lockedUntil.isAfter(now);
        }
    }
}
