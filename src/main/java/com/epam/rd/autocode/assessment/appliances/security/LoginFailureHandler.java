package com.epam.rd.autocode.assessment.appliances.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;

public class LoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String reason = "error";
        if (exception instanceof LockedException) {
            reason = "blocked";
        } else if (exception instanceof CredentialsExpiredException) {
            reason = "expired-password";
        }
        response.sendRedirect(request.getContextPath() + "/login?" + reason);
    }
}
