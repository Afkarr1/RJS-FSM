package com.rjs.fsm.job.dto;

import com.rjs.fsm.job.JobStatusHistory;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class JobHistoryResponse {
    private UUID id;
    private String fromStatus;
    private String toStatus;
    private UUID changedBy;
    private String changedByName;
    private OffsetDateTime changedAt;
    private String note;

    public static JobHistoryResponse from(JobStatusHistory h, String changedByName) {
        return new JobHistoryResponse(
                h.getId(), h.getFromStatus(), h.getToStatus(),
                h.getChangedBy(), changedByName, h.getChangedAt(),
                h.getNote()
        );
    }
}
