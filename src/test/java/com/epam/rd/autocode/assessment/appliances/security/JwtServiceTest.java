package com.epam.rd.autocode.assessment.appliances.security;

import com.epam.rd.autocode.assessment.appliances.validation.AccountType;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-0123456789-abcdef";

    @Test
    void issuedTokenRoundTrips() {
        JwtService service = new JwtService(SECRET, 30, Clock.systemUTC());

        var subject = service.parse(service.issue(AccountType.EMPLOYEE, 7L)).orElseThrow();

        assertThat(subject.accountType()).isEqualTo(AccountType.EMPLOYEE);
        assertThat(subject.accountId()).isEqualTo(7L);
        assertThat(service.isStale(subject)).isFalse();
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtService other = new JwtService("another-secret-key-0123456789-abcdefgh", 30, Clock.systemUTC());
        JwtService service = new JwtService(SECRET, 30, Clock.systemUTC());

        assertThat(service.parse(other.issue(AccountType.CLIENT, 1L))).isEmpty();
        assertThat(service.parse("garbage")).isEmpty();
    }

    @Test
    void expiredTokenIsRejected() {
        Clock past = Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC);
        JwtService service = new JwtService(SECRET, 30, past);

        assertThat(service.parse(service.issue(AccountType.CLIENT, 1L))).isEmpty();
    }

    @Test
    void tokenNearExpiryIsStale() {
        Clock now = Clock.systemUTC();
        JwtService service = new JwtService(SECRET, 30, now);
        var subject = new JwtService.TokenSubject(AccountType.CLIENT, 1L, now.instant().plusSeconds(60));

        assertThat(service.isStale(subject)).isTrue();
    }

    @Test
    void shortSecretIsRejected() {
        assertThatThrownBy(() -> new JwtService("short", 30, Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cookieIsHttpOnlyAndClearable() {
        JwtService service = new JwtService(SECRET, 30, Clock.systemUTC());
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.writeCookie(new MockHttpServletRequest(), response, AccountType.CLIENT, 1L);
        String written = response.getHeader("Set-Cookie");
        service.clearCookie(new MockHttpServletRequest(), response);

        assertThat(written).contains("HttpOnly").contains("Path=/").contains("Max-Age=1800");
        assertThat(response.getHeaders("Set-Cookie").get(1)).contains("Max-Age=0");
    }
}
