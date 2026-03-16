package com.rjs.fsm.api;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.rjs.fsm.api.ApiError.FieldViolation;
import com.rjs.fsm.api.exception.BadRequestException;
import com.rjs.fsm.api.exception.ForbiddenException;
import com.rjs.fsm.api.exception.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
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
// basePackages ini bikin advice tetap kepake walau struktur package lu agak "geser"
@RestControllerAdvice(basePackages = "com.rjs.fsm")
public class GlobalExceptionHandler {

    // === 400: DTO validation ===
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest req
    ) {
        String traceId = UUID.randomUUID().toString();

        List<FieldViolation> violations = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(err -> {
                    if (err instanceof FieldError fe) {
                        return new FieldViolation(fe.getField(), fe.getDefaultMessage());
                    }
                    return new FieldViolation(err.getObjectName(), err.getDefaultMessage());
                })
                .toList();

        ApiError body = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "BAD_REQUEST",
                "Validation failed",
                req.getRequestURI(),
                traceId,
                violations
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // === 400: query/path constraint violation ===
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest req
    ) {
        String traceId = UUID.randomUUID().toString();

        List<FieldViolation> violations = ex.getConstraintViolations()
                .stream()
                .map(v -> new FieldViolation(v.getPropertyPath().toString(), v.getMessage()))
                .toList();

        ApiError body = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "BAD_REQUEST",
                "Validation failed",
                req.getRequestURI(),
                traceId,
                violations
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // === 400: invalid enum format / invalid JSON value (direct InvalidFormatException) ===
    @ExceptionHandler(InvalidFormatException.class)
    public ResponseEntity<ApiError> handleInvalidFormat(
            InvalidFormatException ex,
            HttpServletRequest req
    ) {
        String traceId = UUID.randomUUID().toString();

        ApiError body = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "BAD_REQUEST",
                "Invalid value (check enum / field type)",
                req.getRequestURI(),
                traceId
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // === 400: malformed JSON / invalid enum value (wrapped) ===
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest req
    ) {
        String traceId = UUID.randomUUID().toString();

        ApiError body = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "BAD_REQUEST",
                "Malformed JSON or invalid value",
                req.getRequestURI(),
                traceId
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // === 404 ===
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            NotFoundException ex,
            HttpServletRequest req
    ) {
        String traceId = UUID.randomUUID().toString();

        ApiError body = ApiError.of(
                HttpStatus.NOT_FOUND.value(),
                "NOT_FOUND",
                ex.getMessage(),
                req.getRequestURI(),
                traceId
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // === 403 ===
    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
    public ResponseEntity<ApiError> handleForbidden(
            Exception ex,
            HttpServletRequest req
    ) {
        String traceId = UUID.randomUUID().toString();

        ApiError body = ApiError.of(
                HttpStatus.FORBIDDEN.value(),
                "FORBIDDEN",
                ex.getMessage() == null ? "Forbidden" : ex.getMessage(),
                req.getRequestURI(),
                traceId
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    // === 400 (fallback for IllegalArgumentException) ===
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest req
    ) {
        String traceId = UUID.randomUUID().toString();

        ApiError body = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "BAD_REQUEST",
                ex.getMessage(),
                req.getRequestURI(),
                traceId
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

        // === 400: custom bad request ===
@ExceptionHandler(BadRequestException.class)
public org.springframework.http.ResponseEntity<ApiError> handleBadRequest(
        BadRequestException ex,
        HttpServletRequest req
    ) {
    String traceId = java.util.UUID.randomUUID().toString();

    ApiError body = ApiError.of(
            org.springframework.http.HttpStatus.BAD_REQUEST.value(),
            "BAD_REQUEST",
            ex.getMessage(),
            req.getRequestURI(),
            traceId
    );

    return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST).body(body);
    }
    // === 500 ===
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(
            Exception ex,
            HttpServletRequest req
    ) {
        String traceId = UUID.randomUUID().toString();

        ApiError body = ApiError.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                "Unexpected error (traceId=" + traceId + ")",
                req.getRequestURI(),
                traceId
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}