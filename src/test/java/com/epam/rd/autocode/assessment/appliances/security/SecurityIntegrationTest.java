package com.epam.rd.autocode.assessment.appliances.security;

import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.model.Role;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.repository.EmployeeRepository;
import com.epam.rd.autocode.assessment.appliances.validation.AccountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityIntegrationTest {

    private static final String PASSWORD = "Str0ng!Pass123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private LoginAttemptService loginAttemptService;
    @Autowired
    private JwtService jwtService;

    private Client client;
    private Employee employee;
    private Employee admin;

    @BeforeEach
    void setUp() {
        loginAttemptService.reset("magic@sec.test");
        loginAttemptService.reset("larry@sec.test");
        client = clientRepository.save(new Client(null, "Magic", "magic@sec.test",
                passwordEncoder.encode(PASSWORD), "1111"));
        employee = new Employee(null, "Larry", "larry@sec.test", passwordEncoder.encode(PASSWORD), "Sales");
        employee.setRole(Role.EMPLOYEE);
        employee = employeeRepository.save(employee);
        admin = new Employee(null, "Kobe", "kobe@sec.test", passwordEncoder.encode(PASSWORD), "Board");
        admin.setRole(Role.ADMIN);
        admin = employeeRepository.save(admin);
    }

    @Test
    void anonymousIsRedirectedToLoginFromProtectedPages() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void catalogIsPublicButManagementPagesAreNot() throws Exception {
        mockMvc.perform(get("/appliances")).andExpect(status().isOk());
        for (String path : new String[]{"/appliances/add", "/appliances/deleted", "/appliances/1/edit"}) {
            mockMvc.perform(get(path))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("http://localhost/login"));
        }
        mockMvc.perform(post("/appliances/1/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void publicPagesAreNotRedirectedToLogin() throws Exception {
        for (String path : new String[]{"/login", "/register", "/error"}) {
            int status = mockMvc.perform(get(path)).andReturn().getResponse().getStatus();
            assertThat(status).isNotIn(302, 401, 403);
        }
    }

    @Test
    void roleHierarchyAppliesToUrlRules() throws Exception {
        assertThat(statusFor("/employees", Role.USER, 1L)).isEqualTo(403);
        assertThat(statusFor("/employees", Role.EMPLOYEE, 1L)).isEqualTo(403);
        assertThat(statusFor("/employees", Role.ADMIN, 1L)).isNotEqualTo(403);
        assertThat(statusFor("/clients", Role.USER, 1L)).isEqualTo(403);
        assertThat(statusFor("/clients", Role.EMPLOYEE, 1L)).isNotEqualTo(403);
        assertThat(statusFor("/clients", Role.ADMIN, 1L)).isNotEqualTo(403);
        assertThat(statusFor("/manufacturers", Role.USER, 1L)).isEqualTo(403);
        assertThat(statusFor("/manufacturers", Role.EMPLOYEE, 1L)).isNotEqualTo(403);
        assertThat(statusFor("/appliances", Role.USER, 1L)).isNotEqualTo(403);
    }

    @Test
    void loginSucceedsWithValidCredentials() throws Exception {
        mockMvc.perform(formLogin("/login").user("email", "magic@sec.test").password(PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void loginIsCaseInsensitiveForEmployeesAndClients() throws Exception {
        mockMvc.perform(formLogin("/login").user("email", "LARRY@sec.test").password(PASSWORD))
                .andExpect(redirectedUrl("/"));
        mockMvc.perform(formLogin("/login").user("email", "Magic@Sec.Test").password(PASSWORD))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void wrongPasswordRedirectsToError() throws Exception {
        mockMvc.perform(formLogin("/login").user("email", "magic@sec.test").password("wrong"))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void unknownUserGetsSameErrorAsWrongPassword() throws Exception {
        mockMvc.perform(formLogin("/login").user("email", "nobody@sec.test").password(PASSWORD))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void blockedAccountCannotLogIn() throws Exception {
        client.setBlocked(true);
        clientRepository.save(client);

        mockMvc.perform(formLogin("/login").user("email", "magic@sec.test").password(PASSWORD))
                .andExpect(redirectedUrl("/login?blocked"));
    }

    @Test
    void deletedAccountCannotLogIn() throws Exception {
        clientRepository.delete(client);
        clientRepository.flush();

        mockMvc.perform(formLogin("/login").user("email", "magic@sec.test").password(PASSWORD))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void expiredTemporaryPasswordCannotLogIn() throws Exception {
        employee.setMustChangePassword(true);
        employee.setPasswordExpiresAt(LocalDateTime.now().minusHours(1));
        employeeRepository.save(employee);

        mockMvc.perform(formLogin("/login").user("email", "larry@sec.test").password(PASSWORD))
                .andExpect(redirectedUrl("/login?expired-password"));
    }

    @Test
    void validTemporaryPasswordForcesPasswordChange() throws Exception {
        employee.setMustChangePassword(true);
        employee.setPasswordExpiresAt(LocalDateTime.now().plusHours(24));
        employeeRepository.save(employee);

        Cookie token = loginCookie("larry@sec.test");

        mockMvc.perform(get("/appliances").cookie(token))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/password/change"));
        assertThat(mockMvc.perform(get("/password/change").cookie(token)).andReturn().getResponse().getStatus())
                .isNotEqualTo(302);
    }

    @Test
    void loginIssuesHttpOnlyJwtCookieAndNoSession() throws Exception {
        var result = mockMvc.perform(formLogin("/login").user("email", "magic@sec.test").password(PASSWORD)).andReturn();

        String header = result.getResponse().getHeader("Set-Cookie");
        assertThat(header).startsWith("AUTH_TOKEN=").contains("HttpOnly").contains("SameSite=Lax");
        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test
    void tokenAuthenticatesSubsequentRequests() throws Exception {
        Cookie token = loginCookie("magic@sec.test");

        assertThat(mockMvc.perform(get("/orders").cookie(token)).andReturn().getResponse().getStatus()).isEqualTo(200);
        mockMvc.perform(get("/audit").cookie(token)).andExpect(status().isForbidden());
    }

    @Test
    void blockingAccountRevokesAccessOnNextRequest() throws Exception {
        Cookie token = loginCookie("magic@sec.test");
        client.setBlocked(true);
        clientRepository.save(client);

        mockMvc.perform(get("/orders").cookie(token))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void deletedAccountTokenIsRejected() throws Exception {
        Cookie token = loginCookie("magic@sec.test");
        clientRepository.delete(client);
        clientRepository.flush();

        mockMvc.perform(get("/orders").cookie(token))
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void tamperedOrForeignTokenIsRejected() throws Exception {
        Cookie token = loginCookie("magic@sec.test");
        mockMvc.perform(get("/orders").cookie(new Cookie(JwtService.COOKIE_NAME, token.getValue() + "x")))
                .andExpect(redirectedUrl("http://localhost/login"));
        mockMvc.perform(get("/orders").cookie(new Cookie(JwtService.COOKIE_NAME, "not-a-jwt")))
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void tokenOfAnotherAccountDoesNotGrantAdminRights() throws Exception {
        String clientToken = jwtService.issue(AccountType.CLIENT, client.getId());

        mockMvc.perform(get("/employees").cookie(new Cookie(JwtService.COOKIE_NAME, clientToken)))
                .andExpect(status().isForbidden());
        String adminToken = jwtService.issue(AccountType.EMPLOYEE, admin.getId());
        assertThat(mockMvc.perform(get("/employees").cookie(new Cookie(JwtService.COOKIE_NAME, adminToken)))
                .andReturn().getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void logoutClearsTokenCookie() throws Exception {
        String header = mockMvc.perform(post("/logout").with(csrf()).cookie(loginCookie("magic@sec.test")))
                .andExpect(redirectedUrl("/login?logout"))
                .andReturn().getResponse().getHeaders("Set-Cookie").toString();

        assertThat(header).contains("AUTH_TOKEN=;");
    }

    @Test
    void accountIsLockedAfterRepeatedFailures() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(formLogin("/login").user("email", "magic@sec.test").password("wrong"))
                    .andExpect(redirectedUrl("/login?error"));
        }

        mockMvc.perform(formLogin("/login").user("email", "magic@sec.test").password(PASSWORD))
                .andExpect(redirectedUrl("/login?locked"));
        mockMvc.perform(formLogin("/login").user("email", "MAGIC@sec.test").password(PASSWORD))
                .andExpect(redirectedUrl("/login?locked"));
    }

    @Test
    void unknownEmailIsLockedTooWithoutRevealingExistence() throws Exception {
        loginAttemptService.reset("ghost@sec.test");
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(formLogin("/login").user("email", "ghost@sec.test").password("wrong"));
        }

        mockMvc.perform(formLogin("/login").user("email", "ghost@sec.test").password("wrong"))
                .andExpect(redirectedUrl("/login?locked"));
        loginAttemptService.reset("ghost@sec.test");
    }

    @Test
    void successfulLoginResetsFailureCounter() throws Exception {
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(formLogin("/login").user("email", "magic@sec.test").password("wrong"));
        }
        mockMvc.perform(formLogin("/login").user("email", "magic@sec.test").password(PASSWORD))
                .andExpect(redirectedUrl("/"));
        mockMvc.perform(formLogin("/login").user("email", "magic@sec.test").password("wrong"))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void postWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/login").param("email", "magic@sec.test").param("password", PASSWORD))
                .andExpect(status().isForbidden());
    }

    @Test
    void logoutWithCsrfRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()).with(user(new AppUserDetailsFixture().client(client))))
                .andExpect(redirectedUrl("/login?logout"));
    }

    private Cookie loginCookie(String email) throws Exception {
        String header = mockMvc.perform(formLogin("/login").user("email", email).password(PASSWORD))
                .andExpect(redirectedUrl("/"))
                .andReturn().getResponse().getHeader("Set-Cookie");
        String value = header.substring(header.indexOf('=') + 1, header.indexOf(';'));
        return new Cookie(JwtService.COOKIE_NAME, value);
    }

    private int statusFor(String path, Role role, Long id) throws Exception {
        AppUserDetails details = AppUserDetails.builder()
                .id(id)
                .accountType(role == Role.USER ? AccountType.CLIENT : AccountType.EMPLOYEE)
                .email("x@sec.test")
                .name("X")
                .role(role)
                .password("hash")
                .build();
        return mockMvc.perform(get(path).with(user(details))).andReturn().getResponse().getStatus();
    }

    private static final class AppUserDetailsFixture {

        AppUserDetails client(Client client) {
            return AppUserDetails.builder()
                    .id(client.getId())
                    .accountType(AccountType.CLIENT)
                    .email(client.getEmail())
                    .name(client.getName())
                    .role(Role.USER)
                    .password("hash")
                    .build();
        }
    }
}
