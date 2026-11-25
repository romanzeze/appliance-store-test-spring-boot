package com.epam.rd.autocode.assessment.appliances.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<String> token = tokenOf(request);
        if (token.isPresent()) {
            authenticate(token.get(), request, response);
        }
        chain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request, HttpServletResponse response) {
        Optional<JwtService.TokenSubject> subject = jwtService.parse(token);
        if (subject.isEmpty()) {
            jwtService.clearCookie(request, response);
            return;
        }
        try {
            AppUserDetails user = userDetailsService.loadByAccount(subject.get().accountType(), subject.get().accountId());
            if (!user.isAccountNonLocked()) {
                log.info("Rejected token of blocked account {}:{}", user.getAccountType(), user.getId());
                jwtService.clearCookie(request, response);
                return;
            }
            user.eraseCredentials();
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
            SecurityContextHolder.setContext(context);
            if (jwtService.isStale(subject.get())) {
                jwtService.writeCookie(request, response, user.getAccountType(), user.getId());
            }
        } catch (UsernameNotFoundException exception) {
            jwtService.clearCookie(request, response);
        }
    }

    private Optional<String> tokenOf(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (JwtService.COOKIE_NAME.equals(cookie.getName()) && !cookie.getValue().isEmpty()) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }
}
