package com.flowiq.controller;

import com.flowiq.dto.response.HealthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Value("${spring.application.name:flowiq-backend}")
    private String applicationName;

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

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }
}
