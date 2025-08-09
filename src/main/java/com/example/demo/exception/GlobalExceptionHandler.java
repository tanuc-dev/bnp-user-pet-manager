package com.example.demo.exception;

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

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private String currentTraceId() {
        return MDC.get("traceId") != null ? MDC.get("traceId") : "";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidation(MethodArgumentNotValidException ex) {
        log.warn("Validation failed: {} traceId={}", ex.getMessage(), MDC.get("traceId"));
        Map<String, Object> body = new HashMap<>();
        body.put("traceId", currentTraceId());
        body.put("message", "Validation failed");
        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        body.put("errors", errors);
        return body;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleIllegalArgument(IllegalArgumentException ex) {
        log.info("Bad Request: {} traceId={}", ex.getMessage(), MDC.get("traceId"));
        return Map.of("traceId", currentTraceId(),
                      "message", ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNotFound(RuntimeException ex) {
        log.info("Not found: {} traceId={}", ex.getMessage(), MDC.get("traceId"));
        return Map.of("traceId", currentTraceId(),
                      "message", ex.getMessage());
    }

    @ExceptionHandler({
            PessimisticLockException.class,
            LockTimeoutException.class,
            CannotAcquireLockException.class
    })
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handlePessimisticConflicts(Exception ex) {
        log.warn("Concurrency conflict: {} traceId={}", ex.toString(), MDC.get("traceId"));
        return Map.of("traceId", currentTraceId(),
                      "message","Conflict: resource is busy, please retry.");
    }

}
