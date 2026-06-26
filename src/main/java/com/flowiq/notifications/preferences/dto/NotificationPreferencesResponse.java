package com.flowiq.notifications.preferences.dto;

import com.flowiq.notifications.entity.NotificationChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferencesResponse {

    private List<NotificationPreferenceCategoryResponse> categories;
    private List<NotificationChannel> channels;

    public static NotificationPreferencesResponse of(List<NotificationPreferenceCategoryResponse> categories) {
        return NotificationPreferencesResponse.builder()
                .categories(categories)
                .channels(Arrays.asList(NotificationChannel.values()))
                .build();
    }
}
