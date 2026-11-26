package com.epam.rd.autocode.assessment.appliances.security;

import com.epam.rd.autocode.assessment.appliances.model.Role;
import com.epam.rd.autocode.assessment.appliances.validation.AccountType;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryAccountServiceTest {

    private static final String PASSWORD = "Mem0ry!Support";

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

    private InMemoryAccountService service(String email, String password) {
        return new InMemoryAccountService(email, password, "Support", Role.ADMIN, encoder);
    }

    @Test
    void loadsConfiguredAccountByEmailIgnoringCase() {
        AppUserDetails details = service("support@memory.test", PASSWORD).loadUserByUsername(" Support@Memory.TEST ");

        assertThat(details.getId()).isEqualTo(InMemoryAccountService.ACCOUNT_ID);
        assertThat(details.getAccountType()).isEqualTo(AccountType.IN_MEMORY);
        assertThat(details.getName()).isEqualTo("Support");
        assertThat(details.getRole()).isEqualTo(Role.ADMIN);
        assertThat(encoder.matches(PASSWORD, details.getPassword())).isTrue();
        assertThat(details.getAuthorities()).extracting(Object::toString)
                .containsExactly("ROLE_ADMIN", InMemoryAccountService.AUTHORITY);
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
        assertThat(details.isMustChangePassword()).isFalse();
    }

    @Test
    void unknownEmailIsNotFound() {
        InMemoryAccountService service = service("support@memory.test", PASSWORD);

        assertThatThrownBy(() -> service.loadUserByUsername("other@memory.test"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void findsAccountOnlyByItsReservedId() {
        InMemoryAccountService service = service("support@memory.test", PASSWORD);

        assertThat(service.findById(InMemoryAccountService.ACCOUNT_ID)).isPresent();
        assertThat(service.findById(5L)).isEmpty();
        assertThat(service.isReserved("SUPPORT@memory.test")).isTrue();
        assertThat(service.isReserved("other@memory.test")).isFalse();
    }

    @Test
    void blankEmailDisablesTheAccount() {
        InMemoryAccountService service = service("", "");

        assertThat(service.isEnabled()).isFalse();
        assertThat(service.isReserved("")).isFalse();
        assertThat(service.findById(InMemoryAccountService.ACCOUNT_ID)).isEmpty();
        assertThatThrownBy(() -> service.loadUserByUsername("support@memory.test"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void enabledAccountRequiresStrongPassword() {
        assertThatThrownBy(() -> service("support@memory.test", ""))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service("support@memory.test", "weakpass"))
                .isInstanceOf(IllegalStateException.class);
    }
}
