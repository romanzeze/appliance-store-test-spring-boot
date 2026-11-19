package com.epam.rd.autocode.assessment.appliances.service;

import com.epam.rd.autocode.assessment.appliances.model.Role;
import org.springframework.security.access.AccessDeniedException;

public record Actor(Long id, Role role) {

    public boolean isStaff() {
        return role == Role.EMPLOYEE || role == Role.ADMIN;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean is(Role expected, Long accountId) {
        return role == expected && id != null && id.equals(accountId);
    }

    public void requireStaff() {
        if (!isStaff()) {
            throw new AccessDeniedException("Staff role is required");
        }
    }

    public void requireAdmin() {
        if (!isAdmin()) {
            throw new AccessDeniedException("Admin role is required");
        }
    }
}
