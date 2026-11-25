package com.epam.rd.autocode.assessment.appliances.security;

import com.epam.rd.autocode.assessment.appliances.service.AuditAction;
import com.epam.rd.autocode.assessment.appliances.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityEventListener {

    private final AuditService auditService;
    private final LoginAttemptService loginAttemptService;

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        loginAttemptService.reset(event.getAuthentication().getName());
        if (event.getAuthentication().getPrincipal() instanceof AppUserDetails user) {
            auditService.record(user.toActor(), AuditAction.LOGGED_IN, "ACCOUNT", user.getId());
        }
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        if (event.getException() instanceof BadCredentialsException) {
            loginAttemptService.recordFailure(event.getAuthentication().getName());
        }
        log.warn("Authentication failed: reason={}", event.getException().getClass().getSimpleName());
    }
}
