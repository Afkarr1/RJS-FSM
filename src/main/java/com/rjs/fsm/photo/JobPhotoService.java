package com.rjs.fsm.photo;

import com.rjs.fsm.audit.AuditService;
import com.rjs.fsm.config.AppProperties;
import com.rjs.fsm.exception.BadRequestException;
import com.rjs.fsm.exception.ForbiddenException;
import com.rjs.fsm.exception.NotFoundException;
import com.rjs.fsm.job.Job;
import com.rjs.fsm.job.JobRepository;
import com.rjs.fsm.job.JobService;
import com.rjs.fsm.job.JobStatus;
import com.rjs.fsm.photo.dto.JobPhotoResponse;
import com.rjs.fsm.storage.StorageService;
import com.rjs.fsm.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
public class JobPhotoService {

    private final JobPhotoRepository photoRepo;
    private final JobRepository jobRepo;
    private final JobService jobService;
    private final StorageService storageService;
    private final AuditService auditService;
    private final String baseUrl;

    public JobPhotoService(JobPhotoRepository photoRepo, JobRepository jobRepo,
                           JobService jobService, StorageService storageService,
                           AuditService auditService, AppProperties props) {
        this.photoRepo = photoRepo;
        this.jobRepo = jobRepo;
        this.jobService = jobService;
        this.storageService = storageService;
        this.auditService = auditService;
        this.baseUrl = props.getBaseUrl();
    }

    @Transactional
    public JobPhotoResponse uploadPhoto(UUID jobId, MultipartFile file, UUID uploaderId) {
        UUID tenantId = TenantContext.require();

        Job job = jobRepo.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new NotFoundException("Job not found: " + jobId));

        // Only assigned technician or admin can upload
        if (job.getAssignedToId() != null && !job.getAssignedToId().equals(uploaderId)) {
            throw new ForbiddenException("Hanya teknisi yang ditugaskan yang bisa upload foto");
        }

        // Job must be in progress or assigned
        if (job.getStatus() != JobStatus.IN_PROGRESS && job.getStatus() != JobStatus.ASSIGNED) {
            throw new BadRequestException("Foto hanya bisa diupload saat job sedang dikerjakan (IN_PROGRESS/ASSIGNED)");
        }

        String relativePath = storageService.store(file, "jobs/" + jobId);

        JobPhoto photo = new JobPhoto();
        photo.setTenantId(tenantId);
        photo.setJobId(jobId);
        photo.setFilePath(relativePath);
        photo.setFileName(file.getOriginalFilename());
        photo.setMimeType(file.getContentType());
        photo.setSizeBytes(file.getSize());
        photo.setUploadedBy(uploaderId);

        photo = photoRepo.save(photo);

        // Mark photo uploaded on job
        jobService.markPhotoUploaded(jobId);

        auditService.logAction(uploaderId, "UPLOAD_PHOTO", "JOB_PHOTOS", photo.getId(),
                "Photo uploaded for job: " + jobId);

        return JobPhotoResponse.from(photo, baseUrl);
    }

    @Transactional(readOnly = true)
    public List<JobPhotoResponse> listPhotos(UUID jobId) {
        UUID tenantId = TenantContext.require();
        return photoRepo.findByJobIdAndTenantIdOrderByUploadedAtDesc(jobId, tenantId).stream()
                .map(p -> JobPhotoResponse.from(p, baseUrl)).toList();
    }

    @Transactional(readOnly = true)
    public JobPhoto getPhoto(UUID photoId) {
        return photoRepo.findById(photoId)
                .orElseThrow(() -> new NotFoundException("Photo not found: " + photoId));
    }
}
