package com.rjs.fsm.job;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    Optional<Job> findByIdAndTenantId(UUID id, UUID tenantId);

    List<Job> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<Job> findByTenantIdAndStatusOrderByScheduledDateAsc(UUID tenantId, JobStatus status);

    List<Job> findByAssignedToIdAndTenantIdOrderByScheduledDateAsc(UUID assignedToId, UUID tenantId);

    List<Job> findByAssignedToIdAndTenantIdAndStatusInOrderByScheduledDateAsc(
            UUID assignedToId, UUID tenantId, List<JobStatus> statuses);

    List<Job> findByTenantIdAndStatusAndPhotoUploadedFalse(UUID tenantId, JobStatus status);
}
