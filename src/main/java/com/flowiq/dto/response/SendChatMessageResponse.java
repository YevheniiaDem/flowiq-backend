package com.flowiq.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response after sending a chat message")
public class SendChatMessageResponse {

    private String conversationId;
    private ChatMessageResponse userMessage;
    private ChatMessageResponse assistantMessage;
}
