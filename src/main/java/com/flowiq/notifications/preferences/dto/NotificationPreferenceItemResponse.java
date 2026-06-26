package com.flowiq.notifications.preferences.dto;

import com.flowiq.notifications.entity.NotificationChannel;
import com.flowiq.notifications.preferences.NotificationPreferenceKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceItemResponse {

    private NotificationPreferenceKey key;
    private Map<NotificationChannel, Boolean> channels;
}
