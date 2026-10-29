package com.chinthanshetty.los.service;

import com.chinthanshetty.los.domain.AuditEvent;
import com.chinthanshetty.los.domain.AuditEventRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AuditService {

    public static final String SYSTEM = "system";

    private final AuditEventRepository events;

    public AuditService(AuditEventRepository events) {
        this.events = events;
    }

    public void record(UUID applicationId, String eventType, String actor, String detail) {
        events.save(new AuditEvent(applicationId, eventType, actor, detail));
    }

    public List<AuditEvent> trail(UUID applicationId) {
        return events.findByApplicationIdOrderByIdAsc(applicationId);
    }
}
