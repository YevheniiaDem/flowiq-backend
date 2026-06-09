package com.flowiq.dto.response;

import com.flowiq.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private String id;
    private String email;
    private String name;
    private String role;
    private String avatar;
    private String company;
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
