package com.flowiq.dto.response;

import com.flowiq.entity.ChatConversation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatConversationResponse {

    private String id;
    private String title;
    private List<ChatMessageResponse> messages;
    private String createdAt;
    private String updatedAt;

    public static ChatConversationResponse fromEntity(ChatConversation conversation) {
        return ChatConversationResponse.builder()
                .id(String.valueOf(conversation.getId()))
                .title(conversation.getTitle())
                .messages(conversation.getMessages().stream()
                        .map(ChatMessageResponse::fromEntity)
                        .toList())
                .createdAt(conversation.getCreatedAt().toString())
                .updatedAt(conversation.getUpdatedAt() != null
                        ? conversation.getUpdatedAt().toString()
                        : conversation.getCreatedAt().toString())
                .build();
    }
}
