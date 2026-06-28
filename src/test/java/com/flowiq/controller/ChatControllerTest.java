package com.flowiq.controller;

import com.flowiq.dto.request.SendChatMessageRequest;
import com.flowiq.dto.response.ChatConversationResponse;
import com.flowiq.dto.response.ChatMessageResponse;
import com.flowiq.dto.response.SendChatMessageResponse;
import com.flowiq.exception.ResourceNotFoundException;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.service.ChatService;
import com.flowiq.unit.support.ControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatController tests")
class ChatControllerTest {

    @Mock private ChatService chatService;

    @InjectMocks
    private ChatController chatController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.standaloneMockMvc(chatController);
    }

    @Test
    @DisplayName("GET /api/chat/conversations returns list")
    void conversations_success() throws Exception {
        when(chatService.getConversations()).thenReturn(List.of(
                ChatConversationResponse.builder().id("1").title("Tax question").build()));

        mockMvc.perform(get("/api/chat/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Tax question"));
    }

    @Test
    @DisplayName("GET /api/chat/conversations/{id} returns conversation")
    void conversationById_success() throws Exception {
        when(chatService.getConversation(1L)).thenReturn(
                ChatConversationResponse.builder().id("1").title("Tax question").build());

        mockMvc.perform(get("/api/chat/conversations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"));
    }

    @Test
    @DisplayName("GET /api/chat/conversations/{id} returns 404 when not found")
    void conversationById_notFound() throws Exception {
        when(chatService.getConversation(99L)).thenThrow(new ResourceNotFoundException("Conversation not found"));

        mockMvc.perform(get("/api/chat/conversations/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/chat/message creates message and returns 201")
    void sendMessage_success() throws Exception {
        SendChatMessageRequest request = new SendChatMessageRequest();
        request.setMessage("What is my profit this month?");

        when(chatService.sendMessage(any(SendChatMessageRequest.class)))
                .thenReturn(SendChatMessageResponse.builder()
                        .conversationId("1")
                        .assistantMessage(ChatMessageResponse.builder()
                                .content("Your profit is positive.")
                                .build())
                        .build());

        mockMvc.perform(post("/api/chat/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestSupport.OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assistantMessage.content").value("Your profit is positive."));
    }

    @Test
    @DisplayName("POST /api/chat/message rejects blank message")
    void sendMessage_validationError() throws Exception {
        SendChatMessageRequest request = new SendChatMessageRequest();
        request.setMessage("   ");

        mockMvc.perform(post("/api/chat/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestSupport.OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/chat/conversations returns 401 when unauthorized")
    void conversations_unauthorized() throws Exception {
        doThrow(new UnauthorizedException("Not authenticated")).when(chatService).getConversations();

        mockMvc.perform(get("/api/chat/conversations"))
                .andExpect(status().isUnauthorized());
    }
}
