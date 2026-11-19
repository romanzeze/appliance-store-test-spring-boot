package com.epam.rd.autocode.assessment.appliances.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    public static final String CHANGE_PASSWORD_URL = "/password/change";

    private static final List<String> ALLOWED_PATHS = List.of(CHANGE_PASSWORD_URL, "/logout", "/error", "/favicon.ico");
    private static final List<String> ALLOWED_PREFIXES = List.of("/css/", "/js/", "/images/", "/webjars/");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof AppUserDetails user
                && user.isMustChangePassword()
                && !isAllowed(pathOf(request))) {
            response.sendRedirect(request.getContextPath() + CHANGE_PASSWORD_URL);
            return;
        }
        chain.doFilter(request, response);
    }

    private String pathOf(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    private boolean isAllowed(String path) {
        return ALLOWED_PATHS.contains(path) || ALLOWED_PREFIXES.stream().anyMatch(path::startsWith);
    }
}
