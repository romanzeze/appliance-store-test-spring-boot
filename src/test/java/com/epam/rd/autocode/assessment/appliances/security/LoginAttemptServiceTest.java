package com.epam.rd.autocode.assessment.appliances.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    private static final Instant START = Instant.parse("2026-11-19T10:00:00Z");

    private static final class MutableClock extends Clock {

        private Instant now = START;

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }
    }

    @Test
    void locksAfterMaxFailuresAndUnlocksAfterDuration() {
        MutableClock clock = new MutableClock();
        LoginAttemptService service = new LoginAttemptService(clock, 3, 10);

        service.recordFailure("A@x.com");
        service.recordFailure("a@x.com");
        assertThat(service.isLocked("a@x.com")).isFalse();
        service.recordFailure(" a@x.com ");
        assertThat(service.isLocked("a@x.com")).isTrue();
        assertThat(service.isLocked("other@x.com")).isFalse();

        clock.advance(Duration.ofMinutes(11));
        assertThat(service.isLocked("a@x.com")).isFalse();
        service.recordFailure("a@x.com");
        assertThat(service.isLocked("a@x.com")).isFalse();
    }

    @Test
    void resetClearsFailures() {
        LoginAttemptService service = new LoginAttemptService(new MutableClock(), 2, 10);
        service.recordFailure("a@x.com");
        service.reset("a@x.com");
        service.recordFailure("a@x.com");

        assertThat(service.isLocked("a@x.com")).isFalse();
    }

    @Test
    void nullEmailIsHandled() {
        LoginAttemptService service = new LoginAttemptService(new MutableClock(), 1, 10);
        service.recordFailure(null);

        assertThat(service.isLocked(null)).isTrue();
    }
}
