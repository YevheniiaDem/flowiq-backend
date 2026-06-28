package com.flowiq.unit.profile;

import com.flowiq.exception.BadRequestException;
import com.flowiq.profile.service.AvatarStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AvatarStorageService unit tests")
class AvatarStorageServiceTest {

    @TempDir
    Path tempDir;

    private AvatarStorageService avatarStorageService;

    @BeforeEach
    void setUp() {
        avatarStorageService = new AvatarStorageService(tempDir.toString());
    }

    @Test
    @DisplayName("storeAvatar saves JPEG and returns public URL")
    void storeAvatar_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", "image/jpeg", new byte[]{1, 2, 3});

        String url = avatarStorageService.storeAvatar(1L, file);

        assertThat(url).startsWith("/api/profile/avatars/1_");
        assertThat(url).endsWith(".jpg");
        String filename = url.substring(url.lastIndexOf('/') + 1);
        assertThat(Files.exists(tempDir.resolve(filename))).isTrue();
    }

    @Test
    @DisplayName("storeAvatar rejects empty file")
    void storeAvatar_emptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> avatarStorageService.storeAvatar(1L, file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("required");
    }

    @Test
    @DisplayName("storeAvatar rejects unsupported content type")
    void storeAvatar_invalidType() {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> avatarStorageService.storeAvatar(1L, file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("JPEG");
    }

    @Test
    @DisplayName("resolveAvatarPath returns stored file path")
    void resolveAvatarPath_success() throws Exception {
        Path avatar = tempDir.resolve("1_test.png");
        Files.write(avatar, new byte[]{1});

        Path resolved = avatarStorageService.resolveAvatarPath("1_test.png");

        assertThat(resolved).isEqualTo(avatar.normalize());
    }

    @Test
    @DisplayName("resolveAvatarPath rejects path traversal")
    void resolveAvatarPath_traversal() {
        assertThatThrownBy(() -> avatarStorageService.resolveAvatarPath("../secret.txt"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("resolveAvatarPath rejects missing file")
    void resolveAvatarPath_notFound() {
        assertThatThrownBy(() -> avatarStorageService.resolveAvatarPath("missing.png"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not found");
    }
}
