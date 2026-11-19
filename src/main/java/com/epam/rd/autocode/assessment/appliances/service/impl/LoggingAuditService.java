package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.service.Actor;
import com.epam.rd.autocode.assessment.appliances.service.AuditAction;
import com.epam.rd.autocode.assessment.appliances.service.AuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j(topic = "AUDIT")
@Service
public class LoggingAuditService implements AuditService {

    @Override
    public void record(Actor actor, AuditAction action, String entityType, Long entityId) {
        log.info("actor={}:{} action={} entity={}:{}",
                actor.role(), actor.id(), action, entityType, entityId);
    }
}
