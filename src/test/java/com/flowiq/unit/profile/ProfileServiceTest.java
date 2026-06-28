package com.flowiq.unit.profile;

import com.flowiq.audit.service.AuditService;
import com.flowiq.entity.User;
import com.flowiq.exception.BadRequestException;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.profile.dto.request.ChangePasswordRequest;
import com.flowiq.profile.dto.request.UpdateFopProfileRequest;
import com.flowiq.profile.dto.request.UpdateProfileRequest;
import com.flowiq.profile.dto.response.ProfileResponse;
import com.flowiq.profile.entity.FopProfile;
import com.flowiq.profile.entity.TaxSystem;
import com.flowiq.profile.entity.UserSession;
import com.flowiq.profile.repository.FopProfileRepository;
import com.flowiq.profile.service.AvatarStorageService;
import com.flowiq.profile.service.FopProfileService;
import com.flowiq.profile.service.ProfileService;
import com.flowiq.profile.service.SessionService;
import com.flowiq.repository.UserRepository;
import com.flowiq.unit.support.SecurityTestSupport;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ProfileService unit tests")
class ProfileServiceTest {

    private static final Long USER_ID = 1L;
    private static final String EMAIL = "profile@test.flowiq";

    @Mock
    private UserRepository userRepository;
    @Mock
    private FopProfileRepository fopProfileRepository;
    @Mock
    private FopProfileService fopProfileService;
    @Mock
    private SessionService sessionService;
    @Mock
    private AvatarStorageService avatarStorageService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditService auditService;
    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private ProfileService profileService;

    private User user;

    @BeforeEach
    void setUp() {
        user = SecurityTestSupport.testUser(USER_ID, EMAIL);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setPassword("encoded-password");
        SecurityTestSupport.authenticate(user);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    }

    @AfterEach
    void tearDown() {
        SecurityTestSupport.clearSecurityContext();
    }

    @Test
    @DisplayName("getProfile returns current user profile")
    void getProfile_success() {
        ProfileResponse response = profileService.getProfile();

        assertThat(response.getEmail()).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("updateProfile updates personal fields")
    void updateProfile_success() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Jane");
        request.setLastName("Doe");
        request.setPhone("+380501234567");
        request.setCompany("FlowIQ");

        when(userRepository.save(user)).thenReturn(user);

        ProfileResponse response = profileService.updateProfile(request);

        assertThat(response.getFirstName()).isEqualTo("Jane");
        assertThat(response.getLastName()).isEqualTo("Doe");
        verify(auditService).log(any());
    }

    @Test
    @DisplayName("updateProfile rejects email change")
    void updateProfile_rejectsEmailChange() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Jane");
        request.setEmail("new@test.flowiq");

        assertThatThrownBy(() -> profileService.updateProfile(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Email change requires verification and is not available yet");
    }

    @Test
    @DisplayName("uploadAvatar stores file and updates user")
    void uploadAvatar_success() {
        MockMultipartFile file = new MockMultipartFile("avatar", "avatar.png", "image/png", new byte[]{1, 2, 3});
        when(avatarStorageService.storeAvatar(USER_ID, file)).thenReturn("/avatars/1.png");
        when(userRepository.save(user)).thenReturn(user);

        ProfileResponse response = profileService.uploadAvatar(file);

        assertThat(response.getAvatar()).isEqualTo("/avatars/1.png");
        verify(auditService).log(any());
    }

    @Test
    @DisplayName("getFopProfile returns profile from service")
    void getFopProfile_success() {
        FopProfile profile = sampleFopProfile();
        when(fopProfileService.getOrCreateForUser(user)).thenReturn(profile);

        var response = profileService.getFopProfile();

        assertThat(response.getFopGroup()).isEqualTo(2);
    }

    @Test
    @DisplayName("updateFopProfile persists changes")
    void updateFopProfile_success() {
        FopProfile profile = sampleFopProfile();
        when(fopProfileService.getOrCreateForUser(user)).thenReturn(profile);
        when(fopProfileRepository.save(profile)).thenReturn(profile);

        UpdateFopProfileRequest request = new UpdateFopProfileRequest();
        request.setFopGroup(1);
        request.setTaxSystem(TaxSystem.SINGLE_TAX);
        request.setTaxRate(new BigDecimal("0.10"));
        request.setKvedCodes(List.of("62.01"));

        var response = profileService.updateFopProfile(request);

        assertThat(response.getFopGroup()).isEqualTo(1);
        verify(auditService).log(any());
    }

    @Test
    @DisplayName("changePassword rejects mismatched confirmation")
    void changePassword_mismatch() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPass1!");
        request.setNewPassword("NewPass1!");
        request.setConfirmPassword("Different1!");

        assertThatThrownBy(() -> profileService.changePassword(request, httpServletRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("New password and confirmation do not match");
    }

    @Test
    @DisplayName("changePassword rejects incorrect current password")
    void changePassword_wrongCurrent() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("WrongPass1!");
        request.setNewPassword("NewPass1!");
        request.setConfirmPassword("NewPass1!");

        when(passwordEncoder.matches("WrongPass1!", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> profileService.changePassword(request, httpServletRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Current password is incorrect");
    }

    @Test
    @DisplayName("changePassword updates password and revokes other sessions")
    void changePassword_success() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPass1!");
        request.setNewPassword("NewPass1!");
        request.setConfirmPassword("NewPass1!");

        when(passwordEncoder.matches("OldPass1!", "encoded-password")).thenReturn(true);
        when(passwordEncoder.matches("NewPass1!", "encoded-password")).thenReturn(false);
        when(passwordEncoder.encode("NewPass1!")).thenReturn("new-encoded");
        when(sessionService.resolveCurrentSessionId(httpServletRequest)).thenReturn("session-1");

        profileService.changePassword(request, httpServletRequest);

        verify(sessionService).revokeOtherSessions(USER_ID, "session-1");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("listSessions marks current session")
    void listSessions_success() {
        UserSession session = new UserSession();
        session.setId("session-1");
        when(sessionService.resolveCurrentSessionId(httpServletRequest)).thenReturn("session-1");
        when(sessionService.listActiveSessions(USER_ID)).thenReturn(List.of(session));

        var sessions = profileService.listSessions(httpServletRequest);

        assertThat(sessions).hasSize(1);
        assertThat(sessions.get(0).isCurrent()).isTrue();
    }

    @Test
    @DisplayName("logoutCurrentSession rejects when session cannot be resolved")
    void logoutCurrentSession_noSession() {
        when(sessionService.resolveCurrentSessionId(httpServletRequest)).thenReturn(null);

        assertThatThrownBy(() -> profileService.logoutCurrentSession(httpServletRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Current session could not be resolved");
    }

    @Test
    @DisplayName("rejects unauthenticated access")
    void rejectsUnauthenticated() {
        SecurityTestSupport.clearSecurityContext();

        assertThatThrownBy(() -> profileService.getProfile())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Not authenticated");
    }

    private FopProfile sampleFopProfile() {
        FopProfile profile = new FopProfile();
        profile.setId(1L);
        profile.setUser(user);
        profile.setFopGroup(2);
        profile.setTaxSystem(TaxSystem.SINGLE_TAX);
        profile.setTaxRate(new BigDecimal("0.05"));
        profile.setKvedCodes(List.of("62.01"));
        return profile;
    }
}
