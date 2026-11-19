package com.epam.rd.autocode.assessment.appliances.security;

import com.epam.rd.autocode.assessment.appliances.service.UserSessionService;
import com.epam.rd.autocode.assessment.appliances.validation.AccountType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistryUserSessionService implements UserSessionService {

    private final SessionRegistry sessionRegistry;

    @Override
    public void expireSessions(AccountType accountType, Long accountId) {
        List<SessionInformation> sessions = sessionRegistry.getAllPrincipals().stream()
                .filter(AppUserDetails.class::isInstance)
                .map(AppUserDetails.class::cast)
                .filter(principal -> principal.getAccountType() == accountType && principal.getId().equals(accountId))
                .flatMap(principal -> sessionRegistry.getAllSessions(principal, false).stream())
                .toList();
        sessions.forEach(SessionInformation::expireNow);
        log.info("Expired {} session(s) of {}:{}", sessions.size(), accountType, accountId);
    }
}
