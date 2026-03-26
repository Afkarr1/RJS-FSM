package com.rjs.fsm.review;

import com.rjs.fsm.audit.AuditService;
import com.rjs.fsm.config.AppProperties;
import com.rjs.fsm.exception.BadRequestException;
import com.rjs.fsm.job.Job;
import com.rjs.fsm.job.JobRepository;
import com.rjs.fsm.review.dto.ReviewResponse;
import com.rjs.fsm.review.dto.SubmitReviewRequest;
import com.rjs.fsm.storage.StorageService;
import com.rjs.fsm.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID JOB_ID    = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    static final String TOKEN   = "abc123validtoken";

    @Mock JobReviewLinkRepository linkRepo;
    @Mock JobReviewRepository reviewRepo;
    @Mock ReviewPhotoRepository reviewPhotoRepo;
    @Mock JobRepository jobRepo;
    @Mock StorageService storageService;
    @Mock AuditService auditService;
    @Mock AppProperties props;

    ReviewService reviewService;

    @BeforeEach
    void setUp() {
        TenantContext.set(TENANT_ID);
        when(props.getBaseUrl()).thenReturn("http://localhost:8080");
        reviewService = new ReviewService(
                linkRepo, reviewRepo, reviewPhotoRepo, jobRepo,
                storageService, auditService, props);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private JobReviewLink validLink() {
        JobReviewLink link = new JobReviewLink();
        link.setId(UUID.randomUUID());
        link.setTenantId(TENANT_ID);
        link.setJobId(JOB_ID);
        link.setToken(TOKEN);
        link.setExpiresAt(OffsetDateTime.now().plusHours(24));
        // usedAt = null → not used
        return link;
    }

    private Job buildJob() {
        Job j = new Job();
        j.setId(JOB_ID);
        j.setTenantId(TENANT_ID);
        j.setTitle("Repair AC");
        j.setCustomerName("Budi");
        return j;
    }

    private SubmitReviewRequest buildRequest(int rating) {
        SubmitReviewRequest req = new SubmitReviewRequest();
        req.setRating(rating);
        req.setNote("Great service!");
        return req;
    }

    // ── getReviewPage ────────────────────────────────────────────────────────

    @Test
    void getReviewPage_validToken_returnsActiveResponse() {
        when(linkRepo.findByToken(TOKEN)).thenReturn(Optional.of(validLink()));
        when(jobRepo.findById(JOB_ID)).thenReturn(Optional.of(buildJob()));

        ReviewResponse res = reviewService.getReviewPage(TOKEN);

        assertFalse(res.isAlreadyReviewed());
        assertFalse(res.isExpired());
        assertEquals("Repair AC", res.getJobTitle());
    }

    @Test
    void getReviewPage_usedToken_returnsAlreadyReviewedResponse() {
        JobReviewLink usedLink = validLink();
        usedLink.setUsedAt(OffsetDateTime.now().minusHours(1));

        when(linkRepo.findByToken(TOKEN)).thenReturn(Optional.of(usedLink));
        when(jobRepo.findById(JOB_ID)).thenReturn(Optional.of(buildJob()));

        ReviewResponse res = reviewService.getReviewPage(TOKEN);

        assertTrue(res.isAlreadyReviewed());
    }

    @Test
    void getReviewPage_expiredToken_returnsExpiredResponse() {
        JobReviewLink expiredLink = validLink();
        expiredLink.setExpiresAt(OffsetDateTime.now().minusHours(1));

        when(linkRepo.findByToken(TOKEN)).thenReturn(Optional.of(expiredLink));
        when(jobRepo.findById(JOB_ID)).thenReturn(Optional.of(buildJob()));

        ReviewResponse res = reviewService.getReviewPage(TOKEN);

        assertTrue(res.isExpired());
    }

    // ── submitReview ─────────────────────────────────────────────────────────

    @Test
    void submitReview_usedLink_throwsBadRequest() {
        JobReviewLink usedLink = validLink();
        usedLink.setUsedAt(OffsetDateTime.now().minusHours(1));

        when(linkRepo.findByToken(TOKEN)).thenReturn(Optional.of(usedLink));

        assertThrows(BadRequestException.class,
                () -> reviewService.submitReview(TOKEN, buildRequest(5), null));
    }

    @Test
    void submitReview_expiredLink_throwsBadRequest() {
        JobReviewLink expiredLink = validLink();
        expiredLink.setExpiresAt(OffsetDateTime.now().minusHours(1));

        when(linkRepo.findByToken(TOKEN)).thenReturn(Optional.of(expiredLink));

        assertThrows(BadRequestException.class,
                () -> reviewService.submitReview(TOKEN, buildRequest(4), null));
    }

    @Test
    void submitReview_tooManyPhotos_throwsBadRequest() {
        when(linkRepo.findByToken(TOKEN)).thenReturn(Optional.of(validLink()));

        List<MultipartFile> sixPhotos = List.of(
                mock(MultipartFile.class), mock(MultipartFile.class),
                mock(MultipartFile.class), mock(MultipartFile.class),
                mock(MultipartFile.class), mock(MultipartFile.class));

        assertThrows(BadRequestException.class,
                () -> reviewService.submitReview(TOKEN, buildRequest(3), sixPhotos));
    }

    @Test
    void submitReview_validRequest_savesReviewAndMarksLinkUsed() {
        JobReviewLink link = validLink();
        when(linkRepo.findByToken(TOKEN)).thenReturn(Optional.of(link));
        when(jobRepo.findById(JOB_ID)).thenReturn(Optional.of(buildJob()));
        when(reviewRepo.save(any(JobReview.class))).thenAnswer(inv -> inv.getArgument(0));
        when(linkRepo.save(any(JobReviewLink.class))).thenAnswer(inv -> inv.getArgument(0));

        reviewService.submitReview(TOKEN, buildRequest(5), null);

        verify(reviewRepo).save(argThat(r -> r.getRating() == 5));
        verify(linkRepo).save(argThat(l -> l.getUsedAt() != null));
    }

    // ── createReviewLink ─────────────────────────────────────────────────────

    @Test
    void createReviewLink_setsExpiry48h() {
        Job job = buildJob();
        when(linkRepo.save(any(JobReviewLink.class))).thenAnswer(inv -> inv.getArgument(0));

        OffsetDateTime before = OffsetDateTime.now().plusHours(47);
        reviewService.createReviewLink(job);
        OffsetDateTime after = OffsetDateTime.now().plusHours(49);

        verify(linkRepo).save(argThat(link ->
                link.getExpiresAt().isAfter(before) && link.getExpiresAt().isBefore(after)
        ));
    }
}
