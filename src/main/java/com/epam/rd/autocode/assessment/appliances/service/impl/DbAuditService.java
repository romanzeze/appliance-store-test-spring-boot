package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.dto.audit.AuditLogDto;
import com.epam.rd.autocode.assessment.appliances.model.AuditLog;
import com.epam.rd.autocode.assessment.appliances.repository.AuditLogRepository;
import com.epam.rd.autocode.assessment.appliances.service.Actor;
import com.epam.rd.autocode.assessment.appliances.service.AuditAction;
import com.epam.rd.autocode.assessment.appliances.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j(topic = "AUDIT")
@Service
@RequiredArgsConstructor
public class DbAuditService implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final Clock clock;

    @Override
    @Transactional
    public void record(Actor actor, AuditAction action, String entityType, Long entityId) {
        if (actor.isStaff()) {
            auditLogRepository.save(AuditLog.builder()
                    .createdAt(LocalDateTime.now(clock))
                    .actorId(actor.id())
                    .actorRole(actor.role())
                    .action(action.name())
                    .entityType(entityType)
                    .entityId(entityId)
                    .build());
        }
        log.info("actor={}:{} action={} entity={}:{}", actor.role(), actor.id(), action, entityType, entityId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogDto> findAll(String action, String entityType, Pageable pageable) {
        return auditLogRepository.filter(normalize(action), normalize(entityType), pageable)
                .map(a -> new AuditLogDto(a.getId(), a.getCreatedAt(), a.getActorId(), a.getActorRole(),
                        a.getAction(), a.getEntityType(), a.getEntityId()));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
