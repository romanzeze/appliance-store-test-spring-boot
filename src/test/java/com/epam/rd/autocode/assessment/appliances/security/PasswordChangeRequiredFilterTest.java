package com.epam.rd.autocode.assessment.appliances.security;

import com.epam.rd.autocode.assessment.appliances.model.Role;
import com.epam.rd.autocode.assessment.appliances.validation.AccountType;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class PasswordChangeRequiredFilterTest {

    private final PasswordChangeRequiredFilter filter = new PasswordChangeRequiredFilter();
    private final FilterChain chain = mock(FilterChain.class);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(boolean mustChange) {
        AppUserDetails user = AppUserDetails.builder()
                .id(1L)
                .accountType(AccountType.EMPLOYEE)
                .email("e@example.com")
                .name("E")
                .role(Role.EMPLOYEE)
                .password("hash")
                .mustChangePassword(mustChange)
                .build();
        SecurityContextHolder.getContext()
                .setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
    }

    private MockHttpServletResponse run(String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }

    @Test
    void redirectsToPasswordChangeWhenRequired() throws Exception {
        authenticate(true);

        MockHttpServletResponse response = run("/appliances");

        assertThat(response.getRedirectedUrl()).isEqualTo("/password/change");
        verify(chain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void allowsPasswordChangeLogoutAndStaticResources() throws Exception {
        authenticate(true);

        for (String path : new String[]{"/password/change", "/logout", "/error", "/css/site.css"}) {
            assertThat(run(path).getRedirectedUrl()).isNull();
        }
    }

    @Test
    void passesThroughWhenNoChangeRequired() throws Exception {
        authenticate(false);

        assertThat(run("/appliances").getRedirectedUrl()).isNull();
    }

    @Test
    void passesThroughWhenAnonymous() throws Exception {
        assertThat(run("/appliances").getRedirectedUrl()).isNull();
    }
}
