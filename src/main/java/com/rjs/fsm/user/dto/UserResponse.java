package com.rjs.fsm.user.dto;

import com.rjs.fsm.user.User;
import com.rjs.fsm.user.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String username;
    private String fullName;
    private UserRole role;
    private String phoneE164;
    private boolean active;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime lastLoginAt;

    public static UserResponse from(User u) {
        return new UserResponse(
                u.getId(), u.getUsername(), u.getFullName(), u.getRole(),
                u.getPhoneE164(), u.isActive(),
                u.getCreatedAt(), u.getUpdatedAt(), u.getLastLoginAt()
        );
    }
}
