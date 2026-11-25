package com.epam.rd.autocode.assessment.appliances.service;

import com.epam.rd.autocode.assessment.appliances.model.Role;

public record CurrentUser(Long id, String name, String email, Role role, boolean mustChangePassword) {

    public boolean isClient() {
        return role == Role.USER;
    }

    public boolean isStaff() {
        return !isClient();
    }

    public Actor toActor() {
        return new Actor(id, role);
    }
}
