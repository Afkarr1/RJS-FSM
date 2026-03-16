package com.rjs.fsm.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewPhotoRepository extends JpaRepository<ReviewPhoto, UUID> {
    List<ReviewPhoto> findByReviewIdOrderByUploadedAtAsc(UUID reviewId);
    List<ReviewPhoto> findByJobIdOrderByUploadedAtAsc(UUID jobId);
}
