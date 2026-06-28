package com.flowiq.unit.notifications;

import com.flowiq.audit.service.AuditService;
import com.flowiq.entity.User;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.notifications.entity.NotificationChannel;
import com.flowiq.notifications.preferences.NotificationPreference;
import com.flowiq.notifications.preferences.NotificationPreferenceKey;
import com.flowiq.notifications.preferences.NotificationPreferenceRepository;
import com.flowiq.notifications.preferences.NotificationPreferenceService;
import com.flowiq.notifications.preferences.dto.NotificationPreferenceItemRequest;
import com.flowiq.notifications.preferences.dto.UpdateNotificationPreferencesRequest;
import com.flowiq.repository.UserRepository;
import com.flowiq.unit.support.SecurityTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("NotificationPreferenceService unit tests")
class NotificationPreferenceServiceTest {

    private static final Long USER_ID = 1L;
    private static final String EMAIL = "prefs@test.flowiq";

    @Mock
    private NotificationPreferenceRepository preferenceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private NotificationPreferenceService preferenceService;

    private User user;

    @BeforeEach
    void setUp() {
        user = SecurityTestSupport.testUser(USER_ID, EMAIL);
        SecurityTestSupport.authenticate(user);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(preferenceRepository.findByUser_Id(USER_ID)).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        SecurityTestSupport.clearSecurityContext();
    }

    @Test
    @DisplayName("getPreferences returns all categories with defaults")
    void getPreferences_success() {
        var response = preferenceService.getPreferences();

        assertThat(response.getCategories()).isNotEmpty();
        assertThat(response.getCategories().get(0).getPreferences()).isNotEmpty();
    }

    @Test
    @DisplayName("updatePreferences saves items and audits change")
    void updatePreferences_success() {
        NotificationPreferenceItemRequest item = new NotificationPreferenceItemRequest();
        item.setKey(NotificationPreferenceKey.FINANCIAL_TAXES);
        item.setChannel(NotificationChannel.EMAIL);
        item.setEnabled(false);

        UpdateNotificationPreferencesRequest request = new UpdateNotificationPreferencesRequest();
        request.setPreferences(List.of(item));

        when(preferenceRepository.findByUser_IdAndNotificationTypeAndChannel(
                USER_ID, NotificationPreferenceKey.FINANCIAL_TAXES, NotificationChannel.EMAIL))
                .thenReturn(Optional.empty());
        when(preferenceRepository.save(any(NotificationPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = preferenceService.updatePreferences(request);

        assertThat(response.getCategories()).isNotEmpty();
        verify(preferenceRepository).save(any(NotificationPreference.class));
        verify(auditService).log(any());
    }

    @Test
    @DisplayName("resetToDefaults clears stored preferences")
    void resetToDefaults_success() {
        var response = preferenceService.resetToDefaults();

        assertThat(response.getCategories()).isNotEmpty();
        verify(preferenceRepository).deleteByUser_Id(USER_ID);
        verify(auditService).log(any());
    }

    @Test
    @DisplayName("isEnabled returns stored value when present")
    void isEnabled_storedValue() {
        NotificationPreference preference = new NotificationPreference();
        preference.setEnabled(false);

        when(preferenceRepository.findByUser_IdAndNotificationTypeAndChannel(
                USER_ID, NotificationPreferenceKey.AI_WARNINGS, NotificationChannel.EMAIL))
                .thenReturn(Optional.of(preference));

        assertThat(preferenceService.isEnabled(
                USER_ID, NotificationPreferenceKey.AI_WARNINGS, NotificationChannel.EMAIL))
                .isFalse();
    }

    @Test
    @DisplayName("isEnabled defaults IN_APP to true and EMAIL to false")
    void isEnabled_defaults() {
        when(preferenceRepository.findByUser_IdAndNotificationTypeAndChannel(
                any(), any(), any())).thenReturn(Optional.empty());

        assertThat(preferenceService.isEnabled(
                USER_ID, NotificationPreferenceKey.TASK_REMINDER_TODAY, NotificationChannel.IN_APP))
                .isTrue();
        assertThat(preferenceService.isEnabled(
                USER_ID, NotificationPreferenceKey.TASK_REMINDER_TODAY, NotificationChannel.EMAIL))
                .isFalse();
    }

    @Test
    @DisplayName("isInAppEnabled delegates to isEnabled")
    void isInAppEnabled_success() {
        when(preferenceRepository.findByUser_IdAndNotificationTypeAndChannel(
                USER_ID, NotificationPreferenceKey.REPORT_READY, NotificationChannel.IN_APP))
                .thenReturn(Optional.empty());

        assertThat(preferenceService.isInAppEnabled(USER_ID, NotificationPreferenceKey.REPORT_READY))
                .isTrue();
    }

    @Test
    @DisplayName("rejects unauthenticated access")
    void rejectsUnauthenticated() {
        SecurityTestSupport.clearSecurityContext();

        assertThatThrownBy(() -> preferenceService.getPreferences())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Not authenticated");
    }
}
