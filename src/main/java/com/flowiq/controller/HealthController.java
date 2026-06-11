package com.flowiq.controller;

import com.flowiq.config.openapi.ApiErrorResponses;
import com.flowiq.dto.response.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Tag(name = "Health", description = "Public health check endpoints")
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Value("${spring.application.name:flowiq-backend}")
    private String applicationName;

    @Operation(
            summary = "Application health check",
            description = "Returns service status, version, and environment. No authentication required.",
            security = {}
    )
    @SecurityRequirements
    @ApiResponse(
            responseCode = "200",
            description = "Service is healthy",
            content = @Content(schema = @Schema(implementation = HealthResponse.class))
    )
    @ApiErrorResponses
    @GetMapping
    public ResponseEntity<HealthResponse> health() {
        HealthResponse response = new HealthResponse(
                "UP",
                "Flowiq Backend is running successfully",
                "0.0.1-SNAPSHOT",
                LocalDateTime.now(),
                "development"
        );
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Ping endpoint",
            description = "Simple liveness probe that returns 'pong'. No authentication required.",
            security = {}
    )
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "Pong response", content = @Content(schema = @Schema(type = "string", example = "pong")))
    @ApiErrorResponses
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }
}
