package com.flowiq.notifications.preferences.dto;

import com.flowiq.notifications.entity.NotificationChannel;
import com.flowiq.notifications.preferences.NotificationPreferenceKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceCategoryResponse {

    private NotificationPreferenceKey.PreferenceCategory id;
    private List<NotificationPreferenceItemResponse> preferences;
}
