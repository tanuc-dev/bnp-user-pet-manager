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
