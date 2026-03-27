package com.rjs.fsm.job.dto;

import com.rjs.fsm.job.Job;
import com.rjs.fsm.job.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class JobResponse {
    private UUID id;
    private String title;
    private String description;
    private String customerName;
    private String customerPhone;
    private String address;
    private JobStatus status;
    private UUID assignedToId;
    private String assignedToName;
    private UUID createdById;
    private LocalDate scheduledDate;
    private boolean requiresPhoto;
    private boolean photoUploaded;
    private OffsetDateTime assignedAt;
    private OffsetDateTime inTransitAt;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private OffsetDateTime closedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static JobResponse from(Job j, String assignedToName) {
        return new JobResponse(
                j.getId(), j.getTitle(), j.getDescription(),
                j.getCustomerName(), j.getCustomerPhone(), j.getAddress(),
                j.getStatus(), j.getAssignedToId(), assignedToName,
                j.getCreatedById(), j.getScheduledDate(),
                j.isRequiresPhoto(), j.isPhotoUploaded(),
                j.getAssignedAt(), j.getInTransitAt(), j.getStartedAt(), j.getFinishedAt(), j.getClosedAt(),
                j.getCreatedAt(), j.getUpdatedAt()
        );
    }
}
