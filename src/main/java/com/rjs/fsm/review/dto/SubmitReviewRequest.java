package com.rjs.fsm.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SubmitReviewRequest {
    @NotNull @Min(1) @Max(5)
    private Integer rating;

    @Size(max = 1000, message = "Review maksimal 1000 karakter")
    private String note;
}
