package com.rjs.fsm.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SubmitReviewRequest {
    @NotNull @Min(1) @Max(5)
    private Integer rating;

    private String note;
}
