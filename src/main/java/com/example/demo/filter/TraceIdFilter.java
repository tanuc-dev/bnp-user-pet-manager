package com.example.demo.filter;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * A servlet filter that generates a unique trace ID for each HTTP request and adds it to the logging context (MDC)
 * and response headers. This enables tracing of requests across logs and services.
 * <p>
 * The filter:
 * <ul>
 *   <li>Generates a UUID as the trace ID for each request.</li>
 *   <li>Puts the trace ID into the MDC for logging correlation.</li>
 *   <li>Sets the trace ID as a response header for downstream services or clients.</li>
 *   <li>Logs the start and end of each request, including HTTP method, path, status, and trace ID.</li>
 *   <li>Removes the trace ID from the MDC after the request is processed to avoid leakage between requests.</li>
 * </ul>
 * This filter should be registered as a Spring component and will execute once per request.
 */
@Slf4j
@Component
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String traceId = UUID.randomUUID().toString();
        MDC.put(TRACE_ID, traceId);
        response.setHeader(TRACE_ID, traceId);
        log.info("REQ start method={} path={} traceId={}", request.getMethod(), request.getRequestURI(), MDC.get(TRACE_ID));
        try {
            filterChain.doFilter(request, response);
        } finally {
            log.info("REQ end status={} traceId={}", response.getStatus(), MDC.get(TRACE_ID));
            MDC.remove(TRACE_ID);
        }
    }
}
