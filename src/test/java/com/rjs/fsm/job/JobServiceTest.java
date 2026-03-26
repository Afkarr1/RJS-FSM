package com.rjs.fsm.job;

import com.rjs.fsm.audit.AuditService;
import com.rjs.fsm.exception.BadRequestException;
import com.rjs.fsm.exception.ForbiddenException;
import com.rjs.fsm.job.dto.AssignJobRequest;
import com.rjs.fsm.job.dto.CreateJobRequest;
import com.rjs.fsm.job.dto.JobResponse;
import com.rjs.fsm.notification.NotificationService;
import com.rjs.fsm.tenant.TenantContext;
import com.rjs.fsm.user.User;
import com.rjs.fsm.user.UserRepository;
import com.rjs.fsm.user.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID JOB_ID    = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final UUID ADMIN_ID  = UUID.fromString("22222222-2222-2222-2222-222222222222");
    static final UUID TECH_ID   = UUID.fromString("33333333-3333-3333-3333-333333333333");
    static final UUID OTHER_ID  = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock JobRepository jobRepo;
    @Mock JobStatusHistoryRepository historyRepo;
    @Mock UserRepository userRepo;
    @Mock AuditService auditService;
    @Mock NotificationService notificationService;

    @InjectMocks JobService jobService;

    @BeforeEach
    void setUp() {
        TenantContext.set(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Job buildJob(UUID id, JobStatus status, UUID assignedToId) {
        Job j = new Job();
        j.setId(id);
        j.setTenantId(TENANT_ID);
        j.setTitle("Test Job");
        j.setStatus(status);
        j.setAssignedToId(assignedToId);
        return j;
    }

    private User buildTech(UUID id) {
        User u = new User();
        u.setId(id);
        u.setTenantId(TENANT_ID);
        u.setFullName("Tech User");
        u.setRole(UserRole.TECHNICIAN);
        u.setActive(true);
        return u;
    }

    // ── createJob ────────────────────────────────────────────────────────────

    @Test
    void createJob_withoutTechnician_setsStatusOpen() {
        CreateJobRequest req = new CreateJobRequest();
        req.setTitle("Fix AC");

        when(jobRepo.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        JobResponse res = jobService.createJob(req, ADMIN_ID);

        assertEquals(JobStatus.OPEN, res.getStatus());
        assertNull(res.getAssignedToId());
    }

    @Test
    void createJob_withTechnician_setsStatusAssigned() {
        CreateJobRequest req = new CreateJobRequest();
        req.setTitle("Fix Pump");
        req.setAssignToId(TECH_ID);

        User tech = buildTech(TECH_ID);
        when(userRepo.findByIdAndTenantId(TECH_ID, TENANT_ID)).thenReturn(Optional.of(tech));
        when(jobRepo.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.findById(TECH_ID)).thenReturn(Optional.of(tech));

        JobResponse res = jobService.createJob(req, ADMIN_ID);

        assertEquals(JobStatus.ASSIGNED, res.getStatus());
        assertEquals(TECH_ID, res.getAssignedToId());
    }

    // ── assignJob ────────────────────────────────────────────────────────────

    @Test
    void assignJob_fromOpen_succeeds() {
        Job job = buildJob(JOB_ID, JobStatus.OPEN, null);
        User tech = buildTech(TECH_ID);

        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));
        when(userRepo.findByIdAndTenantId(TECH_ID, TENANT_ID)).thenReturn(Optional.of(tech));
        when(jobRepo.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.findById(TECH_ID)).thenReturn(Optional.of(tech));

        AssignJobRequest req = new AssignJobRequest();
        req.setTechnicianId(TECH_ID);

        JobResponse res = jobService.assignJob(JOB_ID, req, ADMIN_ID);

        assertEquals(JobStatus.ASSIGNED, res.getStatus());
        assertEquals(TECH_ID, res.getAssignedToId());
    }

    @Test
    void assignJob_fromInProgress_throwsBadRequest() {
        Job job = buildJob(JOB_ID, JobStatus.IN_PROGRESS, TECH_ID);
        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));

        AssignJobRequest req = new AssignJobRequest();
        req.setTechnicianId(TECH_ID);

        assertThrows(BadRequestException.class,
                () -> jobService.assignJob(JOB_ID, req, ADMIN_ID));
    }

    // ── startJob ─────────────────────────────────────────────────────────────

    @Test
    void startJob_fromAssigned_succeeds() {
        Job job = buildJob(JOB_ID, JobStatus.ASSIGNED, TECH_ID);

        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));
        when(jobRepo.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.findById(TECH_ID)).thenReturn(Optional.of(buildTech(TECH_ID)));

        JobResponse res = jobService.startJob(JOB_ID, TECH_ID);

        assertEquals(JobStatus.IN_PROGRESS, res.getStatus());
    }

    @Test
    void startJob_notOwned_throwsForbidden() {
        Job job = buildJob(JOB_ID, JobStatus.ASSIGNED, OTHER_ID);
        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));

        assertThrows(ForbiddenException.class,
                () -> jobService.startJob(JOB_ID, TECH_ID));
    }

    // ── finishJob ────────────────────────────────────────────────────────────

    @Test
    void finishJob_requiresPhoto_notUploaded_throwsBadRequest() {
        Job job = buildJob(JOB_ID, JobStatus.IN_PROGRESS, TECH_ID);
        job.setRequiresPhoto(true);
        job.setPhotoUploaded(false);

        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));

        assertThrows(BadRequestException.class,
                () -> jobService.finishJob(JOB_ID, TECH_ID));
    }

    @Test
    void finishJob_requiresPhoto_uploaded_succeeds() {
        Job job = buildJob(JOB_ID, JobStatus.IN_PROGRESS, TECH_ID);
        job.setRequiresPhoto(true);
        job.setPhotoUploaded(true);

        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));
        when(jobRepo.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.findById(TECH_ID)).thenReturn(Optional.of(buildTech(TECH_ID)));

        JobResponse res = jobService.finishJob(JOB_ID, TECH_ID);

        assertEquals(JobStatus.DONE, res.getStatus());
    }

    @Test
    void finishJob_noPhotoRequired_succeeds() {
        Job job = buildJob(JOB_ID, JobStatus.IN_PROGRESS, TECH_ID);
        job.setRequiresPhoto(false);

        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));
        when(jobRepo.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.findById(TECH_ID)).thenReturn(Optional.of(buildTech(TECH_ID)));

        JobResponse res = jobService.finishJob(JOB_ID, TECH_ID);

        assertEquals(JobStatus.DONE, res.getStatus());
    }

    // ── closeJob ─────────────────────────────────────────────────────────────

    @Test
    void closeJob_fromDone_succeeds() {
        Job job = buildJob(JOB_ID, JobStatus.DONE, TECH_ID);

        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));
        when(jobRepo.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.findById(TECH_ID)).thenReturn(Optional.of(buildTech(TECH_ID)));

        JobResponse res = jobService.closeJob(JOB_ID, ADMIN_ID);

        assertEquals(JobStatus.CLOSED, res.getStatus());
    }

    @Test
    void closeJob_fromInProgress_throwsBadRequest() {
        Job job = buildJob(JOB_ID, JobStatus.IN_PROGRESS, TECH_ID);
        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));

        assertThrows(BadRequestException.class,
                () -> jobService.closeJob(JOB_ID, ADMIN_ID));
    }

    // ── markFollowUp ─────────────────────────────────────────────────────────

    @Test
    void markFollowUp_fromInProgress_succeeds() {
        Job job = buildJob(JOB_ID, JobStatus.IN_PROGRESS, TECH_ID);

        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));
        when(jobRepo.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.findById(TECH_ID)).thenReturn(Optional.of(buildTech(TECH_ID)));

        JobResponse res = jobService.markFollowUp(JOB_ID, TECH_ID);

        assertEquals(JobStatus.NEED_FOLLOWUP, res.getStatus());
    }

    @Test
    void markFollowUp_notOwned_throwsForbidden() {
        Job job = buildJob(JOB_ID, JobStatus.IN_PROGRESS, OTHER_ID);
        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));

        assertThrows(ForbiddenException.class,
                () -> jobService.markFollowUp(JOB_ID, TECH_ID));
    }
}
