package com.rjs.fsm.user;

import com.rjs.fsm.exception.BadRequestException;
import com.rjs.fsm.exception.NotFoundException;
import com.rjs.fsm.tenant.TenantContext;
import com.rjs.fsm.user.TechSection;
import com.rjs.fsm.user.dto.*;
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

    @Transactional(readOnly = true)
    public List<UserResponse> listAll() {
        UUID tenantId = TenantContext.require();
        return repo.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listTechnicians() {
        UUID tenantId = TenantContext.require();
        return repo.findByTenantIdAndRoleOrderByFullNameAsc(tenantId, UserRole.TECHNICIAN).stream()
                .map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getResponse(UUID id) {
        return UserResponse.from(get(id));
    }

    @Transactional(readOnly = true)
    public User get(UUID id) {
        UUID tenantId = TenantContext.require();
        return repo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
    }

    @Transactional
    public UserResponse create(CreateUserRequest req) {
        UUID tenantId = TenantContext.require();

        String username = req.getUsername().trim();
        TechSection techSection = req.getTechSection() != null ? req.getTechSection() : TechSection.FIELD;

        // Allow same username with different techSection (teknisi can have FIELD + INTERNAL)
        if (repo.existsByUsernameAndTenantIdAndTechSection(username, tenantId, techSection)) {
            throw new BadRequestException("Username sudah digunakan untuk seksi ini: " + username);
        }

        User u = new User();
        u.setTenantId(tenantId);
        u.setUsername(username);
        u.setFullName(req.getFullName().trim());
        u.setPasswordHash(encoder.encode(req.getPassword()));
        u.setRole(req.getRole());
        u.setPhoneE164(req.getPhoneE164());
        u.setTechSection(techSection);
        u.setActive(true);

        return UserResponse.from(repo.save(u));
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest req) {
        User u = get(id);
        if (req.getFullName() != null && !req.getFullName().isBlank()) {
            u.setFullName(req.getFullName().trim());
        }
        if (req.getRole() != null) {
            u.setRole(req.getRole());
        }
        if (req.getPhoneE164() != null) {
            u.setPhoneE164(req.getPhoneE164());
        }
        if (req.getTechSection() != null) {
            u.setTechSection(req.getTechSection());
        }
        return UserResponse.from(repo.save(u));
    }

    @Transactional
    public UserResponse setActive(UUID id, boolean active) {
        User u = get(id);
        u.setActive(active);
        return UserResponse.from(repo.save(u));
    }

    @Transactional
    public UserResponse updatePassword(UUID id, String newPassword) {
        User u = get(id);
        u.setPasswordHash(encoder.encode(newPassword));
        return UserResponse.from(repo.save(u));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listTechniciansBySection(TechSection section) {
        UUID tenantId = TenantContext.require();
        return repo.findByTenantIdAndRoleAndTechSectionOrderByFullNameAsc(
                        tenantId, UserRole.TECHNICIAN, section)
                .stream().map(UserResponse::from).toList();
    }

    @Transactional
    public void updateLastLogin(UUID userId) {
        repo.findById(userId).ifPresent(u -> {
            u.setLastLoginAt(java.time.OffsetDateTime.now());
            repo.save(u);
        });
    }
}
