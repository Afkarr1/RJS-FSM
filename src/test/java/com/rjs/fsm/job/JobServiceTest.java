package com.rjs.fsm.job;

import com.rjs.fsm.audit.AuditService;
import com.rjs.fsm.exception.BadRequestException;
import com.rjs.fsm.exception.ForbiddenException;
import com.rjs.fsm.exception.NotFoundException;
import com.rjs.fsm.job.dto.AssignJobRequest;
import com.rjs.fsm.job.dto.CreateJobRequest;
import com.rjs.fsm.job.dto.JobResponse;
import com.rjs.fsm.job.dto.RescheduleJobRequest;
import com.rjs.fsm.notification.NotificationService;
import com.rjs.fsm.tenant.TenantContext;
import com.rjs.fsm.user.User;
import com.rjs.fsm.user.UserRepository;
import com.rjs.fsm.user.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
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
        j.setRequiresPhoto(false);
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

    private User buildAdmin(UUID id) {
        User u = new User();
        u.setId(id);
        u.setTenantId(TENANT_ID);
        u.setFullName("Admin User");
        u.setRole(UserRole.ADMIN);
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

    @Test
    void createJob_requiresPhotoDefaultsToTrue_whenRequestFieldIsNull() {
        CreateJobRequest req = new CreateJobRequest();
        req.setTitle("Fix AC");
        // requiresPhoto not set → null

        when(jobRepo.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        jobService.createJob(req, ADMIN_ID);

        verify(jobRepo).save(captor.capture());
        assertTrue(captor.getValue().isRequiresPhoto());
    }

    @Test
    void createJob_requiresPhotoIsFalse_whenRequestSaysFalse() {
        CreateJobRequest req = new CreateJobRequest();
        req.setTitle("Fix AC");
        req.setRequiresPhoto(false);

        when(jobRepo.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        jobService.createJob(req, ADMIN_ID);

        verify(jobRepo).save(captor.capture());
        assertFalse(captor.getValue().isRequiresPhoto());
    }

    @Test
    void createJob_throwsNotFound_whenAssignedTechNotFound() {
        CreateJobRequest req = new CreateJobRequest();
        req.setTitle("Fix AC");
        req.setAssignToId(TECH_ID);

        when(userRepo.findByIdAndTenantId(TECH_ID, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> jobService.createJob(req, ADMIN_ID));
        verify(jobRepo, never()).save(any());
    }

    @Test
    void createJob_throwsBadRequest_whenAssignedUserIsNotTechnician() {
        CreateJobRequest req = new CreateJobRequest();
        req.setTitle("Fix AC");
        req.setAssignToId(ADMIN_ID);

        when(userRepo.findByIdAndTenantId(ADMIN_ID, TENANT_ID)).thenReturn(Optional.of(buildAdmin(ADMIN_ID)));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> jobService.createJob(req, ADMIN_ID));
        assertTrue(ex.getMessage().contains("bukan teknisi"));
    }

    @Test
    void createJob_throwsBadRequest_whenAssignedTechnicianIsInactive() {
        CreateJobRequest req = new CreateJobRequest();
        req.setTitle("Fix AC");
        req.setAssignToId(TECH_ID);

        User inactiveTech = buildTech(TECH_ID);
        inactiveTech.setActive(false);
        when(userRepo.findByIdAndTenantId(TECH_ID, TENANT_ID)).thenReturn(Optional.of(inactiveTech));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> jobService.createJob(req, ADMIN_ID));
        assertTrue(ex.getMessage().contains("tidak aktif"));
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
    void assignJob_fromNeedFollowup_succeeds() {
        Job job = buildJob(JOB_ID, JobStatus.NEED_FOLLOWUP, TECH_ID);
        User tech = buildTech(TECH_ID);

        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));
        when(userRepo.findByIdAndTenantId(TECH_ID, TENANT_ID)).thenReturn(Optional.of(tech));
        when(jobRepo.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.findById(TECH_ID)).thenReturn(Optional.of(tech));

        AssignJobRequest req = new AssignJobRequest();
        req.setTechnicianId(TECH_ID);

        JobResponse res = jobService.assignJob(JOB_ID, req, ADMIN_ID);

        assertEquals(JobStatus.ASSIGNED, res.getStatus());
    }

    @Test
    void assignJob_updatesScheduledDate_whenProvided() {
        Job job = buildJob(JOB_ID, JobStatus.OPEN, null);
        User tech = buildTech(TECH_ID);
        LocalDate newDate = LocalDate.now().plusDays(3);

        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));
        when(userRepo.findByIdAndTenantId(TECH_ID, TENANT_ID)).thenReturn(Optional.of(tech));
        when(jobRepo.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.findById(TECH_ID)).thenReturn(Optional.of(tech));

        AssignJobRequest req = new AssignJobRequest();
        req.setTechnicianId(TECH_ID);
        req.setScheduledDate(newDate);

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        jobService.assignJob(JOB_ID, req, ADMIN_ID);

        verify(jobRepo).save(captor.capture());
        assertEquals(newDate, captor.getValue().getScheduledDate());
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

    @Test
    void assignJob_fromDone_throwsBadRequest() {
        Job job = buildJob(JOB_ID, JobStatus.DONE, TECH_ID);
        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));

        AssignJobRequest req = new AssignJobRequest();
        req.setTechnicianId(TECH_ID);

        assertThrows(BadRequestException.class,
                () -> jobService.assignJob(JOB_ID, req, ADMIN_ID));
    }

    @Test
    void assignJob_fromClosed_throwsBadRequest() {
        Job job = buildJob(JOB_ID, JobStatus.CLOSED, TECH_ID);
        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));

        AssignJobRequest req = new AssignJobRequest();
        req.setTechnicianId(TECH_ID);

        assertThrows(BadRequestException.class,
                () -> jobService.assignJob(JOB_ID, req, ADMIN_ID));
    }

    @Test
    void assignJob_throwsNotFound_whenJobNotFound() {
        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.empty());

        AssignJobRequest req = new AssignJobRequest();
        req.setTechnicianId(TECH_ID);

        assertThrows(NotFoundException.class,
                () -> jobService.assignJob(JOB_ID, req, ADMIN_ID));
    }

    @Test
    void assignJob_throwsNotFound_whenTechNotFound() {
        Job job = buildJob(JOB_ID, JobStatus.OPEN, null);
        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));
        when(userRepo.findByIdAndTenantId(TECH_ID, TENANT_ID)).thenReturn(Optional.empty());

        AssignJobRequest req = new AssignJobRequest();
        req.setTechnicianId(TECH_ID);

        assertThrows(NotFoundException.class,
                () -> jobService.assignJob(JOB_ID, req, ADMIN_ID));
    }

    // ── rescheduleJob ─────────────────────────────────────────────────────────

    @Test
    void rescheduleJob_fromNeedFollowup_transitionsToAssigned() {
        Job job = buildJob(JOB_ID, JobStatus.NEED_FOLLOWUP, TECH_ID);
        job.setPhotoUploaded(true);
        User tech = buildTech(TECH_ID);

        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));
        when(jobRepo.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.findById(TECH_ID)).thenReturn(Optional.of(tech));

        RescheduleJobRequest req = new RescheduleJobRequest();
        req.setScheduledDate(LocalDate.now().plusDays(1));

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        JobResponse res = jobService.rescheduleJob(JOB_ID, req, ADMIN_ID);

        verify(jobRepo).save(captor.capture());
        assertEquals(JobStatus.ASSIGNED, res.getStatus());
        assertFalse(captor.getValue().isPhotoUploaded());
        assertNull(captor.getValue().getStartedAt());
        assertNull(captor.getValue().getFinishedAt());
    }

    @Test
    void rescheduleJob_changesTechnician_whenTechnicianIdProvided() {
        UUID newTechId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        Job job = buildJob(JOB_ID, JobStatus.NEED_FOLLOWUP, TECH_ID);
        User newTech = buildTech(newTechId);

        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));
        when(userRepo.findByIdAndTenantId(newTechId, TENANT_ID)).thenReturn(Optional.of(newTech));
        when(jobRepo.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.findById(newTechId)).thenReturn(Optional.of(newTech));

        RescheduleJobRequest req = new RescheduleJobRequest();
        req.setScheduledDate(LocalDate.now().plusDays(1));
        req.setTechnicianId(newTechId);

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        jobService.rescheduleJob(JOB_ID, req, ADMIN_ID);

        verify(jobRepo).save(captor.capture());
        assertEquals(newTechId, captor.getValue().getAssignedToId());
    }

    @Test
    void rescheduleJob_keepsSameTechnician_whenNoTechnicianIdInRequest() {
        Job job = buildJob(JOB_ID, JobStatus.NEED_FOLLOWUP, TECH_ID);
        User tech = buildTech(TECH_ID);

        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));
        when(jobRepo.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.findById(TECH_ID)).thenReturn(Optional.of(tech));

        RescheduleJobRequest req = new RescheduleJobRequest();
        req.setScheduledDate(LocalDate.now().plusDays(1));
        // technicianId not set → null

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        jobService.rescheduleJob(JOB_ID, req, ADMIN_ID);

        verify(jobRepo).save(captor.capture());
        assertEquals(TECH_ID, captor.getValue().getAssignedToId());
        verify(userRepo, never()).findByIdAndTenantId(any(), any());
    }

    @Test
    void rescheduleJob_throwsBadRequest_whenJobIsNotNeedFollowup() {
        Job job = buildJob(JOB_ID, JobStatus.OPEN, null);
        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));

        RescheduleJobRequest req = new RescheduleJobRequest();
        req.setScheduledDate(LocalDate.now().plusDays(1));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> jobService.rescheduleJob(JOB_ID, req, ADMIN_ID));
        assertTrue(ex.getMessage().contains("NEED_FOLLOWUP"));
    }

    @Test
    void rescheduleJob_throwsNotFound_whenJobNotFound() {
        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.empty());

        RescheduleJobRequest req = new RescheduleJobRequest();
        req.setScheduledDate(LocalDate.now().plusDays(1));

        assertThrows(NotFoundException.class,
                () -> jobService.rescheduleJob(JOB_ID, req, ADMIN_ID));
    }

    // ── startJob ─────────────────────────────────────────────────────────────

    @Test
    void startJob_fromAssigned_succeeds() {
        Job job = buildJob(JOB_ID, JobStatus.ASSIGNED, TECH_ID);

        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));
        when(jobRepo.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.findById(TECH_ID)).thenReturn(Optional.of(buildTech(TECH_ID)));

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        JobResponse res = jobService.startJob(JOB_ID, TECH_ID);

        verify(jobRepo).save(captor.capture());
        assertEquals(JobStatus.IN_PROGRESS, res.getStatus());
        assertNotNull(captor.getValue().getStartedAt());
    }

    @Test
    void startJob_notOwned_throwsForbidden() {
        Job job = buildJob(JOB_ID, JobStatus.ASSIGNED, OTHER_ID);
        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));

        assertThrows(ForbiddenException.class,
                () -> jobService.startJob(JOB_ID, TECH_ID));
    }

    @Test
    void startJob_noAssignee_throwsForbidden() {
        Job job = buildJob(JOB_ID, JobStatus.ASSIGNED, null);
        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));

        assertThrows(ForbiddenException.class,
                () -> jobService.startJob(JOB_ID, TECH_ID));
    }

    @Test
    void startJob_fromOpen_throwsBadRequest() {
        Job job = buildJob(JOB_ID, JobStatus.OPEN, TECH_ID);
        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> jobService.startJob(JOB_ID, TECH_ID));
        assertTrue(ex.getMessage().contains("ASSIGNED"));
    }

    @Test
    void startJob_throwsNotFound_whenJobNotFound() {
        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> jobService.startJob(JOB_ID, TECH_ID));
    }

    // ── finishJob ────────────────────────────────────────────────────────────

    @Test
    void finishJob_noPhotoRequired_succeeds() {
        Job job = buildJob(JOB_ID, JobStatus.IN_PROGRESS, TECH_ID);
        job.setRequiresPhoto(false);

        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));
        when(jobRepo.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.findById(TECH_ID)).thenReturn(Optional.of(buildTech(TECH_ID)));

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        JobResponse res = jobService.finishJob(JOB_ID, TECH_ID);

        verify(jobRepo).save(captor.capture());
        assertEquals(JobStatus.DONE, res.getStatus());
        assertNotNull(captor.getValue().getFinishedAt());
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
    void finishJob_requiresPhoto_notUploaded_throwsBadRequest() {
        Job job = buildJob(JOB_ID, JobStatus.IN_PROGRESS, TECH_ID);
        job.setRequiresPhoto(true);
        job.setPhotoUploaded(false);

        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> jobService.finishJob(JOB_ID, TECH_ID));
        assertTrue(ex.getMessage().contains("Foto wajib"));
    }

    @Test
    void finishJob_sendsNotification_whenCustomerPhonePresent() {
        Job job = buildJob(JOB_ID, JobStatus.IN_PROGRESS, TECH_ID);
        job.setCustomerPhone("+6281234567890");

        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));
        when(jobRepo.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.findById(TECH_ID)).thenReturn(Optional.of(buildTech(TECH_ID)));

        jobService.finishJob(JOB_ID, TECH_ID);

        verify(notificationService).sendReviewLinkToCustomer(any(Job.class));
    }

    @Test
    void finishJob_doesNotSendNotification_whenCustomerPhoneNull() {
        Job job = buildJob(JOB_ID, JobStatus.IN_PROGRESS, TECH_ID);
        job.setCustomerPhone(null);

        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));
        when(jobRepo.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.findById(TECH_ID)).thenReturn(Optional.of(buildTech(TECH_ID)));

        jobService.finishJob(JOB_ID, TECH_ID);

        verify(notificationService, never()).sendReviewLinkToCustomer(any());
    }

    @Test
    void finishJob_doesNotSendNotification_whenCustomerPhoneBlank() {
        Job job = buildJob(JOB_ID, JobStatus.IN_PROGRESS, TECH_ID);
        job.setCustomerPhone("  ");

        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));
        when(jobRepo.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.findById(TECH_ID)).thenReturn(Optional.of(buildTech(TECH_ID)));

        jobService.finishJob(JOB_ID, TECH_ID);

        verify(notificationService, never()).sendReviewLinkToCustomer(any());
    }

    @Test
    void finishJob_notOwned_throwsForbidden() {
        Job job = buildJob(JOB_ID, JobStatus.IN_PROGRESS, OTHER_ID);
        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));

        assertThrows(ForbiddenException.class,
                () -> jobService.finishJob(JOB_ID, TECH_ID));
    }

    @Test
    void finishJob_fromAssigned_throwsBadRequest() {
        Job job = buildJob(JOB_ID, JobStatus.ASSIGNED, TECH_ID);
        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> jobService.finishJob(JOB_ID, TECH_ID));
        assertTrue(ex.getMessage().contains("IN_PROGRESS"));
    }

    // ── closeJob ─────────────────────────────────────────────────────────────

    @Test
    void closeJob_fromDone_succeeds() {
        Job job = buildJob(JOB_ID, JobStatus.DONE, TECH_ID);

        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));
        when(jobRepo.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.findById(TECH_ID)).thenReturn(Optional.of(buildTech(TECH_ID)));

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        JobResponse res = jobService.closeJob(JOB_ID, ADMIN_ID);

        verify(jobRepo).save(captor.capture());
        assertEquals(JobStatus.CLOSED, res.getStatus());
        assertNotNull(captor.getValue().getClosedAt());
    }

    @Test
    void closeJob_fromInProgress_throwsBadRequest() {
        Job job = buildJob(JOB_ID, JobStatus.IN_PROGRESS, TECH_ID);
        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> jobService.closeJob(JOB_ID, ADMIN_ID));
        assertTrue(ex.getMessage().contains("DONE"));
    }

    @Test
    void closeJob_throwsNotFound_whenJobNotFound() {
        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> jobService.closeJob(JOB_ID, ADMIN_ID));
    }

    // ── markFollowUp ─────────────────────────────────────────────────────────

    @Test
    void markFollowUp_fromInProgress_succeeds() {
        Job job = buildJob(JOB_ID, JobStatus.IN_PROGRESS, TECH_ID);

        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));
        when(jobRepo.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.findById(TECH_ID)).thenReturn(Optional.of(buildTech(TECH_ID)));

        JobResponse res = jobService.markFollowUp(JOB_ID, TECH_ID, "Perlu tindak lanjut");

        assertEquals(JobStatus.NEED_FOLLOWUP, res.getStatus());
    }

    @Test
    void markFollowUp_notOwned_throwsForbidden() {
        Job job = buildJob(JOB_ID, JobStatus.IN_PROGRESS, OTHER_ID);
        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));

        assertThrows(ForbiddenException.class,
                () -> jobService.markFollowUp(JOB_ID, TECH_ID, "Perlu tindak lanjut"));
    }

    @Test
    void markFollowUp_fromAssigned_throwsBadRequest() {
        Job job = buildJob(JOB_ID, JobStatus.ASSIGNED, TECH_ID);
        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> jobService.markFollowUp(JOB_ID, TECH_ID, "Perlu tindak lanjut"));
        assertTrue(ex.getMessage().contains("IN_PROGRESS"));
    }

    // ── markPhotoUploaded ─────────────────────────────────────────────────────

    @Test
    void markPhotoUploaded_setsPhotoUploadedTrue() {
        Job job = buildJob(JOB_ID, JobStatus.IN_PROGRESS, TECH_ID);
        job.setPhotoUploaded(false);

        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));
        when(jobRepo.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        jobService.markPhotoUploaded(JOB_ID);

        verify(jobRepo).save(captor.capture());
        assertTrue(captor.getValue().isPhotoUploaded());
    }

    // ── query methods ─────────────────────────────────────────────────────────

    @Test
    void listAllJobs_returnsMappedResponses() {
        Job j1 = buildJob(JOB_ID, JobStatus.OPEN, null);
        Job j2 = buildJob(UUID.randomUUID(), JobStatus.ASSIGNED, TECH_ID);

        when(jobRepo.findByTenantIdOrderByCreatedAtDesc(TENANT_ID)).thenReturn(List.of(j1, j2));
        when(userRepo.findById(TECH_ID)).thenReturn(Optional.of(buildTech(TECH_ID)));

        List<JobResponse> result = jobService.listAllJobs();

        assertEquals(2, result.size());
        verify(jobRepo).findByTenantIdOrderByCreatedAtDesc(TENANT_ID);
    }

    @Test
    void listJobsByStatus_callsCorrectRepository() {
        when(jobRepo.findByTenantIdAndStatusOrderByScheduledDateAsc(TENANT_ID, JobStatus.OPEN))
                .thenReturn(List.of());

        List<JobResponse> result = jobService.listJobsByStatus(JobStatus.OPEN);

        assertTrue(result.isEmpty());
        verify(jobRepo).findByTenantIdAndStatusOrderByScheduledDateAsc(TENANT_ID, JobStatus.OPEN);
    }

    @Test
    void listMyJobs_callsCorrectRepository() {
        when(jobRepo.findByAssignedToIdAndTenantIdOrderByScheduledDateAsc(TECH_ID, TENANT_ID))
                .thenReturn(List.of());

        List<JobResponse> result = jobService.listMyJobs(TECH_ID);

        assertTrue(result.isEmpty());
        verify(jobRepo).findByAssignedToIdAndTenantIdOrderByScheduledDateAsc(TECH_ID, TENANT_ID);
    }

    @Test
    void listMyActiveJobs_passesCorrectStatuses() {
        when(jobRepo.findByAssignedToIdAndTenantIdAndStatusInOrderByScheduledDateAsc(
                eq(TECH_ID), eq(TENANT_ID), any())).thenReturn(List.of());

        jobService.listMyActiveJobs(TECH_ID);

        ArgumentCaptor<List<JobStatus>> captor = ArgumentCaptor.forClass(List.class);
        verify(jobRepo).findByAssignedToIdAndTenantIdAndStatusInOrderByScheduledDateAsc(
                eq(TECH_ID), eq(TENANT_ID), captor.capture());

        List<JobStatus> statuses = captor.getValue();
        assertTrue(statuses.contains(JobStatus.ASSIGNED));
        assertTrue(statuses.contains(JobStatus.IN_PROGRESS));
    }

    @Test
    void getJob_returnsResponse_whenFound() {
        Job job = buildJob(JOB_ID, JobStatus.OPEN, null);
        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));

        JobResponse res = jobService.getJob(JOB_ID);

        assertEquals(JOB_ID, res.getId());
    }

    @Test
    void getJob_throwsNotFound_whenMissing() {
        when(jobRepo.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> jobService.getJob(JOB_ID));
    }
}
