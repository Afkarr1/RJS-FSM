package com.rjs.fsm.job;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_status_history")
@Getter @Setter
public class JobStatusHistory {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "from_status", length = 50)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 50)
    private String toStatus;

    @Column(name = "changed_by", nullable = false)
    private UUID changedBy;

    @Column(name = "changed_at", nullable = false)
    private OffsetDateTime changedAt;

    @Column(columnDefinition = "TEXT")
    private String note;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (changedAt == null) changedAt = OffsetDateTime.now();
    }
}
