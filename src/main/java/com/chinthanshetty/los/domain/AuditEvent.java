package com.chinthanshetty.los.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Append-only record of everything that happened to an application, for compliance review. */
@Entity
@Table(name = "audit_event")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID applicationId;
    private String eventType;
    private String actor;
    private String detail;
    private Instant createdAt;

    protected AuditEvent() {
    }

    public AuditEvent(UUID applicationId, String eventType, String actor, String detail) {
        this.applicationId = applicationId;
        this.eventType = eventType;
        this.actor = actor;
        this.detail = detail;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getActor() {
        return actor;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
