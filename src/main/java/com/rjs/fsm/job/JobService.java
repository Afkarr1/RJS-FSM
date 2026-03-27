package com.rjs.fsm.job;

import com.rjs.fsm.audit.AuditService;
import com.rjs.fsm.exception.BadRequestException;
import com.rjs.fsm.exception.ForbiddenException;
import com.rjs.fsm.exception.NotFoundException;
import com.rjs.fsm.job.dto.*;
import com.rjs.fsm.notification.NotificationService;
import com.rjs.fsm.tenant.TenantContext;
import com.rjs.fsm.user.User;
import com.rjs.fsm.user.UserRepository;
import com.rjs.fsm.user.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    private final JobRepository jobRepo;
    private final JobStatusHistoryRepository historyRepo;
    private final UserRepository userRepo;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public JobService(JobRepository jobRepo, JobStatusHistoryRepository historyRepo,
                      UserRepository userRepo, AuditService auditService,
                      NotificationService notificationService) {
        this.jobRepo = jobRepo;
        this.historyRepo = historyRepo;
        this.userRepo = userRepo;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    // ==================== ADMIN OPERATIONS ====================

    @Transactional
    public JobResponse createJob(CreateJobRequest req, UUID adminId) {
        UUID tenantId = TenantContext.require();

        Job job = new Job();
        job.setTenantId(tenantId);
        job.setTitle(req.getTitle().trim());
        job.setDescription(req.getDescription());
        job.setCustomerName(req.getCustomerName());
        job.setCustomerPhone(req.getCustomerPhone());
        job.setAddress(req.getAddress());
        job.setScheduledDate(req.getScheduledDate());
        job.setCreatedById(adminId);
        job.setRequiresPhoto(req.getRequiresPhoto() != null ? req.getRequiresPhoto() : true);
        job.setStatus(JobStatus.OPEN);

        validateScheduledDate(req.getScheduledDate());

        if (req.getAssignToId() != null) {
            User tech = validateTechnician(req.getAssignToId(), tenantId);
            job.setAssignedToId(tech.getId());
            job.setStatus(JobStatus.ASSIGNED);
            job.setAssignedAt(OffsetDateTime.now(ZoneId.of("Asia/Jakarta")));
        }

        job = jobRepo.save(job);
        recordHistory(job, null, job.getStatus(), adminId, null);
        auditService.logAction(adminId, "CREATE_JOB", "JOBS", job.getId(), "Job created: " + job.getTitle());

        log.info("Job created: id={}, status={}", job.getId(), job.getStatus());
        return toResponse(job);
    }

    @Transactional
    public JobResponse assignJob(UUID jobId, AssignJobRequest req, UUID adminId) {
        UUID tenantId = TenantContext.require();
        Job job = getJobEntity(jobId);

        if (job.getStatus() != JobStatus.OPEN && job.getStatus() != JobStatus.NEED_FOLLOWUP) {
            throw new BadRequestException("Job hanya bisa di-assign dari status OPEN atau NEED_FOLLOWUP");
        }

        if (req.getScheduledDate() != null) validateScheduledDate(req.getScheduledDate());

        User tech = validateTechnician(req.getTechnicianId(), tenantId);

        JobStatus oldStatus = job.getStatus();
        job.setAssignedToId(tech.getId());
        job.setStatus(JobStatus.ASSIGNED);
        job.setAssignedAt(OffsetDateTime.now(ZoneId.of("Asia/Jakarta")));
        if (req.getScheduledDate() != null) {
            job.setScheduledDate(req.getScheduledDate());
        }

        job = jobRepo.save(job);
        recordHistory(job, oldStatus, JobStatus.ASSIGNED, adminId, null);
        auditService.logAction(adminId, "ASSIGN_JOB", "JOBS", job.getId(),
                "Assigned to technician: " + tech.getFullName());

        return toResponse(job);
    }

    @Transactional
    public JobResponse rescheduleJob(UUID jobId, RescheduleJobRequest req, UUID adminId) {
        UUID tenantId = TenantContext.require();
        Job job = getJobEntity(jobId);

        if (job.getStatus() != JobStatus.NEED_FOLLOWUP) {
            throw new BadRequestException("Hanya job dengan status NEED_FOLLOWUP yang bisa dijadwalkan ulang");
        }

        validateScheduledDate(req.getScheduledDate());

        JobStatus oldStatus = job.getStatus();
        job.setScheduledDate(req.getScheduledDate());
        job.setStatus(JobStatus.ASSIGNED);
        job.setAssignedAt(OffsetDateTime.now(ZoneId.of("Asia/Jakarta")));
        job.setStartedAt(null);
        job.setFinishedAt(null);
        job.setPhotoUploaded(false);

        if (req.getTechnicianId() != null) {
            User tech = validateTechnician(req.getTechnicianId(), tenantId);
            job.setAssignedToId(tech.getId());
        }

        job = jobRepo.save(job);
        recordHistory(job, oldStatus, JobStatus.ASSIGNED, adminId, null);
        auditService.logAction(adminId, "RESCHEDULE_JOB", "JOBS", job.getId(),
                "Rescheduled to " + req.getScheduledDate());

        return toResponse(job);
    }

    @Transactional
    public JobResponse closeJob(UUID jobId, UUID adminId) {
        Job job = getJobEntity(jobId);

        if (job.getStatus() != JobStatus.DONE) {
            throw new BadRequestException("Hanya job dengan status DONE yang bisa di-close");
        }

        JobStatus oldStatus = job.getStatus();
        job.setStatus(JobStatus.CLOSED);
        job.setClosedAt(OffsetDateTime.now(ZoneId.of("Asia/Jakarta")));

        job = jobRepo.save(job);
        recordHistory(job, oldStatus, JobStatus.CLOSED, adminId, null);
        auditService.logAction(adminId, "CLOSE_JOB", "JOBS", job.getId(), "Job closed");

        return toResponse(job);
    }

    @Transactional(readOnly = true)
    public List<JobResponse> listAllJobs() {
        UUID tenantId = TenantContext.require();
        return jobRepo.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<JobResponse> listJobsByStatus(JobStatus status) {
        UUID tenantId = TenantContext.require();
        return jobRepo.findByTenantIdAndStatusOrderByScheduledDateAsc(tenantId, status).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public JobResponse getJob(UUID jobId) {
        return toResponse(getJobEntity(jobId));
    }

    @Transactional(readOnly = true)
    public List<JobHistoryResponse> getJobHistory(UUID jobId) {
        UUID tenantId = TenantContext.require();
        getJobEntity(jobId);
        return historyRepo.findByJobIdAndTenantIdOrderByChangedAtAsc(jobId, tenantId).stream()
                .map(h -> {
                    String name = userRepo.findById(h.getChangedBy())
                            .map(u -> u.getFullName() != null && !u.getFullName().isBlank()
                                    ? u.getFullName() : u.getUsername())
                            .orElse("Unknown");
                    return JobHistoryResponse.from(h, name);
                }).toList();
    }

    // ==================== TECHNICIAN OPERATIONS ====================

    @Transactional(readOnly = true)
    public List<JobResponse> listMyJobs(UUID technicianId) {
        UUID tenantId = TenantContext.require();
        return jobRepo.findByAssignedToIdAndTenantIdOrderByScheduledDateAsc(technicianId, tenantId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<JobResponse> listMyActiveJobs(UUID technicianId) {
        UUID tenantId = TenantContext.require();
        List<JobStatus> activeStatuses = List.of(JobStatus.ASSIGNED, JobStatus.IN_TRANSIT, JobStatus.IN_PROGRESS);
        return jobRepo.findByAssignedToIdAndTenantIdAndStatusInOrderByScheduledDateAsc(
                technicianId, tenantId, activeStatuses).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public JobResponse startTransit(UUID jobId, UUID technicianId) {
        Job job = getJobEntity(jobId);
        validateTechOwnership(job, technicianId);

        if (job.getStatus() != JobStatus.ASSIGNED) {
            throw new BadRequestException("Job hanya bisa dalam perjalanan dari status ASSIGNED");
        }

        JobStatus oldStatus = job.getStatus();
        job.setStatus(JobStatus.IN_TRANSIT);
        job.setInTransitAt(OffsetDateTime.now(ZoneId.of("Asia/Jakarta")));

        job = jobRepo.save(job);
        recordHistory(job, oldStatus, JobStatus.IN_TRANSIT, technicianId, null);
        auditService.logAction(technicianId, "TRANSIT_JOB", "JOBS", job.getId(), "Teknisi dalam perjalanan");

        return toResponse(job);
    }

    @Transactional
    public JobResponse startJob(UUID jobId, UUID technicianId) {
        Job job = getJobEntity(jobId);
        validateTechOwnership(job, technicianId);

        if (job.getStatus() != JobStatus.IN_TRANSIT) {
            throw new BadRequestException("Job hanya bisa di-start dari status IN_TRANSIT (Dalam Perjalanan)");
        }

        JobStatus oldStatus = job.getStatus();
        job.setStatus(JobStatus.IN_PROGRESS);
        job.setStartedAt(OffsetDateTime.now(ZoneId.of("Asia/Jakarta")));

        job = jobRepo.save(job);
        recordHistory(job, oldStatus, JobStatus.IN_PROGRESS, technicianId, null);
        auditService.logAction(technicianId, "START_JOB", "JOBS", job.getId(), "Job started");

        return toResponse(job);
    }

    @Transactional
    public JobResponse finishJob(UUID jobId, UUID technicianId) {
        Job job = getJobEntity(jobId);
        validateTechOwnership(job, technicianId);

        if (job.getStatus() != JobStatus.IN_PROGRESS) {
            throw new BadRequestException("Job hanya bisa di-finish dari status IN_PROGRESS");
        }

        if (job.isRequiresPhoto() && !job.isPhotoUploaded()) {
            throw new BadRequestException(
                    "Foto wajib diupload sebelum menyelesaikan pekerjaan. " +
                    "Silakan upload foto terlebih dahulu melalui endpoint upload foto.");
        }

        JobStatus oldStatus = job.getStatus();
        job.setStatus(JobStatus.DONE);
        job.setFinishedAt(OffsetDateTime.now(ZoneId.of("Asia/Jakarta")));

        job = jobRepo.save(job);
        recordHistory(job, oldStatus, JobStatus.DONE, technicianId, null);
        auditService.logAction(technicianId, "FINISH_JOB", "JOBS", job.getId(), "Job finished");

        // Auto-send review link to customer via WhatsApp
        if (job.getCustomerPhone() != null && !job.getCustomerPhone().isBlank()) {
            notificationService.sendReviewLinkToCustomer(job);
        }

        log.info("Job finished: id={}, tech={}", job.getId(), technicianId);
        return toResponse(job);
    }

    @Transactional
    public JobResponse markFollowUp(UUID jobId, UUID technicianId, String reason) {
        Job job = getJobEntity(jobId);
        validateTechOwnership(job, technicianId);

        if (job.getStatus() != JobStatus.IN_PROGRESS) {
            throw new BadRequestException("Job hanya bisa di-mark follow-up dari status IN_PROGRESS");
        }

        JobStatus oldStatus = job.getStatus();
        job.setStatus(JobStatus.NEED_FOLLOWUP);

        job = jobRepo.save(job);
        recordHistory(job, oldStatus, JobStatus.NEED_FOLLOWUP, technicianId, reason);
        auditService.logAction(technicianId, "FOLLOWUP_JOB", "JOBS", job.getId(), "Follow up: " + reason);

        return toResponse(job);
    }

    @Transactional
    public JobResponse cancelJob(UUID jobId, UUID adminId) {
        Job job = getJobEntity(jobId);

        if (job.getStatus() == JobStatus.IN_PROGRESS ||
            job.getStatus() == JobStatus.DONE ||
            job.getStatus() == JobStatus.CLOSED ||
            job.getStatus() == JobStatus.CANCELLED) {
            throw new BadRequestException("Job dengan status " + job.getStatus() + " tidak dapat dibatalkan");
        }

        JobStatus oldStatus = job.getStatus();
        job.setStatus(JobStatus.CANCELLED);

        job = jobRepo.save(job);
        recordHistory(job, oldStatus, JobStatus.CANCELLED, adminId, null);
        auditService.logAction(adminId, "CANCEL_JOB", "JOBS", job.getId(), "Job cancelled");

        log.info("Job cancelled: id={}, previousStatus={}", job.getId(), oldStatus);
        return toResponse(job);
    }

    @Transactional
    public void markPhotoUploaded(UUID jobId) {
        Job job = getJobEntity(jobId);
        job.setPhotoUploaded(true);
        jobRepo.save(job);
    }

    // ==================== INTERNAL HELPERS ====================

    private Job getJobEntity(UUID jobId) {
        UUID tenantId = TenantContext.require();
        return jobRepo.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new NotFoundException("Job not found: " + jobId));
    }

    private User validateTechnician(UUID techId, UUID tenantId) {
        User tech = userRepo.findByIdAndTenantId(techId, tenantId)
                .orElseThrow(() -> new NotFoundException("Teknisi tidak ditemukan: " + techId));
        if (tech.getRole() != UserRole.TECHNICIAN) {
            throw new BadRequestException("User bukan teknisi: " + tech.getFullName());
        }
        if (!tech.isActive()) {
            throw new BadRequestException("Teknisi tidak aktif: " + tech.getFullName());
        }
        return tech;
    }

    private void validateTechOwnership(Job job, UUID technicianId) {
        if (job.getAssignedToId() == null || !technicianId.equals(job.getAssignedToId())) {
            throw new ForbiddenException("Anda tidak memiliki akses ke job ini");
        }
    }

    private void recordHistory(Job job, JobStatus from, JobStatus to, UUID changedBy, String note) {
        JobStatusHistory h = new JobStatusHistory();
        h.setTenantId(job.getTenantId());
        h.setJobId(job.getId());
        h.setFromStatus(from != null ? from.name() : null);
        h.setToStatus(to.name());
        h.setChangedBy(changedBy);
        h.setNote(note);
        historyRepo.save(h);
    }

    private void validateScheduledDate(LocalDate date) {
        if (date == null) return;
        LocalDate todayWIB = LocalDate.now(ZoneId.of("Asia/Jakarta"));
        if (date.isBefore(todayWIB)) {
            throw new BadRequestException("Tanggal jadwal tidak boleh sebelum hari ini (WIB)");
        }
    }

    private JobResponse toResponse(Job job) {
        String assignedName = null;
        if (job.getAssignedToId() != null) {
            assignedName = userRepo.findById(job.getAssignedToId())
                    .map(u -> u.getFullName() != null && !u.getFullName().isBlank()
                            ? u.getFullName() : u.getUsername())
                    .orElse(null);
        }
        return JobResponse.from(job, assignedName);
    }
}
