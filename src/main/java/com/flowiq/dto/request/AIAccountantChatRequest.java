package com.flowiq.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Message to send to the AI Accountant")
public class AIAccountantChatRequest {

    @NotBlank(message = "Message is required")
    @Size(max = 2000, message = "Message must not exceed 2000 characters")
    @Schema(description = "User message to the AI Accountant", example = "Які мої основні витрати цього місяця?", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;
}
