package com.rjs.fsm.user.dto;

import com.rjs.fsm.user.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateUserRequest {
    @NotBlank @Size(min = 3, max = 50)
    private String username;

    @NotBlank
    private String fullName;

    @NotBlank @Size(min = 8, max = 100)
    private String password;

    @NotNull
    private UserRole role;

    private String phoneE164;
}
