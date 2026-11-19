package com.epam.rd.autocode.assessment.appliances.security;

import com.epam.rd.autocode.assessment.appliances.model.Role;
import com.epam.rd.autocode.assessment.appliances.validation.AccountType;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.session.SessionRegistryImpl;

import static org.assertj.core.api.Assertions.assertThat;

class RegistryUserSessionServiceTest {

    private static AppUserDetails user(Long id, AccountType type) {
        return AppUserDetails.builder()
                .id(id)
                .accountType(type)
                .email("user" + id + "@example.com")
                .name("User")
                .role(Role.USER)
                .password("hash")
                .build();
    }

    @Test
    void expiresOnlySessionsOfTheGivenAccount() {
        SessionRegistryImpl registry = new SessionRegistryImpl();
        AppUserDetails target = user(1L, AccountType.CLIENT);
        AppUserDetails sameIdOtherType = user(1L, AccountType.EMPLOYEE);
        AppUserDetails otherClient = user(2L, AccountType.CLIENT);
        registry.registerNewSession("s1", target);
        registry.registerNewSession("s2", user(1L, AccountType.CLIENT));
        registry.registerNewSession("s3", sameIdOtherType);
        registry.registerNewSession("s4", otherClient);

        new RegistryUserSessionService(registry).expireSessions(AccountType.CLIENT, 1L);

        assertThat(registry.getSessionInformation("s1").isExpired()).isTrue();
        assertThat(registry.getSessionInformation("s2").isExpired()).isTrue();
        assertThat(registry.getSessionInformation("s3").isExpired()).isFalse();
        assertThat(registry.getSessionInformation("s4").isExpired()).isFalse();
    }

    @Test
    void doesNothingWhenAccountHasNoSessions() {
        SessionRegistryImpl registry = new SessionRegistryImpl();
        registry.registerNewSession("s1", user(2L, AccountType.CLIENT));

        new RegistryUserSessionService(registry).expireSessions(AccountType.CLIENT, 9L);

        assertThat(registry.getSessionInformation("s1").isExpired()).isFalse();
    }
}
