package com.rjs.fsm.photo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobPhotoRepository extends JpaRepository<JobPhoto, UUID> {

    List<JobPhoto> findByJobIdAndTenantIdOrderByUploadedAtDesc(UUID jobId, UUID tenantId);

    boolean existsByJobIdAndTenantId(UUID jobId, UUID tenantId);
}
