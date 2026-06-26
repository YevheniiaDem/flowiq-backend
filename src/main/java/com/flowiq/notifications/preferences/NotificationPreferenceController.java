package com.flowiq.notifications.preferences;

import com.flowiq.audit.AuditEventType;
import com.flowiq.audit.ResourceType;
import com.flowiq.audit.aspect.Auditable;
import com.flowiq.config.OpenApiConfig;
import com.flowiq.config.openapi.ApiErrorResponses;
import com.flowiq.notifications.preferences.dto.NotificationPreferencesResponse;
import com.flowiq.notifications.preferences.dto.UpdateNotificationPreferencesRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notification Settings", description = "User notification preferences by category and channel")
@RestController
@RequestMapping("/api/settings/notifications")
@RequiredArgsConstructor
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;

    @Operation(summary = "Get notification preferences")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = NotificationPreferencesResponse.class)))
    @ApiErrorResponses
    @GetMapping
    public ResponseEntity<NotificationPreferencesResponse> getPreferences() {
        return ResponseEntity.ok(preferenceService.getPreferences());
    }

    @Operation(summary = "Update notification preferences")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = NotificationPreferencesResponse.class)))
    @ApiErrorResponses
    @Auditable(value = AuditEventType.NOTIFICATION_SETTINGS_UPDATED, resourceType = ResourceType.USER)
    @PutMapping
    public ResponseEntity<NotificationPreferencesResponse> updatePreferences(
            @Valid @RequestBody UpdateNotificationPreferencesRequest request
    ) {
        return ResponseEntity.ok(preferenceService.updatePreferences(request));
    }

    @Operation(summary = "Reset notification preferences to defaults")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = NotificationPreferencesResponse.class)))
    @ApiErrorResponses
    @PostMapping("/reset")
    public ResponseEntity<NotificationPreferencesResponse> resetToDefaults() {
        return ResponseEntity.ok(preferenceService.resetToDefaults());
    }
}
