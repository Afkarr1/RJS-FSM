package com.rjs.fsm.job.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FollowUpRequest {
    @NotBlank(message = "Alasan follow up wajib diisi")
    @Size(max = 500, message = "Alasan maksimal 500 karakter")
    private String reason;
}
