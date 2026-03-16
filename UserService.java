package com.rjs.fsm.user;

import com.rjs.fsm.user.dto.CreateUserRequest;
import com.rjs.fsm.user.dto.UpdateUserRequest;
import com.rjs.fsm.user.dto.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public UserService(UserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    // ====== STEP 1 METHODS (DTO-based) ======

    @Transactional(readOnly = true)
    public List<UserResponse> listAll() {
        return repo.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getResponse(UUID id) {
        return toResponse(get(id));
    }

    @Transactional
    public UserResponse createResponse(CreateUserRequest req) {
        return toResponse(create(req));
    }

    @Transactional
    public UserResponse setActive(UUID id, boolean active) {
        User u = get(id);
        u.setActive(active);
        return toResponse(repo.save(u));
    }

    @Transactional
    public UserResponse updatePassword(UUID id, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("New password cannot be blank");
        }
        User u = get(id);
        u.setPasswordHash(encoder.encode(newPassword));
        return toResponse(repo.save(u));
    }

    // ====== EXISTING METHODS (keep) ======

    @Transactional(readOnly = true)
    public List<User> list() {
        return repo.findAll();
    }

    @Transactional(readOnly = true)
    public User get(UUID id) {
        return repo.findById(id).orElseThrow(() ->
                new IllegalArgumentException("User not found: " + id));
    }

    @Transactional
    public User create(CreateUserRequest req) {
        if (req == null) throw new IllegalArgumentException("Request is required");

        String username = req.getUsername();
        String fullName = req.getFullName();
        String password = req.getPassword();
        UserRole role = req.getRole();

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be blank");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name cannot be blank");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be blank");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role is required");
        }
        if (repo.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }

        User u = new User();
        u.setUsername(username);
        u.setFullName(fullName);
        u.setPasswordHash(encoder.encode(password)); // ✅ BCrypt
        u.setRole(role);
        u.setActive(true);
        return repo.save(u);
    }

    @Transactional
    public User update(UUID id, UpdateUserRequest req) {
        User u = get(id);
        if (req == null) return u;

        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            u.setPasswordHash(encoder.encode(req.getPassword()));
        }
        if (req.getRole() != null) {
            u.setRole(req.getRole());
        }
        if (req.getActive() != null) {
            u.setActive(req.getActive());
        }
        return repo.save(u);
    }

    @Transactional
    public void delete(UUID id) {
        if (!repo.existsById(id)) return;
        repo.deleteById(id);
    }

    // ====== MAPPER ======
    private UserResponse toResponse(User u) {
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
