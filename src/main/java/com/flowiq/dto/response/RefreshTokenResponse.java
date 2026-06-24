package com.flowiq.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "New JWT token pair after refresh")
public class RefreshTokenResponse {

    @Schema(description = "New JWT access token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;

    @Schema(description = "New JWT refresh token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;
}
