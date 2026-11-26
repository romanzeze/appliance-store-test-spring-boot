package com.epam.rd.autocode.assessment.appliances.security;

import com.epam.rd.autocode.assessment.appliances.model.Role;
import com.epam.rd.autocode.assessment.appliances.service.support.EmailNormalizer;
import com.epam.rd.autocode.assessment.appliances.validation.AccountType;
import com.epam.rd.autocode.assessment.appliances.validation.StrongPasswordValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class InMemoryAccountService implements UserDetailsService {

    public static final Long ACCOUNT_ID = 0L;
    public static final String AUTHORITY = "IN_MEMORY";

    private final InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
    private final String email;
    private final String name;
    private final Role role;

    public InMemoryAccountService(@Value("${app.security.in-memory.email:}") String email,
                                  @Value("${app.security.in-memory.password:}") String password,
                                  @Value("${app.security.in-memory.name:System administrator}") String name,
                                  @Value("${app.security.in-memory.role:ADMIN}") Role role,
                                  PasswordEncoder passwordEncoder) {
        this.email = EmailNormalizer.normalize(email);
        this.name = name;
        this.role = role;
        if (isEnabled()) {
            if (password == null || password.isEmpty() || !new StrongPasswordValidator().isValid(password, null)) {
                throw new IllegalStateException("app.security.in-memory.password must be a strong password");
            }
            manager.createUser(User.withUsername(this.email)
                    .password(passwordEncoder.encode(password))
                    .roles(role.name())
                    .build());
            log.info("In-memory account enabled with role {}", role);
        }
    }

    public boolean isEnabled() {
        return email != null && !email.isEmpty();
    }

    public boolean isReserved(String candidate) {
        return isEnabled() && email.equals(EmailNormalizer.normalize(candidate));
    }

    @Override
    public AppUserDetails loadUserByUsername(String username) {
        if (!isReserved(username)) {
            throw new UsernameNotFoundException("Account not found");
        }
        return toDetails(manager.loadUserByUsername(email));
    }

    public Optional<AppUserDetails> findById(Long id) {
        if (!isEnabled() || !ACCOUNT_ID.equals(id)) {
            return Optional.empty();
        }
        return Optional.of(toDetails(manager.loadUserByUsername(email)));
    }

    private AppUserDetails toDetails(UserDetails user) {
        return AppUserDetails.builder()
                .id(ACCOUNT_ID)
                .accountType(AccountType.IN_MEMORY)
                .name(name)
                .email(user.getUsername())
                .password(user.getPassword())
                .role(role)
                .build();
    }
}
