package com.rjs.fsm.job.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter
public class CreateJobRequest {
    @NotBlank
    private String title;

    private String description;
    private String customerName;
    private String customerPhone;
    private String address;
    private LocalDate scheduledDate;
    private UUID assignToId;
    private Boolean requiresPhoto;
}
