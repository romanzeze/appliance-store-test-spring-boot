package com.epam.rd.autocode.assessment.appliances.validation;

public interface EmailSubject {

    default Long getId() {
        return null;
    }

    String getEmail();

    AccountType accountType();
}
