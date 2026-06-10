package com.flowiq.notifications.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationSummaryResponse {

    private long total;
    private long unread;
    private long critical;
    private long warnings;
    private long success;
    private long thisMonth;
}
