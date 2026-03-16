package com.rjs.fsm.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_login_audit")
@Getter @Setter
public class UserLoginAudit {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 80)
    private String username;

    @Column(length = 50)
    private String role;

    @Column(name = "logged_in_at", nullable = false)
    private OffsetDateTime loggedInAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (loggedInAt == null) loggedInAt = OffsetDateTime.now();
    }
}
