package com.example.demo.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @Test
    void setsHeader_putsMdc_thenClearsAfterSuccess() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/ping");
        MockHttpServletResponse res = new MockHttpServletResponse();

        AtomicReference<String> seenInChain = new AtomicReference<>();
        FilterChain chain = (request, response) -> {
            seenInChain.set(MDC.get(TraceIdFilter.TRACE_ID));
            assertThat(seenInChain.get()).isNotBlank();
        };

        filter.doFilter(req, res, chain);

        String headerTraceId = res.getHeader(TraceIdFilter.TRACE_ID);
        assertThat(headerTraceId).isNotBlank();
        assertThat(seenInChain.get()).isEqualTo(headerTraceId);

        assertThat(MDC.get(TraceIdFilter.TRACE_ID)).isNull();
    }

    @Test
    void clearsMdc_evenWhenChainThrows_andHeaderIsStillSet() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/error");
        MockHttpServletResponse res = new MockHttpServletResponse();

        FilterChain chain = (request, response) -> {
            assertThat(MDC.get(TraceIdFilter.TRACE_ID)).isNotBlank();
            throw new ServletException("boom");
        };

        assertThatThrownBy(() -> filter.doFilter(req, res, chain))
                .isInstanceOf(ServletException.class)
                .hasMessageContaining("boom");

        assertThat(res.getHeader(TraceIdFilter.TRACE_ID)).isNotBlank();

        assertThat(MDC.get(TraceIdFilter.TRACE_ID)).isNull();
    }

    @Test
    void generatesDifferentTraceId_perRequest() throws IOException, ServletException {
        MockHttpServletRequest req1 = new MockHttpServletRequest("GET", "/r1");
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        MockHttpServletRequest req2 = new MockHttpServletRequest("GET", "/r2");
        MockHttpServletResponse res2 = new MockHttpServletResponse();

        filter.doFilter(req1, res1, (r, s) -> {});
        filter.doFilter(req2, res2, (r, s) -> {});

        String t1 = res1.getHeader(TraceIdFilter.TRACE_ID);
        String t2 = res2.getHeader(TraceIdFilter.TRACE_ID);

        assertThat(t1).isNotBlank();
        assertThat(t2).isNotBlank();
        assertThat(t1).isNotEqualTo(t2);
    }
}
