package com.rjs.fsm.review;

import com.rjs.fsm.audit.AuditService;
import com.rjs.fsm.config.AppProperties;
import com.rjs.fsm.exception.BadRequestException;
import com.rjs.fsm.exception.NotFoundException;
import com.rjs.fsm.job.Job;
import com.rjs.fsm.job.JobRepository;
import com.rjs.fsm.review.dto.ReviewPhotoResponse;
import com.rjs.fsm.review.dto.ReviewResponse;
import com.rjs.fsm.review.dto.SubmitReviewRequest;
import com.rjs.fsm.storage.StorageService;
import com.rjs.fsm.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);
    private static final int MAX_REVIEW_PHOTOS = 5;

    private final JobReviewLinkRepository linkRepo;
    private final JobReviewRepository reviewRepo;
    private final ReviewPhotoRepository reviewPhotoRepo;
    private final JobRepository jobRepo;
    private final StorageService storageService;
    private final AuditService auditService;
    private final String baseUrl;

    public ReviewService(JobReviewLinkRepository linkRepo, JobReviewRepository reviewRepo,
                         ReviewPhotoRepository reviewPhotoRepo, JobRepository jobRepo,
                         StorageService storageService, AuditService auditService,
                         AppProperties props) {
        this.linkRepo = linkRepo;
        this.reviewRepo = reviewRepo;
        this.reviewPhotoRepo = reviewPhotoRepo;
        this.jobRepo = jobRepo;
        this.storageService = storageService;
        this.auditService = auditService;
        this.baseUrl = props.getBaseUrl();
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
    public ReviewResponse submitReview(String token, SubmitReviewRequest req, List<MultipartFile> photos) {
        JobReviewLink link = linkRepo.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Link review tidak ditemukan"));

        if (link.isUsed()) {
            throw new BadRequestException("Review sudah pernah disubmit");
        }
        if (link.isExpired()) {
            throw new BadRequestException("Link review sudah expired");
        }

        if (photos != null && photos.size() > MAX_REVIEW_PHOTOS) {
            throw new BadRequestException("Maksimal " + MAX_REVIEW_PHOTOS + " foto per review");
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

        // Save review photos
        List<ReviewPhotoResponse> photoResponses = new ArrayList<>();
        if (photos != null) {
            for (MultipartFile file : photos) {
                if (file.isEmpty()) continue;

                String subDir = "reviews/" + job.getId();
                String relativePath = storageService.store(file, subDir);

                ReviewPhoto rp = new ReviewPhoto();
                rp.setTenantId(link.getTenantId());
                rp.setReviewId(review.getId());
                rp.setJobId(link.getJobId());
                rp.setFilePath(relativePath);
                rp.setFileName(file.getOriginalFilename());
                rp.setMimeType(file.getContentType());
                rp.setSizeBytes(file.getSize());
                reviewPhotoRepo.save(rp);

                photoResponses.add(ReviewPhotoResponse.from(rp, baseUrl));
            }
        }

        // Mark link as used
        link.setUsedAt(OffsetDateTime.now());
        linkRepo.save(link);

        // Audit
        TenantContext.set(link.getTenantId());
        try {
            auditService.logAction(null, "SUBMIT_REVIEW", "JOB_REVIEWS", review.getId(),
                    "Customer review: rating=" + req.getRating() + ", photos=" + photoResponses.size()
                            + " for job " + job.getTitle());
        } finally {
            TenantContext.clear();
        }

        log.info("Review submitted for job {}: rating={}, photos={}", job.getId(), req.getRating(), photoResponses.size());
        return new ReviewResponse(job.getTitle(), job.getCustomerName(),
                "Terima kasih atas review Anda!", false, false, photoResponses);
    }

    @Transactional(readOnly = true)
    public ReviewPhoto getReviewPhoto(UUID photoId) {
        return reviewPhotoRepo.findById(photoId)
                .orElseThrow(() -> new NotFoundException("Foto review tidak ditemukan"));
    }

    @Transactional(readOnly = true)
    public List<ReviewPhotoResponse> getReviewPhotos(UUID reviewId) {
        return reviewPhotoRepo.findByReviewIdOrderByUploadedAtAsc(reviewId).stream()
                .map(p -> ReviewPhotoResponse.from(p, baseUrl))
                .toList();
    }
}
