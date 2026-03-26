package com.rjs.fsm.user;

import com.rjs.fsm.exception.BadRequestException;
import com.rjs.fsm.exception.NotFoundException;
import com.rjs.fsm.tenant.TenantContext;
import com.rjs.fsm.user.dto.CreateUserRequest;
import com.rjs.fsm.user.dto.UpdateUserRequest;
import com.rjs.fsm.user.dto.UserResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
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
    void create_trimsUsername() {
        when(repo.existsByUsernameAndTenantId(any(), eq(TENANT_ID))).thenReturn(false);
        when(encoder.encode(any())).thenReturn("hash");
        when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.create(buildCreateRequest("  admin  ", "pass", UserRole.ADMIN));

        verify(repo).save(argThat(u -> "admin".equals(u.getUsername())));
    }

    @Test
    void create_setsActiveTrueByDefault() {
        when(repo.existsByUsernameAndTenantId(any(), eq(TENANT_ID))).thenReturn(false);
        when(encoder.encode(any())).thenReturn("hash");
        when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        userService.create(buildCreateRequest("john", "pass", UserRole.TECHNICIAN));

        verify(repo).save(captor.capture());
        assertTrue(captor.getValue().isActive());
    }

    @Test
    void create_duplicateUsername_throwsBadRequest() {
        when(repo.existsByUsernameAndTenantId(any(), eq(TENANT_ID))).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> userService.create(buildCreateRequest("existing", "pass", UserRole.TECHNICIAN)));

        assertTrue(ex.getMessage().contains("sudah digunakan"));
        verify(repo, never()).save(any());
    }

    // ── update ───────────────────────────────────────────────────────────────

    @Test
    void update_changesFullName_whenProvided() {
        User user = buildUser(USER_ID, UserRole.TECHNICIAN, true);
        when(repo.findByIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.of(user));
        when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserRequest req = new UpdateUserRequest();
        req.setFullName("  New Name  ");

        userService.update(USER_ID, req);

        verify(repo).save(argThat(u -> "New Name".equals(u.getFullName())));
    }

    @Test
    void update_changesRole_whenProvided() {
        User user = buildUser(USER_ID, UserRole.TECHNICIAN, true);
        when(repo.findByIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.of(user));
        when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserRequest req = new UpdateUserRequest();
        req.setRole(UserRole.ADMIN);

        userService.update(USER_ID, req);

        verify(repo).save(argThat(u -> UserRole.ADMIN.equals(u.getRole())));
    }

    @Test
    void update_changesPhone_whenProvided() {
        User user = buildUser(USER_ID, UserRole.TECHNICIAN, true);
        when(repo.findByIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.of(user));
        when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserRequest req = new UpdateUserRequest();
        req.setPhoneE164("+6281234567890");

        userService.update(USER_ID, req);

        verify(repo).save(argThat(u -> "+6281234567890".equals(u.getPhoneE164())));
    }

    @Test
    void update_doesNotChangeFullName_whenBlank() {
        User user = buildUser(USER_ID, UserRole.TECHNICIAN, true);
        String originalName = user.getFullName();
        when(repo.findByIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.of(user));
        when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserRequest req = new UpdateUserRequest();
        req.setFullName("   ");

        userService.update(USER_ID, req);

        verify(repo).save(argThat(u -> originalName.equals(u.getFullName())));
    }

    @Test
    void update_throwsNotFound_whenUserMissing() {
        when(repo.findByIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.empty());

        UpdateUserRequest req = new UpdateUserRequest();
        req.setFullName("New Name");

        assertThrows(NotFoundException.class,
                () -> userService.update(USER_ID, req));
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

    @Test
    void setActive_throwsNotFound_whenUserMissing() {
        when(repo.findByIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> userService.setActive(USER_ID, true));
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

    @Test
    void updatePassword_throwsNotFound_whenUserMissing() {
        when(repo.findByIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> userService.updatePassword(USER_ID, "newPass"));
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

    @Test
    void listTechnicians_returnsEmpty_whenNoneExist() {
        when(repo.findByTenantIdAndRoleOrderByFullNameAsc(TENANT_ID, UserRole.TECHNICIAN))
                .thenReturn(List.of());

        List<UserResponse> result = userService.listTechnicians();

        assertTrue(result.isEmpty());
    }

    // ── listAll ──────────────────────────────────────────────────────────────

    @Test
    void listAll_returnsAllUsersForTenant() {
        User u1 = buildUser(USER_ID, UserRole.ADMIN, true);
        User u2 = buildUser(UUID.randomUUID(), UserRole.TECHNICIAN, true);
        User u3 = buildUser(UUID.randomUUID(), UserRole.TECHNICIAN, false);

        when(repo.findByTenantIdOrderByCreatedAtDesc(TENANT_ID)).thenReturn(List.of(u1, u2, u3));

        List<UserResponse> result = userService.listAll();

        assertEquals(3, result.size());
        verify(repo).findByTenantIdOrderByCreatedAtDesc(TENANT_ID);
    }

    // ── getResponse ──────────────────────────────────────────────────────────

    @Test
    void getResponse_returnsUser_whenFound() {
        User user = buildUser(USER_ID, UserRole.TECHNICIAN, true);
        when(repo.findByIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.of(user));

        UserResponse res = userService.getResponse(USER_ID);

        assertEquals(USER_ID, res.getId());
    }

    @Test
    void getResponse_throwsNotFound_whenMissing() {
        when(repo.findByIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> userService.getResponse(USER_ID));
    }

    // ── updateLastLogin ──────────────────────────────────────────────────────

    @Test
    void updateLastLogin_setsLastLoginAt_whenUserExists() {
        User user = buildUser(USER_ID, UserRole.ADMIN, true);
        when(repo.findById(USER_ID)).thenReturn(Optional.of(user));
        when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.updateLastLogin(USER_ID);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repo).save(captor.capture());
        assertNotNull(captor.getValue().getLastLoginAt());
    }

    @Test
    void updateLastLogin_doesNothing_whenUserNotFound() {
        when(repo.findById(USER_ID)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> userService.updateLastLogin(USER_ID));
        verify(repo, never()).save(any());
    }
}
