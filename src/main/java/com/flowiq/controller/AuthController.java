package com.flowiq.controller;

import com.flowiq.audit.AuditEventType;
import com.flowiq.audit.ResourceType;
import com.flowiq.audit.aspect.Auditable;
import com.flowiq.config.OpenApiConfig;
import com.flowiq.config.openapi.ApiErrorResponses;
import com.flowiq.dto.request.LoginRequest;
import com.flowiq.dto.request.RefreshTokenRequest;
import com.flowiq.dto.request.RegisterRequest;
import com.flowiq.dto.response.AuthResponse;
import com.flowiq.dto.response.RefreshTokenResponse;
import com.flowiq.dto.response.UserResponse;
import com.flowiq.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "User registration, login, and session management")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account and returns JWT access and refresh tokens. No authentication required.",
            security = {}
    )
    @SecurityRequirements
    @ApiResponse(responseCode = "201", description = "User registered successfully",
            content = @Content(schema = @Schema(implementation = AuthResponse.class)))
    @ApiErrorResponses
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(
            summary = "Login",
            description = "Authenticates user credentials and returns JWT access and refresh tokens. No authentication required.",
            security = {}
    )
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "Login successful",
            content = @Content(schema = @Schema(implementation = AuthResponse.class)))
    @ApiErrorResponses
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(
            summary = "Refresh JWT tokens",
            description = "Exchanges a valid refresh token for a new access and refresh token pair. No authentication required.",
            security = {}
    )
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "New token pair issued",
            content = @Content(schema = @Schema(implementation = RefreshTokenResponse.class)))
    @ApiErrorResponses
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @Operation(
            summary = "Get current user",
            description = "Returns the profile of the currently authenticated user. Requires a valid JWT Bearer token."
    )
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponse(responseCode = "200", description = "Current user profile",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    @ApiErrorResponses
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        return ResponseEntity.ok(authService.getCurrentUser());
    }

    @Operation(
            summary = "Logout",
            description = "Invalidates the current session on the client side. Requires a valid JWT Bearer token."
    )
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponse(responseCode = "204", description = "Logout successful")
    @ApiErrorResponses
    @Auditable(value = AuditEventType.AUTH_LOGOUT, resourceType = ResourceType.SESSION)
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
