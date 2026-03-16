package com.rjs.fsm.api;

import java.time.OffsetDateTime;
import java.util.List;

public class ApiError {

    private OffsetDateTime timestamp;
    private int status;
    private String error;     // e.g. BAD_REQUEST, NOT_FOUND
    private String message;   // human readable
    private String path;      // request URI
    private String traceId;   // unique id per error
    private List<FieldViolation> violations; // optional

    public ApiError() {}

    public ApiError(
            OffsetDateTime timestamp,
            int status,
            String error,
            String message,
            String path,
            String traceId,
            List<FieldViolation> violations
    ) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.traceId = traceId;
        this.violations = violations;
    }

    public static ApiError of(
            int status,
            String error,
            String message,
            String path,
            String traceId
    ) {
        return new ApiError(OffsetDateTime.now(), status, error, message, path, traceId, null);
    }

    public static ApiError of(
            int status,
            String error,
            String message,
            String path,
            String traceId,
            List<FieldViolation> violations
    ) {
        return new ApiError(OffsetDateTime.now(), status, error, message, path, traceId, violations);
    }

    public OffsetDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public List<FieldViolation> getViolations() { return violations; }
    public void setViolations(List<FieldViolation> violations) { this.violations = violations; }

    public static class FieldViolation {
        private String field;
        private String message;

        public FieldViolation() {}

        public FieldViolation(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() { return field; }
        public void setField(String field) { this.field = field; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}