package com.rjs.fsm.review;

import com.rjs.fsm.audit.AuditService;
import com.rjs.fsm.config.AppProperties;
import com.rjs.fsm.exception.BadRequestException;
import com.rjs.fsm.exception.NotFoundException;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    ReviewService reviewService;

    @BeforeEach
    void setUp() {
        TenantContext.set(TENANT_ID);
        AppProperties props = new AppProperties();
        props.setBaseUrl("http://localhost:8080");
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

    private MockMultipartFile mockFile(String name) {
        return new MockMultipartFile(name, name + ".jpg", "image/jpeg",
                new byte[]{1, 2, 3}); // non-empty
    }

    // ── getReviewPage ────────────────────────────────────────────────────────

    @Test
    void getReviewPage_validToken_returnsActiveResponse() {
        when(linkRepo.findByToken(TOKEN)).thenReturn(Optional.of(validLink()));
        when(jobRepo.findById(JOB_ID)).thenReturn(Optional.of(buildJob()));

        ReviewResponse res = reviewService.getReviewPage(TOKEN);

        assertFalse(res.isAlreadyReviewed());
        assertFalse(res.isExpired());
        assertTrue(res.getMessage().contains("Silakan"));
    }

    @Test
    void getReviewPage_usedToken_returnsAlreadyReviewedResponse() {
        JobReviewLink usedLink = validLink();
        usedLink.setUsedAt(OffsetDateTime.now().minusHours(1));

        when(linkRepo.findByToken(TOKEN)).thenReturn(Optional.of(usedLink));
        when(jobRepo.findById(JOB_ID)).thenReturn(Optional.of(buildJob()));

        ReviewResponse res = reviewService.getReviewPage(TOKEN);

        assertTrue(res.isAlreadyReviewed());
        assertFalse(res.isExpired());
    }

    @Test
    void getReviewPage_expiredToken_returnsExpiredResponse() {
        JobReviewLink expiredLink = validLink();
        expiredLink.setExpiresAt(OffsetDateTime.now().minusHours(1));

        when(linkRepo.findByToken(TOKEN)).thenReturn(Optional.of(expiredLink));
        when(jobRepo.findById(JOB_ID)).thenReturn(Optional.of(buildJob()));

        ReviewResponse res = reviewService.getReviewPage(TOKEN);

        assertTrue(res.isExpired());
        assertFalse(res.isAlreadyReviewed());
    }

    @Test
    void getReviewPage_includesJobTitleAndCustomerName() {
        when(linkRepo.findByToken(TOKEN)).thenReturn(Optional.of(validLink()));
        when(jobRepo.findById(JOB_ID)).thenReturn(Optional.of(buildJob()));

        ReviewResponse res = reviewService.getReviewPage(TOKEN);

        assertEquals("Repair AC", res.getJobTitle());
        assertEquals("Budi", res.getCustomerName());
    }

    @Test
    void getReviewPage_throwsNotFound_whenTokenNotFound() {
        when(linkRepo.findByToken(TOKEN)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> reviewService.getReviewPage(TOKEN));
    }

    @Test
    void getReviewPage_throwsNotFound_whenJobMissing() {
        when(linkRepo.findByToken(TOKEN)).thenReturn(Optional.of(validLink()));
        when(jobRepo.findById(JOB_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> reviewService.getReviewPage(TOKEN));
    }

    // ── submitReview ─────────────────────────────────────────────────────────

    @Test
    void submitReview_usedLink_throwsBadRequest() {
        JobReviewLink usedLink = validLink();
        usedLink.setUsedAt(OffsetDateTime.now().minusHours(1));

        when(linkRepo.findByToken(TOKEN)).thenReturn(Optional.of(usedLink));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> reviewService.submitReview(TOKEN, buildRequest(5), null));
        assertTrue(ex.getMessage().contains("sudah pernah"));
        verify(reviewRepo, never()).save(any());
    }

    @Test
    void submitReview_expiredLink_throwsBadRequest() {
        JobReviewLink expiredLink = validLink();
        expiredLink.setExpiresAt(OffsetDateTime.now().minusHours(1));

        when(linkRepo.findByToken(TOKEN)).thenReturn(Optional.of(expiredLink));

        assertThrows(BadRequestException.class,
                () -> reviewService.submitReview(TOKEN, buildRequest(4), null));
        verify(reviewRepo, never()).save(any());
    }

    @Test
    void submitReview_tooManyPhotos_throwsBadRequest() {
        when(linkRepo.findByToken(TOKEN)).thenReturn(Optional.of(validLink()));

        List<MockMultipartFile> sixPhotos = List.of(
                mockFile("p1"), mockFile("p2"), mockFile("p3"),
                mockFile("p4"), mockFile("p5"), mockFile("p6"));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> reviewService.submitReview(TOKEN, buildRequest(3), List.copyOf(sixPhotos)));
        assertTrue(ex.getMessage().contains("Maksimal 5"));
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

    @Test
    void submitReview_savesPhotosWhenProvided() {
        when(linkRepo.findByToken(TOKEN)).thenReturn(Optional.of(validLink()));
        when(jobRepo.findById(JOB_ID)).thenReturn(Optional.of(buildJob()));
        when(reviewRepo.save(any(JobReview.class))).thenAnswer(inv -> inv.getArgument(0));
        when(linkRepo.save(any(JobReviewLink.class))).thenAnswer(inv -> inv.getArgument(0));
        when(storageService.store(any(), anyString())).thenReturn("reviews/path/file.jpg");

        List<MockMultipartFile> photos = List.of(mockFile("p1"), mockFile("p2"));
        reviewService.submitReview(TOKEN, buildRequest(4), List.copyOf(photos));

        verify(storageService, times(2)).store(any(), anyString());
        verify(reviewPhotoRepo, times(2)).save(any(ReviewPhoto.class));
    }

    @Test
    void submitReview_skipsEmptyFiles() {
        MockMultipartFile emptyFile = new MockMultipartFile("empty", "empty.jpg", "image/jpeg", new byte[0]);
        MockMultipartFile realFile = mockFile("real");

        when(linkRepo.findByToken(TOKEN)).thenReturn(Optional.of(validLink()));
        when(jobRepo.findById(JOB_ID)).thenReturn(Optional.of(buildJob()));
        when(reviewRepo.save(any(JobReview.class))).thenAnswer(inv -> inv.getArgument(0));
        when(linkRepo.save(any(JobReviewLink.class))).thenAnswer(inv -> inv.getArgument(0));
        when(storageService.store(any(), anyString())).thenReturn("path/file.jpg");

        reviewService.submitReview(TOKEN, buildRequest(5), List.of(emptyFile, realFile));

        verify(storageService, times(1)).store(any(), anyString());
        verify(reviewPhotoRepo, times(1)).save(any(ReviewPhoto.class));
    }

    @Test
    void submitReview_succeedsWithNullPhotos() {
        when(linkRepo.findByToken(TOKEN)).thenReturn(Optional.of(validLink()));
        when(jobRepo.findById(JOB_ID)).thenReturn(Optional.of(buildJob()));
        when(reviewRepo.save(any(JobReview.class))).thenAnswer(inv -> inv.getArgument(0));
        when(linkRepo.save(any(JobReviewLink.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> reviewService.submitReview(TOKEN, buildRequest(5), null));
        verify(reviewPhotoRepo, never()).save(any());
    }

    @Test
    void submitReview_succeedsWithEmptyPhotoList() {
        when(linkRepo.findByToken(TOKEN)).thenReturn(Optional.of(validLink()));
        when(jobRepo.findById(JOB_ID)).thenReturn(Optional.of(buildJob()));
        when(reviewRepo.save(any(JobReview.class))).thenAnswer(inv -> inv.getArgument(0));
        when(linkRepo.save(any(JobReviewLink.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> reviewService.submitReview(TOKEN, buildRequest(3), List.of()));
        verify(reviewPhotoRepo, never()).save(any());
    }

    @Test
    void submitReview_restoresTenantContext_whenPreviousContextWasSet() {
        TenantContext.set(TENANT_ID); // explicitly set before call

        when(linkRepo.findByToken(TOKEN)).thenReturn(Optional.of(validLink()));
        when(jobRepo.findById(JOB_ID)).thenReturn(Optional.of(buildJob()));
        when(reviewRepo.save(any(JobReview.class))).thenAnswer(inv -> inv.getArgument(0));
        when(linkRepo.save(any(JobReviewLink.class))).thenAnswer(inv -> inv.getArgument(0));

        reviewService.submitReview(TOKEN, buildRequest(5), null);

        assertEquals(TENANT_ID, TenantContext.get());
    }

    @Test
    void submitReview_clearsTenantContext_whenNoPreviousContextWasSet() {
        TenantContext.clear(); // simulate no context before call

        when(linkRepo.findByToken(TOKEN)).thenReturn(Optional.of(validLink()));
        when(jobRepo.findById(JOB_ID)).thenReturn(Optional.of(buildJob()));
        when(reviewRepo.save(any(JobReview.class))).thenAnswer(inv -> inv.getArgument(0));
        when(linkRepo.save(any(JobReviewLink.class))).thenAnswer(inv -> inv.getArgument(0));

        reviewService.submitReview(TOKEN, buildRequest(5), null);

        assertNull(TenantContext.get());
    }

    @Test
    void submitReview_throwsNotFound_whenTokenNotFound() {
        when(linkRepo.findByToken(TOKEN)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> reviewService.submitReview(TOKEN, buildRequest(5), null));
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

    @Test
    void createReviewLink_savesCorrectFields() {
        Job job = buildJob();
        when(linkRepo.save(any(JobReviewLink.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<JobReviewLink> captor = ArgumentCaptor.forClass(JobReviewLink.class);
        reviewService.createReviewLink(job);

        verify(linkRepo).save(captor.capture());
        JobReviewLink saved = captor.getValue();
        assertEquals(JOB_ID, saved.getJobId());
        assertEquals(TENANT_ID, saved.getTenantId());
        assertNotNull(saved.getToken());
        assertEquals(32, saved.getToken().length()); // UUID without dashes = 32 chars
        assertFalse(saved.getToken().contains("-"));
    }

    @Test
    void createReviewLink_returnsToken() {
        Job job = buildJob();
        when(linkRepo.save(any(JobReviewLink.class))).thenAnswer(inv -> inv.getArgument(0));

        String token = reviewService.createReviewLink(job);

        assertNotNull(token);
        assertEquals(32, token.length());
    }
}
