package com.rjs.fsm.job.dto;

import com.rjs.fsm.job.JobStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateJobStatusRequest {
    @NotNull
    private JobStatus status;
}
