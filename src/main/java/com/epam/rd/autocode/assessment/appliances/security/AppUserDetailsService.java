package com.epam.rd.autocode.assessment.appliances.security;

import com.epam.rd.autocode.assessment.appliances.model.User;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.repository.EmployeeRepository;
import com.epam.rd.autocode.assessment.appliances.service.support.EmailNormalizer;
import com.epam.rd.autocode.assessment.appliances.validation.AccountType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final Clock clock;
    private final InMemoryAccountService inMemoryAccountService;

    @Override
    @Transactional(readOnly = true)
    public AppUserDetails loadUserByUsername(String email) {
        String normalized = EmailNormalizer.normalize(email);
        if (normalized == null || normalized.isEmpty()) {
            throw new UsernameNotFoundException("Account not found");
        }
        Optional<User> employee = employeeRepository.findByEmailIgnoreCaseAndDeletedFalse(normalized)
                .map(User.class::cast);
        if (employee.isPresent()) {
            return toDetails(employee.get(), AccountType.EMPLOYEE);
        }
        return clientRepository.findByEmailIgnoreCaseAndDeletedFalse(normalized)
                .map(client -> toDetails(client, AccountType.CLIENT))
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
    }

    @Transactional(readOnly = true)
    public AppUserDetails loadByAccount(AccountType accountType, Long id) {
        if (accountType == AccountType.IN_MEMORY) {
            return inMemoryAccountService.findById(id)
                    .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
        }
        Optional<? extends User> user = accountType == AccountType.CLIENT
                ? clientRepository.findByIdAndDeletedFalse(id)
                : employeeRepository.findByIdAndDeletedFalse(id);
        return user.map(found -> toDetails(found, accountType))
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
    }

    private AppUserDetails toDetails(User user, AccountType accountType) {
        LocalDateTime expiresAt = user.getPasswordExpiresAt();
        boolean expired = user.isMustChangePassword() && expiresAt != null
                && expiresAt.isBefore(LocalDateTime.now(clock));
        return AppUserDetails.builder()
                .id(user.getId())
                .accountType(accountType)
                .name(user.getName())
                .email(user.getEmail())
                .password(user.getPassword())
                .role(user.getRole())
                .blocked(user.isBlocked())
                .mustChangePassword(user.isMustChangePassword())
                .credentialsExpired(expired)
                .build();
    }
}
