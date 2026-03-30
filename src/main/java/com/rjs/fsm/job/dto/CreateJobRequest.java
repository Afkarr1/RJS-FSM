package com.rjs.fsm.job.dto;

import com.rjs.fsm.job.JobType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter
public class CreateJobRequest {
    @NotBlank(message = "Judul pekerjaan wajib diisi")
    @Size(max = 255, message = "Judul maksimal 255 karakter")
    private String title;

    private String description;

    @Size(max = 150, message = "Nama customer maksimal 150 karakter")
    private String customerName;

    @Size(max = 20, message = "Nomor telepon maksimal 20 karakter")
    private String customerPhone;

    private String address;
    private LocalDate scheduledDate;
    private UUID assignToId;
    private Boolean requiresPhoto;

    private JobType jobType = JobType.FIELD_SERVICE;

    @Size(max = 100, message = "No. seri maksimal 100 karakter")
    private String machineSerialNo;

    @Size(max = 500, message = "Estimasi maksimal 500 karakter")
    private String estimateText;
}
