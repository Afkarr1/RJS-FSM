package com.rjs.fsm.user;

import com.rjs.fsm.exception.BadRequestException;
import com.rjs.fsm.tenant.TenantContext;
import com.rjs.fsm.user.dto.CreateUserRequest;
import com.rjs.fsm.user.dto.UserResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID USER_ID   = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Mock UserRepository repo;
    @Mock PasswordEncoder encoder;

    @InjectMocks UserService userService;

    @BeforeEach
    void setUp() {
        TenantContext.set(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private User buildUser(UUID id, UserRole role, boolean active) {
        User u = new User();
        u.setId(id);
        u.setTenantId(TENANT_ID);
        u.setUsername("testuser");
        u.setFullName("Test User");
        u.setPasswordHash("hash");
        u.setRole(role);
        u.setActive(active);
        return u;
    }

    private CreateUserRequest buildCreateRequest(String username, String password, UserRole role) {
        CreateUserRequest req = new CreateUserRequest();
        req.setUsername(username);
        req.setPassword(password);
        req.setFullName("Full Name");
        req.setRole(role);
        return req;
    }

    // ── create ───────────────────────────────────────────────────────────────

    @Test
    void create_encodesPassword() {
        when(repo.existsByUsernameAndTenantId(any(), eq(TENANT_ID))).thenReturn(false);
        when(encoder.encode("secret")).thenReturn("$2a$10$hashed");
        when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.create(buildCreateRequest("john", "secret", UserRole.TECHNICIAN));

        verify(encoder).encode("secret");
        verify(repo).save(argThat(u -> "$2a$10$hashed".equals(u.getPasswordHash())));
    }

    @Test
    void create_lowercasesUsername() {
        when(repo.existsByUsernameAndTenantId(any(), eq(TENANT_ID))).thenReturn(false);
        when(encoder.encode(any())).thenReturn("hash");
        when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.create(buildCreateRequest("JohnDoe", "pass", UserRole.ADMIN));

        verify(repo).save(argThat(u -> "johndoe".equals(u.getUsername())));
    }

    @Test
    void create_duplicateUsername_throwsBadRequest() {
        when(repo.existsByUsernameAndTenantId(any(), eq(TENANT_ID))).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> userService.create(buildCreateRequest("existing", "pass", UserRole.TECHNICIAN)));

        verify(repo, never()).save(any());
    }

    // ── listTechnicians ──────────────────────────────────────────────────────

    @Test
    void listTechnicians_returnsOnlyTechnicianRole() {
        User tech = buildUser(USER_ID, UserRole.TECHNICIAN, true);
        when(repo.findByTenantIdAndRoleOrderByFullNameAsc(TENANT_ID, UserRole.TECHNICIAN))
                .thenReturn(List.of(tech));

        List<UserResponse> result = userService.listTechnicians();

        assertEquals(1, result.size());
        assertEquals(UserRole.TECHNICIAN, result.get(0).getRole());
    }

    // ── setActive ────────────────────────────────────────────────────────────

    @Test
    void setActive_true_activatesUser() {
        User user = buildUser(USER_ID, UserRole.TECHNICIAN, false);
        when(repo.findByIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.of(user));
        when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse res = userService.setActive(USER_ID, true);

        assertTrue(res.isActive());
    }

    @Test
    void setActive_false_deactivatesUser() {
        User user = buildUser(USER_ID, UserRole.TECHNICIAN, true);
        when(repo.findByIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.of(user));
        when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse res = userService.setActive(USER_ID, false);

        assertFalse(res.isActive());
    }

    // ── updatePassword ───────────────────────────────────────────────────────

    @Test
    void updatePassword_encodesAndSaves() {
        User user = buildUser(USER_ID, UserRole.ADMIN, true);
        when(repo.findByIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.of(user));
        when(encoder.encode("newSecret")).thenReturn("$2a$10$newHash");
        when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.updatePassword(USER_ID, "newSecret");

        verify(repo).save(argThat(u -> "$2a$10$newHash".equals(u.getPasswordHash())));
    }
}
