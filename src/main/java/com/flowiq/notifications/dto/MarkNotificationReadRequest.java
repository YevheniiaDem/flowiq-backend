package com.flowiq.notifications.dto;

import lombok.Data;

@Data
public class MarkNotificationReadRequest {
    private Boolean read = true;
}
