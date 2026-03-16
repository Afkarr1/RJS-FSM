package com.rjs.fsm.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JobReviewLinkRepository extends JpaRepository<JobReviewLink, UUID> {

    Optional<JobReviewLink> findByToken(String token);

    Optional<JobReviewLink> findByJobId(UUID jobId);
}
