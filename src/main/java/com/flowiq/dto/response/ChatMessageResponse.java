package com.flowiq.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.flowiq.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Chat message")
public class ChatMessageResponse {

    private String id;
    private String role;
    private String content;
    private String timestamp;

    public static ChatMessageResponse fromEntity(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(String.valueOf(message.getId()))
                .role(message.getRole().name().toLowerCase())
                .content(message.getContent())
                .timestamp(message.getCreatedAt().toString())
                .build();
    }
}
