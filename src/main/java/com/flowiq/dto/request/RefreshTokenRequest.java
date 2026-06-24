package com.flowiq.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Refresh token request")
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required")
    @Schema(description = "JWT refresh token", requiredMode = Schema.RequiredMode.REQUIRED)
    private String refreshToken;
}
