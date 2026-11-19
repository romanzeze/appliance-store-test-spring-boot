package com.epam.rd.autocode.assessment.appliances.service;

public interface AuditService {

    void record(Actor actor, AuditAction action, String entityType, Long entityId);
}
