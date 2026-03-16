package com.rjs.fsm.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JobReviewRepository extends JpaRepository<JobReview, UUID> {

    Optional<JobReview> findByJobId(UUID jobId);

    boolean existsByJobId(UUID jobId);
}
