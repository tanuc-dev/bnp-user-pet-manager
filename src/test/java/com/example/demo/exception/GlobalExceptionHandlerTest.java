package com.example.demo.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.PessimisticLockException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    private static final String TEST_TRACE_ID = "TEST-TRACE-ID-123";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;

    // ---------- Test endpoints to trigger each scenario ----------
    @RestController
    @TestConfiguration
    static class TestController {
        @PostMapping("/test/validate")
        public String validate(@Valid @RequestBody CreateReq req) {
            return "ok";
        }

        @GetMapping("/test/illegal")
        public String illegal() {
            throw new IllegalArgumentException("bad input here");
        }

        @GetMapping("/test/notfound")
        public String notfound() {
            throw new RuntimeException("User not found: 42");
        }

        @GetMapping("/test/conflict")
        public String conflict() {
            throw new PessimisticLockException("busy");
        }
    }

    @TestConfiguration
    static class MdcTestConfig {
        @Bean
        OncePerRequestFilter testTraceIdFilter() {
            return new OncePerRequestFilter() {
                @Override
                protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                        throws IOException, jakarta.servlet.ServletException {
                    MDC.put("traceId", TEST_TRACE_ID);
                    try {
                        chain.doFilter(request, response);
                    } finally {
                        MDC.remove("traceId");
                    }
                }
            };
        }
    }

    static class CreateReq {
        @NotBlank(message = "must not be blank")
        public String name;

        @Min(value = 1, message = "must be at least 1")
        public Integer age;
    }

    // --------------------------- Tests ---------------------------

    @Test
    void handleValidation_returns400_andFieldErrors() throws Exception {
        // Missing name (blank) and age < 1 -> both should be in "errors"
        var bad = new CreateReq();
        bad.name = " "; // blank triggers @NotBlank
        bad.age = 0; // < 1 triggers @Min

        mvc.perform(post("/test/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.traceId").value(TEST_TRACE_ID))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").value("must not be blank"))
                .andExpect(jsonPath("$.errors.age").value("must be at least 1"));
    }

    @Test
    void handleIllegalArgument_returns400_withMessage() throws Exception {
        mvc.perform(get("/test/illegal"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.traceId").value(TEST_TRACE_ID))
                .andExpect(jsonPath("$.message").value("bad input here"));
    }

    @Test
    void handleNotFound_returns404_withMessage() throws Exception {
        mvc.perform(get("/test/notfound"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.traceId").value(TEST_TRACE_ID))
                .andExpect(jsonPath("$.message").value("User not found: 42"));
    }

    @Test
    void handlePessimisticConflicts_returns409_withGenericBusyMessage() throws Exception {
        mvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.traceId").value(TEST_TRACE_ID))
                .andExpect(jsonPath("$.message").value("Conflict: resource is busy, please retry."));
    }

    @Test
    void currentTraceId_returnsTraceId_whenPresent() throws Exception {
        MDC.put("traceId", "12345");
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        Method m = GlobalExceptionHandler.class.getDeclaredMethod("currentTraceId");
        m.setAccessible(true);
        String result = (String) m.invoke(handler);

        assertThat(result).isEqualTo("12345");
        MDC.clear();

        result = (String) m.invoke(handler);
        assertThat(result).isEqualTo("");
    }
}
