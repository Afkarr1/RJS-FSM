package com.rjs.fsm.review.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ReviewResponse {
    private String jobTitle;
    private String customerName;
    private String message;
    private boolean alreadyReviewed;
    private boolean expired;
    private List<ReviewPhotoResponse> photos;

    public ReviewResponse(String jobTitle, String customerName, String message,
                          boolean alreadyReviewed, boolean expired) {
        this(jobTitle, customerName, message, alreadyReviewed, expired, null);
    }
}
