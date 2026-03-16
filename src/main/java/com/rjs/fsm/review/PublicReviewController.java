package com.rjs.fsm.review;

import com.rjs.fsm.review.dto.ReviewResponse;
import com.rjs.fsm.review.dto.SubmitReviewRequest;
import com.rjs.fsm.storage.StorageService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/public")
public class PublicReviewController {

    private final ReviewService reviewService;
    private final StorageService storageService;

    public PublicReviewController(ReviewService reviewService, StorageService storageService) {
        this.reviewService = reviewService;
        this.storageService = storageService;
    }

    @GetMapping("/reviews/{token}")
    public ReviewResponse getReviewPage(@PathVariable String token) {
        return reviewService.getReviewPage(token);
    }

    @PostMapping(value = "/reviews/{token}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReviewResponse submitReview(@PathVariable String token,
                                       @Valid @RequestPart("review") SubmitReviewRequest req,
                                       @RequestPart(value = "photos", required = false) List<MultipartFile> photos) {
        return reviewService.submitReview(token, req, photos);
    }

    @GetMapping("/review-photos/{photoId}/download")
    public ResponseEntity<Resource> downloadReviewPhoto(@PathVariable UUID photoId) throws MalformedURLException {
        ReviewPhoto photo = reviewService.getReviewPhoto(photoId);
        Path path = storageService.resolve(photo.getFilePath());
        Resource resource = new UrlResource(path.toUri());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        photo.getMimeType() != null ? photo.getMimeType() : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + photo.getFileName() + "\"")
                .body(resource);
    }
}
