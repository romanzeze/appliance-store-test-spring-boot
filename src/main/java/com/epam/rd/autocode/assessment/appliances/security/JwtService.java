package com.epam.rd.autocode.assessment.appliances.security;

import com.epam.rd.autocode.assessment.appliances.validation.AccountType;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class JwtService {

    public static final String COOKIE_NAME = "AUTH_TOKEN";

    private static final int MIN_SECRET_BYTES = 32;

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Clock clock;
    private final Duration ttl;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.ttl-minutes:30}") long ttlMinutes,
                      Clock clock) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("app.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes long");
        }
        SecretKey key = new SecretKeySpec(bytes, "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        this.decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        this.clock = clock;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    public String issue(AccountType accountType, Long accountId) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(accountType.name() + ":" + accountId)
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    public Optional<TokenSubject> parse(String token) {
        try {
            Jwt jwt = decoder.decode(token);
            String[] parts = jwt.getSubject().split(":", 2);
            return Optional.of(new TokenSubject(AccountType.valueOf(parts[0]), Long.valueOf(parts[1]),
                    jwt.getExpiresAt()));
        } catch (JwtException | IllegalArgumentException | ArrayIndexOutOfBoundsException | NullPointerException e) {
            return Optional.empty();
        }
    }

    public boolean isStale(TokenSubject subject) {
        return subject.expiresAt().isBefore(clock.instant().plus(ttl.dividedBy(2)));
    }

    public void writeCookie(HttpServletRequest request, HttpServletResponse response,
                            AccountType accountType, Long accountId) {
        addCookie(request, response, issue(accountType, accountId), ttl);
    }

    public void clearCookie(HttpServletRequest request, HttpServletResponse response) {
        addCookie(request, response, "", Duration.ZERO);
    }

    private void addCookie(HttpServletRequest request, HttpServletResponse response, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public record TokenSubject(AccountType accountType, Long accountId, Instant expiresAt) {
    }
}
