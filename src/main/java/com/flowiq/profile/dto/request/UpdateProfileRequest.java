package com.flowiq.profile.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Update personal profile fields")
public class UpdateProfileRequest {

    @NotBlank
    @Size(max = 100)
    @Schema(example = "Іван")
    private String firstName;

    @Size(max = 100)
    @Schema(example = "Петренко")
    private String lastName;

    @Email
    @Size(max = 100)
    @Schema(description = "Email change requires verification (not yet implemented)", example = "user@example.com")
    private String email;

    @Size(max = 30)
    @Schema(example = "+380501234567")
    private String phone;

    @Size(max = 100)
    @Schema(example = "ФОП Петренко")
    private String company;
}
