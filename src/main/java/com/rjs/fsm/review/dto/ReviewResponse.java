package com.rjs.fsm.review.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReviewResponse {
    private String jobTitle;
    private String customerName;
    private String message;
    private boolean alreadyReviewed;
    private boolean expired;
}
