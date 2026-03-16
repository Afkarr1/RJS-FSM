package com.rjs.fsm.job.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter
public class RescheduleJobRequest {
    @NotNull(message = "Tanggal jadwal wajib diisi")
    @Future(message = "Tanggal jadwal harus di masa depan")
    private LocalDate scheduledDate;

    private UUID technicianId;
}
