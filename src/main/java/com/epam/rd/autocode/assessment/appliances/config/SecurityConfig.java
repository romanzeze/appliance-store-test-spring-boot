package com.epam.rd.autocode.assessment.appliances.config;

import com.epam.rd.autocode.assessment.appliances.model.Role;
import com.epam.rd.autocode.assessment.appliances.security.AuditLogoutHandler;
import com.epam.rd.autocode.assessment.appliances.security.LoggingAccessDeniedHandler;
import com.epam.rd.autocode.assessment.appliances.security.LoginFailureHandler;
import com.epam.rd.autocode.assessment.appliances.security.PasswordChangeRequiredFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/", "/login", "/register", "/error", "/favicon.ico", "/contact",
            "/css/**", "/js/**", "/images/**", "/webjars/**"
    };

    private static final String CATALOG_PATH = "/appliances";

    private static final String[] ADMIN_PATHS = {
            "/employees/**",
            "/audit/**",
            "/clients/*/erase",
            "/clients/deleted",
            "/clients/*/restore",
            "/manufacturers/deleted",
            "/manufacturers/*/restore",
            "/appliances/deleted",
            "/appliances/*/restore",
            "/orders/deleted",
            "/orders/*/restore"
    };

    private static final String[] STAFF_PATHS = {
            "/clients/**",
            "/manufacturers/**",
            "/appliances/add",
            "/appliances/*/edit",
            "/appliances/*/delete",
            "/orders/*/approve"
    };

    private final LoggingAccessDeniedHandler accessDeniedHandler;
    private final AuditLogoutHandler logoutHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public static RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
        hierarchy.setHierarchy("ROLE_" + Role.ADMIN + " > ROLE_" + Role.EMPLOYEE
                + "\nROLE_" + Role.EMPLOYEE + " > ROLE_" + Role.USER);
        return hierarchy;
    }

    @Bean
    public static MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy);
        return handler;
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SessionRegistry sessionRegistry)
            throws Exception {
        http
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .requestMatchers(HttpMethod.GET, CATALOG_PATH, CATALOG_PATH + "/{id:\\d+}").permitAll()
                        .requestMatchers(ADMIN_PATHS).hasRole(Role.ADMIN.name())
                        .requestMatchers(STAFF_PATHS).hasRole(Role.EMPLOYEE.name())
                        .anyRequest().authenticated())
                .formLogin(login -> login
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/", false)
                        .failureHandler(new LoginFailureHandler()))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .addLogoutHandler(logoutHandler)
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))
                .sessionManagement(session -> session
                        .sessionFixation(fixation -> fixation.migrateSession())
                        .sessionConcurrency(concurrency -> concurrency
                                .maximumSessions(-1)
                                .sessionRegistry(sessionRegistry)
                                .expiredUrl("/login?expired")))
                .exceptionHandling(exceptions -> exceptions.accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(new PasswordChangeRequiredFilter(), AuthorizationFilter.class);
        return http.build();
    }
}
