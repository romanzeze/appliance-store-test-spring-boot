package com.epam.rd.autocode.assessment.appliances.security;

import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.model.Role;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.repository.EmployeeRepository;
import com.epam.rd.autocode.assessment.appliances.service.UserSessionService;
import com.epam.rd.autocode.assessment.appliances.validation.AccountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
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
    private UserSessionService userSessionService;

    private Client client;
    private Employee employee;
    private Employee admin;

    @BeforeEach
    void setUp() {
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

        MvcResult login = mockMvc.perform(formLogin("/login").user("email", "larry@sec.test").password(PASSWORD))
                .andExpect(redirectedUrl("/"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession();

        mockMvc.perform(get("/appliances").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/password/change"));
        assertThat(mockMvc.perform(get("/password/change").session(session)).andReturn().getResponse().getStatus())
                .isNotEqualTo(302);
    }

    @Test
    void expiringSessionsForcesReLogin() throws Exception {
        MvcResult login = mockMvc.perform(formLogin("/login").user("email", "magic@sec.test").password(PASSWORD))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession();
        assertThat(mockMvc.perform(get("/appliances").session(session)).andReturn().getResponse().getStatus())
                .isNotIn(302, 403);

        userSessionService.expireSessions(AccountType.CLIENT, client.getId());

        mockMvc.perform(get("/appliances").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?expired"));
    }

    @Test
    void expiringSessionsOfAnotherAccountKeepsSessionAlive() throws Exception {
        MvcResult login = mockMvc.perform(formLogin("/login").user("email", "magic@sec.test").password(PASSWORD))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession();

        userSessionService.expireSessions(AccountType.EMPLOYEE, employee.getId());

        assertThat(mockMvc.perform(get("/appliances").session(session)).andReturn().getResponse().getStatus())
                .isNotIn(302, 403);
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
