package com.rjs.fsm.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SetActiveRequest {
    @NotNull
    private Boolean active;
}
