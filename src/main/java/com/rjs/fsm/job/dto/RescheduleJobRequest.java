package com.rjs.fsm.job.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter
public class RescheduleJobRequest {
    @NotNull(message = "Tanggal jadwal wajib diisi")
    @FutureOrPresent(message = "Tanggal jadwal tidak boleh sebelum hari ini")
    private LocalDate scheduledDate;

    private UUID technicianId;
}
