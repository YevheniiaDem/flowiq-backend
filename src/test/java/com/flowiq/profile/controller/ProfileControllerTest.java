package com.flowiq.profile.controller;

import com.flowiq.exception.ResourceNotFoundException;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.profile.dto.request.ChangePasswordRequest;
import com.flowiq.profile.dto.request.UpdateFopProfileRequest;
import com.flowiq.profile.dto.request.UpdateProfileRequest;
import com.flowiq.profile.dto.response.FopProfileResponse;
import com.flowiq.profile.dto.response.ProfileResponse;
import com.flowiq.profile.dto.response.SessionResponse;
import com.flowiq.profile.entity.TaxSystem;
import com.flowiq.profile.service.AvatarStorageService;
import com.flowiq.profile.service.ProfileService;
import com.flowiq.unit.support.ControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileController tests")
class ProfileControllerTest {

    @Mock
    private ProfileService profileService;

    @Mock
    private AvatarStorageService avatarStorageService;

    @InjectMocks
    private ProfileController profileController;

    private MockMvc mockMvc;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.standaloneMockMvc(profileController);
    }

    @Test
    @DisplayName("GET /api/profile returns personal profile")
    void getProfile_success() throws Exception {
        when(profileService.getProfile()).thenReturn(
                ProfileResponse.builder().id("1").email("user@example.com").firstName("Ivan").build());

        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    @Test
    @DisplayName("PUT /api/profile updates personal profile")
    void updateProfile_success() throws Exception {
        UpdateProfileRequest request = validUpdateProfileRequest();

        when(profileService.updateProfile(any(UpdateProfileRequest.class)))
                .thenReturn(ProfileResponse.builder().firstName("Ivan").lastName("Petrenko").build());

        mockMvc.perform(put("/api/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestSupport.OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Petrenko"));
    }

    @Test
    @DisplayName("PUT /api/profile rejects blank first name")
    void updateProfile_validationError() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("   ");

        mockMvc.perform(put("/api/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestSupport.OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/profile/avatar uploads avatar")
    void uploadAvatar_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[]{1, 2, 3});

        when(profileService.uploadAvatar(any())).thenReturn(
                ProfileResponse.builder().avatar("/api/profile/avatars/1_test.png").build());

        mockMvc.perform(multipart("/api/profile/avatar").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatar").value("/api/profile/avatars/1_test.png"));
    }

    @Test
    @DisplayName("GET /api/profile/avatars/{filename} returns avatar image")
    void getAvatar_success() throws Exception {
        Path avatarFile = tempDir.resolve("test-avatar.jpg");
        Files.write(avatarFile, new byte[]{1, 2, 3});
        when(avatarStorageService.resolveAvatarPath("test-avatar.jpg")).thenReturn(avatarFile);

        mockMvc.perform(get("/api/profile/avatars/test-avatar.jpg"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/profile/fop returns FOP profile")
    void getFopProfile_success() throws Exception {
        when(profileService.getFopProfile()).thenReturn(
                FopProfileResponse.builder().fopGroup(2).taxSystem(TaxSystem.SINGLE_TAX).build());

        mockMvc.perform(get("/api/profile/fop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fopGroup").value(2));
    }

    @Test
    @DisplayName("PUT /api/profile/fop updates FOP profile")
    void updateFopProfile_success() throws Exception {
        UpdateFopProfileRequest request = validUpdateFopProfileRequest();

        when(profileService.updateFopProfile(any(UpdateFopProfileRequest.class)))
                .thenReturn(FopProfileResponse.builder().fopGroup(2).taxSystem(TaxSystem.SINGLE_TAX).build());

        mockMvc.perform(put("/api/profile/fop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestSupport.OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taxSystem").value("SINGLE_TAX"));
    }

    @Test
    @DisplayName("PUT /api/profile/fop rejects missing tax system")
    void updateFopProfile_validationError() throws Exception {
        UpdateFopProfileRequest request = new UpdateFopProfileRequest();
        request.setFopGroup(2);
        request.setKvedCodes(List.of("62.01"));

        mockMvc.perform(put("/api/profile/fop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestSupport.OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/profile/change-password changes password")
    void changePassword_success() throws Exception {
        ChangePasswordRequest request = validChangePasswordRequest();

        mockMvc.perform(post("/api/profile/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestSupport.OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(profileService).changePassword(any(ChangePasswordRequest.class), any());
    }

    @Test
    @DisplayName("POST /api/profile/change-password rejects weak password")
    void changePassword_validationError() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPass123!");
        request.setNewPassword("weak");
        request.setConfirmPassword("weak");

        mockMvc.perform(post("/api/profile/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestSupport.OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/profile/sessions returns active sessions")
    void listSessions_success() throws Exception {
        when(profileService.listSessions(any())).thenReturn(List.of(
                SessionResponse.builder().id("sess-1").deviceLabel("Chrome").current(true).build()));

        mockMvc.perform(get("/api/profile/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].current").value(true));
    }

    @Test
    @DisplayName("POST /api/profile/sessions/logout-current returns 204")
    void logoutCurrentSession_success() throws Exception {
        mockMvc.perform(post("/api/profile/sessions/logout-current"))
                .andExpect(status().isNoContent());

        verify(profileService).logoutCurrentSession(any());
    }

    @Test
    @DisplayName("POST /api/profile/sessions/logout-all returns 204")
    void logoutAllSessions_success() throws Exception {
        mockMvc.perform(post("/api/profile/sessions/logout-all"))
                .andExpect(status().isNoContent());

        verify(profileService).logoutAllSessions(any());
    }

    @Test
    @DisplayName("GET /api/profile returns 401 when unauthorized")
    void getProfile_unauthorized() throws Exception {
        doThrow(new UnauthorizedException("Not authenticated")).when(profileService).getProfile();

        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/profile/fop returns 404 when FOP profile not found")
    void getFopProfile_notFound() throws Exception {
        when(profileService.getFopProfile()).thenThrow(new ResourceNotFoundException("FOP profile not found"));

        mockMvc.perform(get("/api/profile/fop"))
                .andExpect(status().isNotFound());
    }

    private UpdateProfileRequest validUpdateProfileRequest() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Ivan");
        request.setLastName("Petrenko");
        request.setEmail("user@example.com");
        return request;
    }

    private UpdateFopProfileRequest validUpdateFopProfileRequest() {
        UpdateFopProfileRequest request = new UpdateFopProfileRequest();
        request.setFopGroup(2);
        request.setTaxSystem(TaxSystem.SINGLE_TAX);
        request.setTaxRate(new BigDecimal("0.05"));
        request.setKvedCodes(List.of("62.01"));
        return request;
    }

    private ChangePasswordRequest validChangePasswordRequest() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPass123!");
        request.setNewPassword("NewPass456!");
        request.setConfirmPassword("NewPass456!");
        return request;
    }
}
