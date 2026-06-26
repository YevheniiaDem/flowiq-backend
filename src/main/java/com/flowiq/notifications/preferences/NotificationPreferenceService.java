package com.flowiq.notifications.preferences;

import com.flowiq.audit.AuditEventType;
import com.flowiq.audit.AuditOutcome;
import com.flowiq.audit.ResourceType;
import com.flowiq.audit.dto.AuditEventRequest;
import com.flowiq.audit.service.AuditService;
import com.flowiq.entity.User;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.notifications.entity.NotificationChannel;
import com.flowiq.notifications.preferences.dto.*;
import com.flowiq.repository.UserRepository;
import com.flowiq.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private static final List<NotificationChannel> ALL_CHANNELS = List.of(NotificationChannel.values());

    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public NotificationPreferencesResponse getPreferences() {
        User user = getCurrentUserEntity();
        return buildResponse(user.getId());
    }

    @Transactional
    public NotificationPreferencesResponse updatePreferences(UpdateNotificationPreferencesRequest request) {
        User user = getCurrentUserEntity();
        Long userId = user.getId();

        for (NotificationPreferenceItemRequest item : request.getPreferences()) {
            NotificationPreference preference = preferenceRepository
                    .findByUser_IdAndNotificationTypeAndChannel(userId, item.getKey(), item.getChannel())
                    .orElseGet(() -> {
                        NotificationPreference created = new NotificationPreference();
                        created.setUser(user);
                        created.setNotificationType(item.getKey());
                        created.setChannel(item.getChannel());
                        return created;
                    });
            preference.setEnabled(item.getEnabled());
            preferenceRepository.save(preference);
        }

        auditService.log(AuditEventRequest.builder()
                .actorUserId(userId)
                .actorEmail(user.getEmail())
                .actorRole(user.getRole().name())
                .eventType(AuditEventType.NOTIFICATION_SETTINGS_UPDATED)
                .outcome(AuditOutcome.SUCCESS)
                .resourceType(ResourceType.USER)
                .resourceId(userId)
                .metadata(Map.of("itemsUpdated", request.getPreferences().size()))
                .build());

        return buildResponse(userId);
    }

    @Transactional
    public NotificationPreferencesResponse resetToDefaults() {
        User user = getCurrentUserEntity();
        preferenceRepository.deleteByUser_Id(user.getId());

        auditService.log(AuditEventRequest.builder()
                .actorUserId(user.getId())
                .actorEmail(user.getEmail())
                .actorRole(user.getRole().name())
                .eventType(AuditEventType.NOTIFICATION_SETTINGS_UPDATED)
                .outcome(AuditOutcome.SUCCESS)
                .resourceType(ResourceType.USER)
                .resourceId(user.getId())
                .metadata(Map.of("action", "reset_to_defaults"))
                .build());

        return buildResponse(user.getId());
    }

    @Transactional(readOnly = true)
    public boolean isEnabled(Long userId, NotificationPreferenceKey key, NotificationChannel channel) {
        return preferenceRepository
                .findByUser_IdAndNotificationTypeAndChannel(userId, key, channel)
                .map(NotificationPreference::isEnabled)
                .orElseGet(() -> defaultEnabled(channel));
    }

    @Transactional(readOnly = true)
    public boolean isInAppEnabled(Long userId, NotificationPreferenceKey key) {
        return isEnabled(userId, key, NotificationChannel.IN_APP);
    }

    private NotificationPreferencesResponse buildResponse(Long userId) {
        List<NotificationPreference> stored = preferenceRepository.findByUser_Id(userId);
        Map<String, Boolean> lookup = stored.stream()
                .collect(Collectors.toMap(
                        p -> p.getNotificationType().name() + ":" + p.getChannel().name(),
                        NotificationPreference::isEnabled,
                        (a, b) -> b
                ));

        List<NotificationPreferenceCategoryResponse> categories = Arrays.stream(
                NotificationPreferenceKey.PreferenceCategory.values()
        ).map(category -> NotificationPreferenceCategoryResponse.builder()
                .id(category)
                .preferences(Arrays.stream(NotificationPreferenceKey.values())
                        .filter(key -> key.getCategory() == category)
                        .map(key -> NotificationPreferenceItemResponse.builder()
                                .key(key)
                                .channels(channelMap(key, lookup))
                                .build())
                        .toList())
                .build()).toList();

        return NotificationPreferencesResponse.of(categories);
    }

    private Map<NotificationChannel, Boolean> channelMap(
            NotificationPreferenceKey key,
            Map<String, Boolean> lookup
    ) {
        Map<NotificationChannel, Boolean> channels = new EnumMap<>(NotificationChannel.class);
        for (NotificationChannel channel : ALL_CHANNELS) {
            String mapKey = key.name() + ":" + channel.name();
            channels.put(channel, lookup.getOrDefault(mapKey, defaultEnabled(channel)));
        }
        return channels;
    }

    private boolean defaultEnabled(NotificationChannel channel) {
        return channel == NotificationChannel.IN_APP;
    }

    private User getCurrentUserEntity() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("Not authenticated");
        }
        return userRepository.findByEmail(principal.getEmail())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }
}
