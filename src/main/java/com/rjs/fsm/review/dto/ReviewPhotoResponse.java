package com.rjs.fsm.review.dto;

import com.rjs.fsm.review.ReviewPhoto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ReviewPhotoResponse {
    private UUID id;
    private String fileName;
    private String mimeType;
    private Long sizeBytes;
    private String downloadUrl;
    private OffsetDateTime uploadedAt;

    public static ReviewPhotoResponse from(ReviewPhoto photo, String baseUrl) {
        return new ReviewPhotoResponse(
                photo.getId(),
                photo.getFileName(),
                photo.getMimeType(),
                photo.getSizeBytes(),
                baseUrl + "/api/public/review-photos/" + photo.getId() + "/download",
                photo.getUploadedAt()
        );
    }
}
