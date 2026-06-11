package com.flowiq.dto.response;

import com.flowiq.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User profile information")
public class UserResponse {

    @Schema(description = "User ID", example = "1")
    private String id;

    @Schema(description = "Email address", example = "user@example.com")
    private String email;

    @Schema(description = "Full name", example = "Іван Петренко")
    private String name;

    @Schema(description = "User role", example = "user")
    private String role;

    @Schema(description = "Avatar URL")
    private String avatar;

    @Schema(description = "Company or FOP name", example = "ФОП Петренко")
    private String company;

    @Schema(description = "Account creation timestamp")
    private String createdAt;

    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .id(String.valueOf(user.getId()))
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name().toLowerCase())
                .avatar(user.getAvatarUrl())
                .company(user.getCompany())
                .createdAt(user.getCreatedAt() != null
                        ? user.getCreatedAt().toString()
                        : LocalDateTime.now().toString())
                .build();
    }
}
