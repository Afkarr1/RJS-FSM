package com.rjs.fsm.user.dto;

import com.rjs.fsm.user.UserRole;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateUserRequest {
    private String fullName;
    private UserRole role;
    private String phoneE164;
}
