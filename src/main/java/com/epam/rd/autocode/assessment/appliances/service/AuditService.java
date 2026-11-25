package com.epam.rd.autocode.assessment.appliances.service;

import com.epam.rd.autocode.assessment.appliances.dto.audit.AuditLogDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditService {

    void record(Actor actor, AuditAction action, String entityType, Long entityId);

    Page<AuditLogDto> findAll(String action, String entityType, Pageable pageable);
}
