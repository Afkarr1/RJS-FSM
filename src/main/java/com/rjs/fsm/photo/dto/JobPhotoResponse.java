package com.rjs.fsm.photo.dto;

import com.rjs.fsm.photo.JobPhoto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class JobPhotoResponse {
    private UUID id;
    private UUID jobId;
    private String fileName;
    private String mimeType;
    private Long sizeBytes;
    private UUID uploadedBy;
    private OffsetDateTime uploadedAt;
    private String downloadUrl;

    public static JobPhotoResponse from(JobPhoto p, String baseUrl) {
        return new JobPhotoResponse(
                p.getId(), p.getJobId(), p.getFileName(),
                p.getMimeType(), p.getSizeBytes(), p.getUploadedBy(), p.getUploadedAt(),
                baseUrl + "/api/photos/" + p.getId() + "/download"
        );
    }
}
