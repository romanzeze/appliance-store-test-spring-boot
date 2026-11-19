package com.epam.rd.autocode.assessment.appliances.controller;

import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.security.AppUserDetails;
import com.epam.rd.autocode.assessment.appliances.validation.AccountType;

public final class ControllerTestSupport {

    private ControllerTestSupport() {
    }

    public static AppUserDetails details(Client client) {
        return AppUserDetails.builder()
                .id(client.getId())
                .accountType(AccountType.CLIENT)
                .email(client.getEmail())
                .name(client.getName())
                .role(client.getRole())
                .password("hash")
                .mustChangePassword(client.isMustChangePassword())
                .build();
    }

    public static AppUserDetails details(Employee employee) {
        return AppUserDetails.builder()
                .id(employee.getId())
                .accountType(AccountType.EMPLOYEE)
                .email(employee.getEmail())
                .name(employee.getName())
                .role(employee.getRole())
                .password("hash")
                .mustChangePassword(employee.isMustChangePassword())
                .build();
    }
}
