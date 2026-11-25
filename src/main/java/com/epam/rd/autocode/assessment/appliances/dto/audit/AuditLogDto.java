package com.epam.rd.autocode.assessment.appliances.dto.audit;

import com.epam.rd.autocode.assessment.appliances.model.Role;

import java.time.LocalDateTime;

public record AuditLogDto(Long id, LocalDateTime createdAt, Long actorId, Role actorRole,
                          String action, String entityType, Long entityId) {
}
