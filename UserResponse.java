package com.rjs.fsm.user.dto;

import com.rjs.fsm.user.UserRole;

import java.time.OffsetDateTime;
import java.util.UUID;

public class UserResponse {
    private UUID id;
    private String username;
    private UserRole role;
    private boolean active;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static UserResponse of(
            UUID id, String username, UserRole role, boolean active,
            OffsetDateTime createdAt, OffsetDateTime updatedAt
    ) {
        UserResponse r = new UserResponse();
        r.id = id;
        r.username = username;
        r.role = role;
        r.active = active;
        r.createdAt = createdAt;
        r.updatedAt = updatedAt;
        return r;
    }

    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public UserRole getRole() { return role; }
    public boolean isActive() { return active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
