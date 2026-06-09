package com.flowiq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthResponse {
    private String status;
    private String message;
    private String version;
    private LocalDateTime timestamp;
    private String environment;
}
