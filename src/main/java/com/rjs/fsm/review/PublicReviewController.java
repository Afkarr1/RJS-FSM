package com.rjs.fsm.review;

import com.rjs.fsm.review.dto.ReviewResponse;
import com.rjs.fsm.review.dto.SubmitReviewRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/reviews")
public class PublicReviewController {

    private final ReviewService reviewService;

    public PublicReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/{token}")
    public ReviewResponse getReviewPage(@PathVariable String token) {
        return reviewService.getReviewPage(token);
    }

    @PostMapping("/{token}")
    public ReviewResponse submitReview(@PathVariable String token,
                                        @Valid @RequestBody SubmitReviewRequest req) {
        return reviewService.submitReview(token, req);
    }
}
