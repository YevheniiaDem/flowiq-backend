package com.flowiq.unit.exception;

import com.flowiq.dto.request.RegisterRequest;
import com.flowiq.exception.BadRequestException;
import com.flowiq.exception.GlobalExceptionHandler;
import com.flowiq.exception.ResourceNotFoundException;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.unit.support.ControllerTestSupport;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GlobalExceptionHandler tests")
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new ExceptionTestController())
                .setControllerAdvice(handler)
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("validation errors return 400 with field messages")
    void handleValidation_returnsBadRequestWithFieldErrors() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("not-an-email");
        request.setPassword("short");
        request.setName("");

        mockMvc.perform(post("/test-exceptions/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestSupport.OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    @DisplayName("BadRequestException returns 400 with message")
    void handleBadRequest_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/test-exceptions/bad-request"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid input"));
    }

    @Test
    @DisplayName("UnauthorizedException returns 401 with message")
    void handleUnauthorized_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/test-exceptions/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @DisplayName("BadCredentialsException returns 401 with generic message")
    void handleBadCredentials_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/test-exceptions/bad-credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("ResourceNotFoundException returns 404 with message")
    void handleNotFound_returnsNotFound() throws Exception {
        mockMvc.perform(get("/test-exceptions/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Resource missing"));
    }

    @Test
    @DisplayName("generic Exception returns 500 with safe message")
    void handleGeneral_returnsInternalServerError() throws Exception {
        mockMvc.perform(get("/test-exceptions/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @RestController
    @RequestMapping("/test-exceptions")
    static class ExceptionTestController {

        @PostMapping("/validation")
        void validation(@Valid @RequestBody RegisterRequest request) {
        }

        @GetMapping("/bad-request")
        void badRequest() {
            throw new BadRequestException("Invalid input");
        }

        @GetMapping("/unauthorized")
        void unauthorized() {
            throw new UnauthorizedException("Access denied");
        }

        @GetMapping("/bad-credentials")
        void badCredentials() {
            throw new BadCredentialsException("Bad credentials");
        }

        @GetMapping("/not-found")
        void notFound() {
            throw new ResourceNotFoundException("Resource missing");
        }

        @GetMapping("/generic")
        void generic() {
            throw new RuntimeException("boom");
        }
    }
}
