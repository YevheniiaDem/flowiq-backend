package com.flowiq.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Application health status")
public class HealthResponse {
    private String status;
    private String message;
    private String version;
    private LocalDateTime timestamp;
    private String environment;
}
