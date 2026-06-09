package com.flowiq.controller;

import com.flowiq.dto.request.SendChatMessageRequest;
import com.flowiq.dto.response.ChatConversationResponse;
import com.flowiq.dto.response.SendChatMessageResponse;
import com.flowiq.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/conversations")
    public ResponseEntity<List<ChatConversationResponse>> getConversations() {
        return ResponseEntity.ok(chatService.getConversations());
    }

    @GetMapping("/conversations/{id}")
    public ResponseEntity<ChatConversationResponse> getConversation(@PathVariable Long id) {
        return ResponseEntity.ok(chatService.getConversation(id));
    }

    @PostMapping("/message")
    public ResponseEntity<SendChatMessageResponse> sendMessage(
            @Valid @RequestBody SendChatMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatService.sendMessage(request));
    }
}
