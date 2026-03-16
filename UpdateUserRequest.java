package com.rjs.fsm.user.dto;

import com.rjs.fsm.user.UserRole;

public class UpdateUserRequest {
    private String password;
    private UserRole role;
    private Boolean active;

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
