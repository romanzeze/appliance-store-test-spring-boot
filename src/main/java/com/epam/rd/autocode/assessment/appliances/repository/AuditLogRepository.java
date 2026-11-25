package com.epam.rd.autocode.assessment.appliances.repository;

import com.epam.rd.autocode.assessment.appliances.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("select a from AuditLog a where a.actorRole <> com.epam.rd.autocode.assessment.appliances.model.Role.USER and (:action = '' or a.action = :action) and (:entityType = '' or a.entityType = :entityType)")
    Page<AuditLog> filter(@Param("action") String action, @Param("entityType") String entityType, Pageable pageable);
}
