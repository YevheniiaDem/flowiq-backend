package com.flowiq.unit.service;

import com.flowiq.dto.request.SendChatMessageRequest;
import com.flowiq.entity.ChatConversation;
import com.flowiq.entity.Transaction;
import com.flowiq.entity.User;
import com.flowiq.exception.ResourceNotFoundException;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.repository.ChatConversationRepository;
import com.flowiq.repository.TransactionRepository;
import com.flowiq.repository.UserRepository;
import com.flowiq.service.ChatService;
import com.flowiq.service.TransactionSeedService;
import com.flowiq.unit.support.SecurityTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ChatService unit tests")
class ChatServiceTest {

    private static final Long USER_ID = 1L;
    private static final String EMAIL = "chat@test.flowiq";

    @Mock
    private ChatConversationRepository conversationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionSeedService transactionSeedService;

    @InjectMocks
    private ChatService chatService;

    private User user;

    @BeforeEach
    void setUp() {
        user = SecurityTestSupport.testUser(USER_ID, EMAIL);
        SecurityTestSupport.authenticate(user);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                anyLong(), any(Transaction.Type.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumExpensesByCategory(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(transactionRepository.sumRevenueByCategory(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        SecurityTestSupport.clearSecurityContext();
    }

    @Test
    @DisplayName("getConversations returns user conversations")
    void getConversations_success() {
        ChatConversation conversation = sampleConversation(1L, "Revenue question");
        when(conversationRepository.findByUserIdOrderByUpdatedAtDesc(USER_ID))
                .thenReturn(List.of(conversation));

        var conversations = chatService.getConversations();

        assertThat(conversations).hasSize(1);
        assertThat(conversations.get(0).getTitle()).isEqualTo("Revenue question");
    }

    @Test
    @DisplayName("getConversation returns owned conversation")
    void getConversation_success() {
        ChatConversation conversation = sampleConversation(2L, "Expenses");
        when(conversationRepository.findByIdAndUserId(2L, USER_ID)).thenReturn(Optional.of(conversation));

        var response = chatService.getConversation(2L);

        assertThat(response.getId()).isEqualTo("2");
        assertThat(response.getTitle()).isEqualTo("Expenses");
    }

    @Test
    @DisplayName("getConversation throws when conversation not found")
    void getConversation_notFound() {
        when(conversationRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.getConversation(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conversation not found");
    }

    @Test
    @DisplayName("sendMessage creates new conversation and returns AI reply")
    void sendMessage_newConversation() {
        SendChatMessageRequest request = new SendChatMessageRequest();
        request.setMessage("What is my revenue?");

        when(conversationRepository.save(any(ChatConversation.class))).thenAnswer(invocation -> {
            ChatConversation saved = invocation.getArgument(0);
            saved.setId(10L);
            LocalDateTime now = LocalDateTime.now();
            saved.getMessages().forEach(message -> message.setCreatedAt(now));
            return saved;
        });

        var response = chatService.sendMessage(request);

        assertThat(response.getConversationId()).isEqualTo("10");
        assertThat(response.getUserMessage().getContent()).isEqualTo("What is my revenue?");
        assertThat(response.getAssistantMessage().getContent()).isNotBlank();
        verify(transactionSeedService).seedIfEmpty(user);
    }

    @Test
    @DisplayName("sendMessage appends to existing conversation")
    void sendMessage_existingConversation() {
        ChatConversation conversation = sampleConversation(5L, "Existing chat");
        SendChatMessageRequest request = new SendChatMessageRequest();
        request.setConversationId(5L);
        request.setMessage("Show expenses");

        when(conversationRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(ChatConversation.class))).thenAnswer(invocation -> {
            ChatConversation saved = invocation.getArgument(0);
            LocalDateTime now = LocalDateTime.now();
            saved.getMessages().forEach(message -> message.setCreatedAt(now));
            return saved;
        });

        var response = chatService.sendMessage(request);

        assertThat(response.getConversationId()).isEqualTo("5");
        assertThat(conversation.getMessages()).hasSize(2);
    }

    @Test
    @DisplayName("sendMessage throws when existing conversation not found")
    void sendMessage_conversationNotFound() {
        SendChatMessageRequest request = new SendChatMessageRequest();
        request.setConversationId(88L);
        request.setMessage("Hello");

        when(conversationRepository.findByIdAndUserId(88L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.sendMessage(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conversation not found");
    }

    @Test
    @DisplayName("sendMessage replies with profit details for profit keyword")
    void sendMessage_profitKeyword() {
        SendChatMessageRequest request = new SendChatMessageRequest();
        request.setMessage("What is my profit this month?");

        when(transactionRepository.sumByUserAndTypeAndDateRange(anyLong(), eq(Transaction.Type.REVENUE), any(), any()))
                .thenReturn(new BigDecimal("5000"));
        when(transactionRepository.sumByUserAndTypeAndDateRange(anyLong(), eq(Transaction.Type.EXPENSE), any(), any()))
                .thenReturn(new BigDecimal("2000"));
        when(conversationRepository.save(any(ChatConversation.class))).thenAnswer(invocation -> {
            ChatConversation saved = invocation.getArgument(0);
            saved.setId(11L);
            LocalDateTime now = LocalDateTime.now();
            saved.getMessages().forEach(message -> message.setCreatedAt(now));
            return saved;
        });

        var response = chatService.sendMessage(request);

        assertThat(response.getAssistantMessage().getContent()).contains("Прибуток");
    }

    @Test
    @DisplayName("sendMessage replies with marketing analysis for marketing keyword")
    void sendMessage_marketingKeyword() {
        SendChatMessageRequest request = new SendChatMessageRequest();
        request.setMessage("How much did I spend on marketing?");

        when(transactionRepository.sumByUserAndTypeAndDateRange(anyLong(), any(), any(), any()))
                .thenReturn(new BigDecimal("1000"));
        when(transactionRepository.sumExpensesByCategory(anyLong(), any(), any()))
                .thenReturn(List.of(categorySum("Marketing", "400")));
        when(conversationRepository.save(any(ChatConversation.class))).thenAnswer(invocation -> {
            ChatConversation saved = invocation.getArgument(0);
            saved.setId(12L);
            LocalDateTime now = LocalDateTime.now();
            saved.getMessages().forEach(message -> message.setCreatedAt(now));
            return saved;
        });

        var response = chatService.sendMessage(request);

        assertThat(response.getAssistantMessage().getContent()).contains("маркетинг");
    }

    private TransactionRepository.CategorySumProjection categorySum(String category, String amount) {
        return new TransactionRepository.CategorySumProjection() {
            @Override
            public String getCategory() {
                return category;
            }

            @Override
            public java.math.BigDecimal getAmount() {
                return new java.math.BigDecimal(amount);
            }
        };
    }

    @Test
    @DisplayName("rejects unauthenticated access")
    void rejectsUnauthenticated() {
        SecurityTestSupport.clearSecurityContext();

        assertThatThrownBy(() -> chatService.getConversations())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Not authenticated");
    }

    private ChatConversation sampleConversation(Long id, String title) {
        ChatConversation conversation = new ChatConversation();
        conversation.setId(id);
        conversation.setUser(user);
        conversation.setTitle(title);
        conversation.setMessages(new ArrayList<>());
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());
        return conversation;
    }
}
