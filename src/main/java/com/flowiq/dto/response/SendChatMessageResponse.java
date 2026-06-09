package com.flowiq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendChatMessageResponse {

    private String conversationId;
    private ChatMessageResponse userMessage;
    private ChatMessageResponse assistantMessage;
}
