package com.flowiq.profile.controller;

import com.flowiq.audit.AuditEventType;
import com.flowiq.audit.ResourceType;
import com.flowiq.audit.aspect.Auditable;
import com.flowiq.config.OpenApiConfig;
import com.flowiq.config.openapi.ApiErrorResponses;
import com.flowiq.profile.dto.request.ChangePasswordRequest;
import com.flowiq.profile.dto.request.UpdateFopProfileRequest;
import com.flowiq.profile.dto.request.UpdateProfileRequest;
import com.flowiq.profile.dto.response.FopProfileResponse;
import com.flowiq.profile.dto.response.ProfileResponse;
import com.flowiq.profile.dto.response.SessionResponse;
import com.flowiq.profile.service.AvatarStorageService;
import com.flowiq.profile.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

@Tag(name = "Profile", description = "User profile, FOP settings, password, and session management")
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final AvatarStorageService avatarStorageService;

    @Operation(summary = "Get personal profile")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = ProfileResponse.class)))
    @ApiErrorResponses
    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile() {
        return ResponseEntity.ok(profileService.getProfile());
    }

    @Operation(summary = "Update personal profile")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = ProfileResponse.class)))
    @ApiErrorResponses
    @Auditable(value = AuditEventType.PROFILE_UPDATED, resourceType = ResourceType.USER)
    @PutMapping
    public ResponseEntity<ProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(profileService.updateProfile(request));
    }

    @Operation(summary = "Upload profile avatar")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = ProfileResponse.class)))
    @ApiErrorResponses
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfileResponse> uploadAvatar(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(profileService.uploadAvatar(file));
    }

    @Operation(summary = "Get avatar image", security = {})
    @GetMapping("/avatars/{filename}")
    public ResponseEntity<Resource> getAvatar(@PathVariable String filename) {
        Path path = avatarStorageService.resolveAvatarPath(filename);
        FileSystemResource resource = new FileSystemResource(path);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @Operation(summary = "Get FOP profile")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = FopProfileResponse.class)))
    @ApiErrorResponses
    @GetMapping("/fop")
    public ResponseEntity<FopProfileResponse> getFopProfile() {
        return ResponseEntity.ok(profileService.getFopProfile());
    }

    @Operation(summary = "Update FOP profile")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = FopProfileResponse.class)))
    @ApiErrorResponses
    @Auditable(value = AuditEventType.FOP_UPDATED, resourceType = ResourceType.USER)
    @PutMapping("/fop")
    public ResponseEntity<FopProfileResponse> updateFopProfile(@Valid @RequestBody UpdateFopProfileRequest request) {
        return ResponseEntity.ok(profileService.updateFopProfile(request));
    }

    @Operation(summary = "Change password")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponse(responseCode = "204", description = "Password changed")
    @ApiErrorResponses
    @Auditable(value = AuditEventType.PASSWORD_CHANGED, resourceType = ResourceType.USER)
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        profileService.changePassword(request, httpRequest);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List active sessions")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiErrorResponses
    @GetMapping("/sessions")
    public ResponseEntity<List<SessionResponse>> listSessions(HttpServletRequest httpRequest) {
        return ResponseEntity.ok(profileService.listSessions(httpRequest));
    }

    @Operation(summary = "Logout current session")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponse(responseCode = "204")
    @ApiErrorResponses
    @Auditable(value = AuditEventType.SESSION_TERMINATED, resourceType = ResourceType.SESSION)
    @PostMapping("/sessions/logout-current")
    public ResponseEntity<Void> logoutCurrentSession(HttpServletRequest httpRequest) {
        profileService.logoutCurrentSession(httpRequest);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Logout all sessions")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponse(responseCode = "204")
    @ApiErrorResponses
    @Auditable(value = AuditEventType.SESSION_TERMINATED, resourceType = ResourceType.SESSION)
    @PostMapping("/sessions/logout-all")
    public ResponseEntity<Void> logoutAllSessions(HttpServletRequest httpRequest) {
        profileService.logoutAllSessions(httpRequest);
        return ResponseEntity.noContent().build();
    }
}
