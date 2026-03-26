package com.rjs.fsm.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.UUID;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.rjs.fsm")
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                      HttpServletRequest req) {
        List<ApiError.FieldViolation> violations = ex.getBindingResult().getAllErrors().stream()
                .map(err -> {
                    if (err instanceof FieldError fe) {
                        return new ApiError.FieldViolation(fe.getField(), fe.getDefaultMessage());
                    }
                    return new ApiError.FieldViolation(err.getObjectName(), err.getDefaultMessage());
                }).toList();

        return respond(HttpStatus.BAD_REQUEST, "Validation failed", req, violations);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex,
                                                               HttpServletRequest req) {
        List<ApiError.FieldViolation> violations = ex.getConstraintViolations().stream()
                .map(v -> new ApiError.FieldViolation(v.getPropertyPath().toString(), v.getMessage()))
                .toList();

        return respond(HttpStatus.BAD_REQUEST, "Validation failed", req, violations);
    }

    @ExceptionHandler(InvalidFormatException.class)
    public ResponseEntity<ApiError> handleInvalidFormat(InvalidFormatException ex, HttpServletRequest req) {
        return respond(HttpStatus.BAD_REQUEST, "Invalid value (check enum / field type)", req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return respond(HttpStatus.BAD_REQUEST, "Malformed JSON or invalid value", req);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, HttpServletRequest req) {
        return respond(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
    public ResponseEntity<ApiError> handleForbidden(Exception ex, HttpServletRequest req) {
        return respond(HttpStatus.FORBIDDEN, ex.getMessage() != null ? ex.getMessage() : "Forbidden", req);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex, HttpServletRequest req) {
        return respond(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return respond(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        String traceId = UUID.randomUUID().toString();
        log.error("Unhandled exception [traceId={}]", traceId, ex);
        ApiError body = ApiError.of(500, "INTERNAL_SERVER_ERROR",
                "Unexpected error (traceId=" + traceId + ")", req.getRequestURI(), traceId);
        return ResponseEntity.status(500).body(body);
    }

    private ResponseEntity<ApiError> respond(HttpStatus status, String message, HttpServletRequest req) {
        return respond(status, message, req, null);
    }

    private ResponseEntity<ApiError> respond(HttpStatus status, String message,
                                              HttpServletRequest req, List<ApiError.FieldViolation> violations) {
        String traceId = UUID.randomUUID().toString();
        ApiError body = ApiError.of(status.value(), status.name(), message,
                req.getRequestURI(), traceId, violations);
        return ResponseEntity.status(status).body(body);
    }
}
