package com.flowiq.unit.support;

import com.flowiq.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Shared setup for standalone {@link MockMvc} controller tests with validation and exception handling.
 */
public final class ControllerTestSupport {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private ControllerTestSupport() {
    }

    public static MockMvc standaloneMockMvc(Object controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }
}
