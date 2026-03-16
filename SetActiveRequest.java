package com.rjs.fsm.user.dto;

import jakarta.validation.constraints.NotNull;

public class SetActiveRequest {
    @NotNull
    private Boolean active;

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
