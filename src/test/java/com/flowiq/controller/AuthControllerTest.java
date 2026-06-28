package com.flowiq.controller;

import com.flowiq.dto.request.LoginRequest;
import com.flowiq.dto.request.RefreshTokenRequest;
import com.flowiq.dto.request.RegisterRequest;
import com.flowiq.dto.response.AuthResponse;
import com.flowiq.dto.response.RefreshTokenResponse;
import com.flowiq.dto.response.UserResponse;
import com.flowiq.exception.GlobalExceptionHandler;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController tests")
class AuthControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/auth/register returns 201 on success")
    void register_success() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@test.flowiq");
        request.setPassword("Password1!");
        request.setName("New User");

        when(authService.register(any(RegisterRequest.class), any(HttpServletRequest.class)))
                .thenReturn(AuthResponse.builder()
                        .token("access")
                        .refreshToken("refresh")
                        .user(UserResponse.builder().email("new@test.flowiq").build())
                        .build());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("access"));
    }

    @Test
    @DisplayName("POST /api/auth/register rejects invalid email")
    void register_validationError() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("not-an-email");
        request.setPassword("Password1!");
        request.setName("New User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @DisplayName("POST /api/auth/login returns token pair")
    void login_success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.flowiq");
        request.setPassword("Password1!");

        when(authService.login(any(LoginRequest.class), any(HttpServletRequest.class)))
                .thenReturn(AuthResponse.builder()
                        .token("access")
                        .refreshToken("refresh")
                        .user(UserResponse.builder().email("user@test.flowiq").build())
                        .build());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("access"));
    }

    @Test
    @DisplayName("POST /api/auth/login rejects blank password")
    void login_validationError() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.flowiq");
        request.setPassword("");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/auth/me returns current user")
    void me_success() throws Exception {
        when(authService.getCurrentUser())
                .thenReturn(UserResponse.builder().email("user@test.flowiq").name("Test").build());

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@test.flowiq"));
    }

    @Test
    @DisplayName("GET /api/auth/me returns 401 when not authenticated")
    void me_unauthorized() throws Exception {
        when(authService.getCurrentUser()).thenThrow(new UnauthorizedException("Not authenticated"));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/logout returns 204")
    void logout_success() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent());

        verify(authService).logout(any(HttpServletRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/refresh rejects blank refresh token")
    void refresh_validationError() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/refresh returns new tokens")
    void refresh_success() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh.jwt");

        when(authService.refresh(any(RefreshTokenRequest.class)))
                .thenReturn(RefreshTokenResponse.builder()
                        .token("new.access")
                        .refreshToken("new.refresh")
                        .build());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").value("new.refresh"));
    }
}
