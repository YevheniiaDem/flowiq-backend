package com.flowiq.notifications.preferences.dto;

import com.flowiq.notifications.entity.NotificationChannel;
import com.flowiq.notifications.preferences.NotificationPreferenceKey;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NotificationPreferenceItemRequest {

    @NotNull
    private NotificationPreferenceKey key;

    @NotNull
    private NotificationChannel channel;

    @NotNull
    private Boolean enabled;
}
