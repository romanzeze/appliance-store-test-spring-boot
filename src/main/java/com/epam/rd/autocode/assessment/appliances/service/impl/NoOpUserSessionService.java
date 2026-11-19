package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.service.UserSessionService;
import com.epam.rd.autocode.assessment.appliances.validation.AccountType;
import org.springframework.stereotype.Service;

@Service
public class NoOpUserSessionService implements UserSessionService {

    @Override
    public void expireSessions(AccountType accountType, Long accountId) {
    }
}
