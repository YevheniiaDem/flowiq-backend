package com.flowiq.profile.service;

import com.flowiq.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class AvatarStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private final Path avatarDirectory;

    public AvatarStorageService(@Value("${flowiq.profile.avatar-dir:uploads/avatars}") String avatarDir) {
        this.avatarDirectory = Paths.get(avatarDir).toAbsolutePath().normalize();
    }

    public String storeAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Avatar file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("Avatar must be JPEG, PNG, WebP, or GIF");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BadRequestException("Avatar must be smaller than 5 MB");
        }

        String extension = switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> "bin";
        };

        try {
            Files.createDirectories(avatarDirectory);
            String filename = userId + "_" + UUID.randomUUID() + "." + extension;
            Path target = avatarDirectory.resolve(filename).normalize();
            if (!target.startsWith(avatarDirectory)) {
                throw new BadRequestException("Invalid avatar path");
            }
            file.transferTo(target);
            return "/api/profile/avatars/" + filename;
        } catch (IOException e) {
            throw new BadRequestException("Failed to store avatar");
        }
    }

    private static final java.util.regex.Pattern SAFE_FILENAME =
            java.util.regex.Pattern.compile("^[0-9]+_[0-9a-fA-F-]+\\.(jpg|png|webp|gif)$");

    public Path resolveAvatarPath(String filename) {
        if (filename == null || !SAFE_FILENAME.matcher(filename).matches()) {
            throw new BadRequestException("Invalid avatar filename");
        }
        Path resolved = avatarDirectory.resolve(filename).normalize();
        if (!resolved.startsWith(avatarDirectory)) {
            throw new BadRequestException("Invalid avatar filename");
        }
        if (!Files.exists(resolved)) {
            throw new BadRequestException("Avatar not found");
        }
        return resolved;
    }
}
