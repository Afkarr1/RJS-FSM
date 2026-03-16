package com.rjs.fsm.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_admin_actions")
@Getter @Setter
public class UserAdminAction {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "target_user_id", nullable = false)
    private UUID targetUserId;

    @Column(name = "actor_admin_id", nullable = false)
    private UUID actorAdminId;

    @Column(name = "action_type", nullable = false, length = 100)
    private String actionType;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
