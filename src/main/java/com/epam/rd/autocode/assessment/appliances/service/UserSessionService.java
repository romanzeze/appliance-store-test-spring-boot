package com.epam.rd.autocode.assessment.appliances.service;

import com.epam.rd.autocode.assessment.appliances.validation.AccountType;

public interface UserSessionService {

    void expireSessions(AccountType accountType, Long accountId);
}
