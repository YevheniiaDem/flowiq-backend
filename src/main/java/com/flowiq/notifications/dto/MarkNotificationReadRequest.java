package com.flowiq.notifications.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Optional body when marking a notification as read")
public class MarkNotificationReadRequest {

    @Schema(description = "Read state (always true for this endpoint)", example = "true", defaultValue = "true")
    private Boolean read = true;
}
