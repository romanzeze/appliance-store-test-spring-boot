package com.epam.rd.autocode.assessment.appliances.security;

import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.model.Role;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.repository.EmployeeRepository;
import com.epam.rd.autocode.assessment.appliances.service.ServiceTestSupport;
import com.epam.rd.autocode.assessment.appliances.validation.AccountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserDetailsServiceTest {

    @Mock
    private ClientRepository clientRepository;
    @Mock
    private EmployeeRepository employeeRepository;

    private AppUserDetailsService service;

    @BeforeEach
    void setUp() {
        service = new AppUserDetailsService(clientRepository, employeeRepository, ServiceTestSupport.CLOCK,
                new InMemoryAccountService("support@memory.test", "Mem0ry!Support", "Support", Role.ADMIN,
                        new BCryptPasswordEncoder(4)));
    }

    @Test
    void loadsEmployeeBeforeClient() {
        Employee employee = new Employee(5L, "Kobe", "kobe@example.com", "hash", "Sales");
        employee.setRole(Role.ADMIN);
        when(employeeRepository.findByEmailIgnoreCaseAndDeletedFalse("kobe@example.com"))
                .thenReturn(Optional.of(employee));

        AppUserDetails details = service.loadUserByUsername("  Kobe@Example.com ");

        assertThat(details.getId()).isEqualTo(5L);
        assertThat(details.getAccountType()).isEqualTo(AccountType.EMPLOYEE);
        assertThat(details.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ADMIN");
        assertThat(details.isAccountNonLocked()).isTrue();
    }

    @Test
    void loadsClientWhenNoEmployeeMatches() {
        Client client = new Client(3L, "Magic", "magic@example.com", "hash", "1234");
        when(employeeRepository.findByEmailIgnoreCaseAndDeletedFalse("magic@example.com"))
                .thenReturn(Optional.empty());
        when(clientRepository.findByEmailIgnoreCaseAndDeletedFalse("magic@example.com"))
                .thenReturn(Optional.of(client));

        AppUserDetails details = service.loadUserByUsername("magic@example.com");

        assertThat(details.getAccountType()).isEqualTo(AccountType.CLIENT);
        assertThat(details.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_USER");
    }

    @Test
    void blockedAccountIsLocked() {
        Client client = new Client(3L, "Magic", "magic@example.com", "hash", "1234");
        client.setBlocked(true);
        when(employeeRepository.findByEmailIgnoreCaseAndDeletedFalse("magic@example.com"))
                .thenReturn(Optional.empty());
        when(clientRepository.findByEmailIgnoreCaseAndDeletedFalse("magic@example.com"))
                .thenReturn(Optional.of(client));

        assertThat(service.loadUserByUsername("magic@example.com").isAccountNonLocked()).isFalse();
    }

    @Test
    void expiredTemporaryPasswordMarksCredentialsExpired() {
        Employee employee = new Employee(6L, "Larry", "larry@example.com", "hash", "Sales");
        employee.setMustChangePassword(true);
        employee.setPasswordExpiresAt(LocalDateTime.of(2026, 11, 18, 10, 0));
        when(employeeRepository.findByEmailIgnoreCaseAndDeletedFalse("larry@example.com"))
                .thenReturn(Optional.of(employee));

        AppUserDetails details = service.loadUserByUsername("larry@example.com");

        assertThat(details.isCredentialsNonExpired()).isFalse();
        assertThat(details.isMustChangePassword()).isTrue();
    }

    @Test
    void validTemporaryPasswordStillRequiresChange() {
        Employee employee = new Employee(6L, "Larry", "larry@example.com", "hash", "Sales");
        employee.setMustChangePassword(true);
        employee.setPasswordExpiresAt(LocalDateTime.of(2026, 11, 20, 10, 0));
        when(employeeRepository.findByEmailIgnoreCaseAndDeletedFalse("larry@example.com"))
                .thenReturn(Optional.of(employee));

        AppUserDetails details = service.loadUserByUsername("larry@example.com");

        assertThat(details.isCredentialsNonExpired()).isTrue();
        assertThat(details.isMustChangePassword()).isTrue();
    }

    @Test
    void unknownEmailThrows() {
        when(employeeRepository.findByEmailIgnoreCaseAndDeletedFalse("none@example.com"))
                .thenReturn(Optional.empty());
        when(clientRepository.findByEmailIgnoreCaseAndDeletedFalse("none@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("none@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
        assertThatThrownBy(() -> service.loadUserByUsername("  "))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void loadByAccountFindsActiveAccountOnly() {
        Client client = new Client(3L, "Magic", "magic@example.com", "hash", "1234");
        when(clientRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(client));
        when(clientRepository.findByIdAndDeletedFalse(4L)).thenReturn(Optional.empty());

        assertThat(service.loadByAccount(AccountType.CLIENT, 3L).getId()).isEqualTo(3L);
        assertThatThrownBy(() -> service.loadByAccount(AccountType.CLIENT, 4L))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void loadByAccountResolvesInMemoryAccountWithoutDatabase() {
        AppUserDetails details = service.loadByAccount(AccountType.IN_MEMORY, InMemoryAccountService.ACCOUNT_ID);

        assertThat(details.getEmail()).isEqualTo("support@memory.test");
        assertThat(details.isInMemory()).isTrue();
        assertThatThrownBy(() -> service.loadByAccount(AccountType.IN_MEMORY, 9L))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
