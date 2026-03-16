package com.rjs.fsm.user;

import com.rjs.fsm.user.dto.CreateUserRequest;
import com.rjs.fsm.user.dto.SetActiveRequest;
import com.rjs.fsm.user.dto.UpdatePasswordRequest;
import com.rjs.fsm.user.dto.UpdateUserRequest;
import com.rjs.fsm.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    // ✅ STEP 1: list
    @GetMapping
    public List<UserResponse> list() {
        return userService.listAll();
    }

    // ✅ STEP 1: get
    @GetMapping("/{id}")
    public UserResponse get(@PathVariable UUID id) {
        return userService.getResponse(id);
    }

    // ✅ STEP 1: create (BCrypt handled in service)
    @PostMapping
    public UserResponse create(@Valid @RequestBody CreateUserRequest req) {
        return userService.createResponse(req);
    }

    // ✅ STEP 1: enable/disable
    @PutMapping("/{id}/active")
    public UserResponse setActive(@PathVariable UUID id, @Valid @RequestBody SetActiveRequest req) {
        return userService.setActive(id, req.getActive());
    }

    // ✅ STEP 1: reset password
    @PutMapping("/{id}/password")
    public UserResponse resetPassword(@PathVariable UUID id, @Valid @RequestBody UpdatePasswordRequest req) {
        return userService.updatePassword(id, req.getNewPassword());
    }

    // (opsional dev) endpoint lama tetap ada, tapi aman karena return UserResponse
    @PutMapping("/{id}")
public UserResponse update(@PathVariable UUID id,
                           @RequestBody UpdateUserRequest req) {
    User u = userService.update(id, req);

    return UserResponse.of(
            u.getId(),
            u.getUsername(),
            u.getRole(),
            u.isActive(),
            u.getCreatedAt(),
            u.getUpdatedAt()
        );
    }
}