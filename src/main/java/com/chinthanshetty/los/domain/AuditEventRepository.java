package com.chinthanshetty.los.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findByApplicationIdOrderByIdAsc(UUID applicationId);
}
