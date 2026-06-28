package com.flowiq.notifications.preferences;

import com.flowiq.exception.UnauthorizedException;
import com.flowiq.notifications.entity.NotificationChannel;
import com.flowiq.notifications.preferences.dto.NotificationPreferenceItemRequest;
import com.flowiq.notifications.preferences.dto.NotificationPreferencesResponse;
import com.flowiq.notifications.preferences.dto.UpdateNotificationPreferencesRequest;
import com.flowiq.unit.support.ControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationPreferenceController tests")
class NotificationPreferenceControllerTest {

    @Mock
    private NotificationPreferenceService preferenceService;

    @InjectMocks
    private NotificationPreferenceController notificationPreferenceController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.standaloneMockMvc(notificationPreferenceController);
    }

    @Test
    @DisplayName("GET /api/settings/notifications returns preferences")
    void getPreferences_success() throws Exception {
        when(preferenceService.getPreferences()).thenReturn(
                NotificationPreferencesResponse.builder()
                        .categories(List.of())
                        .channels(List.of(NotificationChannel.IN_APP))
                        .build());

        mockMvc.perform(get("/api/settings/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channels[0]").value("IN_APP"));
    }

    @Test
    @DisplayName("PUT /api/settings/notifications updates preferences")
    void updatePreferences_success() throws Exception {
        UpdateNotificationPreferencesRequest request = validUpdateRequest();

        when(preferenceService.updatePreferences(any(UpdateNotificationPreferencesRequest.class)))
                .thenReturn(NotificationPreferencesResponse.builder().categories(List.of()).build());

        mockMvc.perform(put("/api/settings/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestSupport.OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/settings/notifications rejects empty preferences list")
    void updatePreferences_validationError() throws Exception {
        UpdateNotificationPreferencesRequest request = new UpdateNotificationPreferencesRequest();
        request.setPreferences(Collections.emptyList());

        mockMvc.perform(put("/api/settings/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestSupport.OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/settings/notifications/reset resets to defaults")
    void resetToDefaults_success() throws Exception {
        when(preferenceService.resetToDefaults()).thenReturn(
                NotificationPreferencesResponse.builder().categories(List.of()).build());

        mockMvc.perform(post("/api/settings/notifications/reset"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/settings/notifications returns 401 when unauthorized")
    void getPreferences_unauthorized() throws Exception {
        doThrow(new UnauthorizedException("Not authenticated")).when(preferenceService).getPreferences();

        mockMvc.perform(get("/api/settings/notifications"))
                .andExpect(status().isUnauthorized());
    }

    private UpdateNotificationPreferencesRequest validUpdateRequest() {
        NotificationPreferenceItemRequest item = new NotificationPreferenceItemRequest();
        item.setKey(NotificationPreferenceKey.FINANCIAL_TAXES);
        item.setChannel(NotificationChannel.IN_APP);
        item.setEnabled(true);

        UpdateNotificationPreferencesRequest request = new UpdateNotificationPreferencesRequest();
        request.setPreferences(List.of(item));
        return request;
    }
}
