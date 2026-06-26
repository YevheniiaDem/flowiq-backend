package com.flowiq.profile.dto.response;

import com.flowiq.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User personal profile")
public class ProfileResponse {

    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String name;
    private String phone;
    private String avatar;
    private String company;
    private String role;
    private String createdAt;
    private String updatedAt;

    public static ProfileResponse fromEntity(User user) {
        return ProfileResponse.builder()
                .id(String.valueOf(user.getId()))
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .name(user.getName())
                .phone(user.getPhone())
                .avatar(user.getAvatarUrl())
                .company(user.getCompany())
                .role(user.getRole().name().toLowerCase())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .updatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null)
                .build();
    }
}
