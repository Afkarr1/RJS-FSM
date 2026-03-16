package com.rjs.fsm.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private final OffsetDateTime timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final String traceId;
    private final List<FieldViolation> violations;

    private ApiError(int status, String error, String message, String path,
                     String traceId, List<FieldViolation> violations) {
        this.timestamp = OffsetDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.traceId = traceId;
        this.violations = violations;
    }

    public static ApiError of(int status, String error, String message,
                               String path, String traceId) {
        return new ApiError(status, error, message, path, traceId, null);
    }

    public static ApiError of(int status, String error, String message,
                               String path, String traceId, List<FieldViolation> violations) {
        return new ApiError(status, error, message, path, traceId, violations);
    }

    @Getter
    @AllArgsConstructor
    public static class FieldViolation {
        private final String field;
        private final String message;
    }
}
