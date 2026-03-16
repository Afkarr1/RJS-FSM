package com.rjs.fsm.review;

import com.rjs.fsm.audit.AuditService;
import com.rjs.fsm.exception.BadRequestException;
import com.rjs.fsm.exception.NotFoundException;
import com.rjs.fsm.job.Job;
import com.rjs.fsm.job.JobRepository;
import com.rjs.fsm.review.dto.ReviewResponse;
import com.rjs.fsm.review.dto.SubmitReviewRequest;
import com.rjs.fsm.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final JobReviewLinkRepository linkRepo;
    private final JobReviewRepository reviewRepo;
    private final JobRepository jobRepo;
    private final AuditService auditService;

    public ReviewService(JobReviewLinkRepository linkRepo, JobReviewRepository reviewRepo,
                         JobRepository jobRepo, AuditService auditService) {
        this.linkRepo = linkRepo;
        this.reviewRepo = reviewRepo;
        this.jobRepo = jobRepo;
        this.auditService = auditService;
    }

    public String createReviewLink(Job job) {
        String token = UUID.randomUUID().toString().replace("-", "");

        JobReviewLink link = new JobReviewLink();
        link.setTenantId(job.getTenantId());
        link.setJobId(job.getId());
        link.setToken(token);
        link.setExpiresAt(OffsetDateTime.now().plusHours(48));

        linkRepo.save(link);
        log.info("Review link created for job {}: token={}", job.getId(), token);
        return token;
    }

    @Transactional(readOnly = true)
    public ReviewResponse getReviewPage(String token) {
        JobReviewLink link = linkRepo.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Link review tidak ditemukan atau tidak valid"));

        Job job = jobRepo.findById(link.getJobId())
                .orElseThrow(() -> new NotFoundException("Job tidak ditemukan"));

        if (link.isUsed()) {
            return new ReviewResponse(job.getTitle(), job.getCustomerName(),
                    "Review sudah pernah disubmit. Terima kasih!", true, false);
        }
        if (link.isExpired()) {
            return new ReviewResponse(job.getTitle(), job.getCustomerName(),
                    "Link review sudah expired.", false, true);
        }

        return new ReviewResponse(job.getTitle(), job.getCustomerName(),
                "Silakan berikan rating dan ulasan Anda.", false, false);
    }

    @Transactional
    public ReviewResponse submitReview(String token, SubmitReviewRequest req) {
        JobReviewLink link = linkRepo.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Link review tidak ditemukan"));

        if (link.isUsed()) {
            throw new BadRequestException("Review sudah pernah disubmit");
        }
        if (link.isExpired()) {
            throw new BadRequestException("Link review sudah expired");
        }

        Job job = jobRepo.findById(link.getJobId())
                .orElseThrow(() -> new NotFoundException("Job tidak ditemukan"));

        // Save review
        JobReview review = new JobReview();
        review.setTenantId(link.getTenantId());
        review.setJobId(link.getJobId());
        review.setRating(req.getRating());
        review.setNote(req.getNote());
        reviewRepo.save(review);

        // Mark link as used
        link.setUsedAt(OffsetDateTime.now());
        linkRepo.save(link);

        // Audit
        TenantContext.set(link.getTenantId());
        try {
            auditService.logAction(null, "SUBMIT_REVIEW", "JOB_REVIEWS", review.getId(),
                    "Customer review: rating=" + req.getRating() + " for job " + job.getTitle());
        } finally {
            TenantContext.clear();
        }

        log.info("Review submitted for job {}: rating={}", job.getId(), req.getRating());
        return new ReviewResponse(job.getTitle(), job.getCustomerName(),
                "Terima kasih atas review Anda!", false, false);
    }
}
