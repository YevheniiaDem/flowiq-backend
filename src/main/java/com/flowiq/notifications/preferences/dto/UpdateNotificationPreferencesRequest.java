package com.flowiq.notifications.preferences.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class UpdateNotificationPreferencesRequest {

    @NotEmpty
    @Valid
    private List<NotificationPreferenceItemRequest> preferences;
}
