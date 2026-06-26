package com.flowiq.profile.service;

import com.flowiq.audit.AuditEventType;
import com.flowiq.audit.AuditOutcome;
import com.flowiq.audit.ResourceType;
import com.flowiq.audit.dto.AuditEventRequest;
import com.flowiq.audit.service.AuditService;
import com.flowiq.entity.User;
import com.flowiq.exception.BadRequestException;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.profile.dto.request.ChangePasswordRequest;
import com.flowiq.profile.dto.request.UpdateFopProfileRequest;
import com.flowiq.profile.dto.request.UpdateProfileRequest;
import com.flowiq.profile.dto.response.FopProfileResponse;
import com.flowiq.profile.dto.response.ProfileResponse;
import com.flowiq.profile.dto.response.SessionResponse;
import com.flowiq.profile.entity.FopProfile;
import com.flowiq.profile.repository.FopProfileRepository;
import com.flowiq.repository.UserRepository;
import com.flowiq.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final FopProfileRepository fopProfileRepository;
    private final FopProfileService fopProfileService;
    private final SessionService sessionService;
    private final AvatarStorageService avatarStorageService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public ProfileResponse getProfile() {
        return ProfileResponse.fromEntity(getCurrentUserEntity());
    }

    @Transactional
    public ProfileResponse updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUserEntity();

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName() != null ? request.getLastName().trim() : "");
        user.setName(buildFullName(user.getFirstName(), user.getLastName()));
        user.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        user.setCompany(request.getCompany());

        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            // TODO: require email verification before applying change
            throw new BadRequestException("Email change requires verification and is not available yet");
        }

        User saved = userRepository.save(user);

        auditService.log(AuditEventRequest.builder()
                .actorUserId(saved.getId())
                .actorEmail(saved.getEmail())
                .actorRole(saved.getRole().name())
                .eventType(AuditEventType.PROFILE_UPDATED)
                .outcome(AuditOutcome.SUCCESS)
                .resourceType(ResourceType.USER)
                .resourceId(saved.getId())
                .metadata(Map.of("fields", "firstName,lastName,phone,company"))
                .build());

        return ProfileResponse.fromEntity(saved);
    }

    @Transactional
    public ProfileResponse uploadAvatar(MultipartFile file) {
        User user = getCurrentUserEntity();
        String avatarUrl = avatarStorageService.storeAvatar(user.getId(), file);
        user.setAvatarUrl(avatarUrl);
        User saved = userRepository.save(user);

        auditService.log(AuditEventRequest.builder()
                .actorUserId(saved.getId())
                .actorEmail(saved.getEmail())
                .actorRole(saved.getRole().name())
                .eventType(AuditEventType.PROFILE_UPDATED)
                .outcome(AuditOutcome.SUCCESS)
                .resourceType(ResourceType.USER)
                .resourceId(saved.getId())
                .metadata(Map.of("fields", "avatar"))
                .build());

        return ProfileResponse.fromEntity(saved);
    }

    @Transactional
    public FopProfileResponse getFopProfile() {
        User user = getCurrentUserEntity();
        FopProfile profile = fopProfileService.getOrCreateForUser(user);
        return FopProfileResponse.fromEntity(profile);
    }

    @Transactional
    public FopProfileResponse updateFopProfile(UpdateFopProfileRequest request) {
        User user = getCurrentUserEntity();
        FopProfile profile = fopProfileService.getOrCreateForUser(user);

        profile.setFopGroup(request.getFopGroup());
        profile.setTaxSystem(request.getTaxSystem());
        profile.setVatPayer(request.isVatPayer());
        profile.setTaxRate(request.getTaxRate());
        profile.setRegistrationDate(request.getRegistrationDate());
        profile.setRegion(request.getRegion());
        profile.setMainKved(request.getMainKved());
        profile.setMainKvedName(request.getMainKvedName());
        profile.setKvedCodes(new ArrayList<>(request.getKvedCodes()));

        FopProfile saved = fopProfileRepository.save(profile);

        auditService.log(AuditEventRequest.builder()
                .actorUserId(user.getId())
                .actorEmail(user.getEmail())
                .actorRole(user.getRole().name())
                .eventType(AuditEventType.FOP_UPDATED)
                .outcome(AuditOutcome.SUCCESS)
                .resourceType(ResourceType.USER)
                .resourceId(user.getId())
                .metadata(Map.of(
                        "fopGroup", saved.getFopGroup(),
                        "taxSystem", saved.getTaxSystem().name()
                ))
                .build());

        return FopProfileResponse.fromEntity(saved);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request, HttpServletRequest httpRequest) {
        if (!Objects.equals(request.getNewPassword(), request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirmation do not match");
        }

        User user = getCurrentUserEntity();
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("New password must be different from current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        String currentSessionId = sessionService.resolveCurrentSessionId(httpRequest);
        if (currentSessionId != null) {
            sessionService.revokeOtherSessions(user.getId(), currentSessionId);
        } else {
            sessionService.revokeAllSessions(user.getId());
        }

        auditService.log(AuditEventRequest.builder()
                .actorUserId(user.getId())
                .actorEmail(user.getEmail())
                .actorRole(user.getRole().name())
                .eventType(AuditEventType.PASSWORD_CHANGED)
                .outcome(AuditOutcome.SUCCESS)
                .resourceType(ResourceType.USER)
                .resourceId(user.getId())
                .metadata(Map.of("sessionsRevoked", "other_devices"))
                .build());
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> listSessions(HttpServletRequest httpRequest) {
        User user = getCurrentUserEntity();
        String currentSessionId = sessionService.resolveCurrentSessionId(httpRequest);
        return sessionService.listActiveSessions(user.getId()).stream()
                .map(session -> SessionResponse.fromEntity(
                        session,
                        session.getId().equals(currentSessionId)
                ))
                .toList();
    }

    @Transactional
    public void logoutCurrentSession(HttpServletRequest httpRequest) {
        User user = getCurrentUserEntity();
        String currentSessionId = sessionService.resolveCurrentSessionId(httpRequest);
        if (currentSessionId == null) {
            throw new BadRequestException("Current session could not be resolved");
        }
        sessionService.revokeSession(currentSessionId);
        logSessionTerminated(user, currentSessionId, "current");
    }

    @Transactional
    public void logoutAllSessions(HttpServletRequest httpRequest) {
        User user = getCurrentUserEntity();
        sessionService.revokeAllSessions(user.getId());
        logSessionTerminated(user, null, "all");
    }

    private void logSessionTerminated(User user, String sessionId, String scope) {
        auditService.log(AuditEventRequest.builder()
                .actorUserId(user.getId())
                .actorEmail(user.getEmail())
                .actorRole(user.getRole().name())
                .eventType(AuditEventType.SESSION_TERMINATED)
                .outcome(AuditOutcome.SUCCESS)
                .resourceType(ResourceType.SESSION)
                .resourceId(user.getId())
                .metadata(Map.of(
                        "scope", scope,
                        "sessionId", sessionId != null ? sessionId : "all"
                ))
                .build());
    }

    private String buildFullName(String firstName, String lastName) {
        if (lastName == null || lastName.isBlank()) {
            return firstName;
        }
        return firstName + " " + lastName;
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
