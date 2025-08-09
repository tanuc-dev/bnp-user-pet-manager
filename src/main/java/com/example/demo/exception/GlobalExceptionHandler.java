package com.example.demo.exception;

import static com.example.demo.filter.TraceIdFilter.TRACE_ID;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.MDC;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for REST controllers.
 * <p>
 * Handles and logs various exceptions thrown during request processing,
 * providing consistent error responses with trace IDs for easier debugging.
 * </p>
 * <ul>
 *   <li>{@link MethodArgumentNotValidException}: Handles validation errors, returns BAD_REQUEST with field-specific messages.</li>
 *   <li>{@link IllegalArgumentException}: Handles bad requests, returns BAD_REQUEST with the exception message.</li>
 *   <li>{@link RuntimeException}: Handles not found errors, returns NOT_FOUND with the exception message.</li>
 *   <li>Pessimistic locking exceptions: Handles concurrency conflicts, returns CONFLICT with a retry message.</li>
 * </ul>
 * <p>
 * Each response includes a trace ID for correlation in logs.
 * </p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String MESSAGE = "message";

    private String currentTraceId() {
        return MDC.get(TRACE_ID) != null ? MDC.get(TRACE_ID) : "";
    }

    /**
     * Handles validation errors.
     *
     * @param ex the exception
     * @return a map containing the error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidation(MethodArgumentNotValidException ex) {
        log.warn("Validation failed: {} traceId={}", ex.getMessage(), MDC.get(TRACE_ID));
        Map<String, Object> body = new HashMap<>();
        body.put(TRACE_ID, currentTraceId());
        body.put(MESSAGE, "Validation failed");
        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        body.put("errors", errors);
        return body;
    }

    /**
     * Handles illegal argument exceptions.
     *
     * @param ex the exception
     * @return a map containing the error details
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleIllegalArgument(IllegalArgumentException ex) {
        log.info("Bad Request: {} traceId={}", ex.getMessage(), MDC.get(TRACE_ID));
        return Map.of(TRACE_ID, currentTraceId(),
                      MESSAGE, ex.getMessage());
    }

    /**
     * Handles runtime exceptions.
     *
     * @param ex the exception
     * @return a map containing the error details
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNotFound(RuntimeException ex) {
        log.info("Not found: {} traceId={}", ex.getMessage(), MDC.get(TRACE_ID));
        return Map.of(TRACE_ID, currentTraceId(),
                      MESSAGE, ex.getMessage());
    }

    /**
     * Handles pessimistic locking exceptions.
     *
     * @param ex the exception
     * @return a map containing the error details
     */
    @ExceptionHandler({
            PessimisticLockException.class,
            LockTimeoutException.class,
            CannotAcquireLockException.class
    })
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handlePessimisticConflicts(Exception ex) {
        log.warn("Concurrency conflict: {} traceId={}", ex.toString(), MDC.get(TRACE_ID));
        return Map.of(TRACE_ID, currentTraceId(),
                      MESSAGE,"Conflict: resource is busy, please retry.");
    }

}
