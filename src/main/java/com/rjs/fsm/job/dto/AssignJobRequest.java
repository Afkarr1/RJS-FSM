package com.rjs.fsm.job.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter
public class AssignJobRequest {
    @NotNull
    private UUID technicianId;

    private LocalDate scheduledDate;
}
