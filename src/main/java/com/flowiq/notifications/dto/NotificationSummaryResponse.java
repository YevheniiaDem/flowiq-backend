package com.flowiq.notifications.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Aggregated notification statistics")
public class NotificationSummaryResponse {

    @Schema(description = "Total notifications", example = "25")
    private long total;

    @Schema(description = "Unread notifications", example = "5")
    private long unread;

    @Schema(description = "Critical severity count", example = "1")
    private long critical;

    @Schema(description = "Warning severity count", example = "3")
    private long warnings;

    @Schema(description = "Success severity count", example = "8")
    private long success;

    @Schema(description = "Notifications created this month", example = "12")
    private long thisMonth;
}
