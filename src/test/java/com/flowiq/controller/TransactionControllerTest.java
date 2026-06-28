package com.flowiq.controller;

import com.flowiq.dto.request.CreateTransactionRequest;
import com.flowiq.dto.request.UpdateTransactionRequest;
import com.flowiq.dto.response.TransactionPageResponse;
import com.flowiq.dto.response.TransactionResponse;
import com.flowiq.dto.response.TransactionSummaryResponse;
import com.flowiq.exception.GlobalExceptionHandler;
import com.flowiq.exception.ResourceNotFoundException;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionController tests")
class TransactionControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionController transactionController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(transactionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/transactions returns paginated list")
    void list_success() throws Exception {
        when(transactionService.getTransactions(isNull(), eq(0), eq(10), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(TransactionPageResponse.builder()
                        .content(List.of())
                        .page(0)
                        .size(10)
                        .totalElements(0)
                        .totalPages(0)
                        .build());

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    @DisplayName("GET /api/transactions returns 401 when service rejects auth")
    void list_unauthorized() throws Exception {
        when(transactionService.getTransactions(any(), any(Integer.class), any(Integer.class), any(), any(), any(), any()))
                .thenThrow(new UnauthorizedException("Not authenticated"));

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/transactions/summary returns totals")
    void summary_success() throws Exception {
        when(transactionService.getSummary(isNull(), isNull(), isNull()))
                .thenReturn(TransactionSummaryResponse.builder()
                        .totalRevenue(new BigDecimal("1000"))
                        .totalExpenses(new BigDecimal("400"))
                        .netProfit(new BigDecimal("600"))
                        .transactionCount(5)
                        .build());

        mockMvc.perform(get("/api/transactions/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netProfit").value(600));
    }

    @Test
    @DisplayName("GET /api/transactions/{id} returns 404 when not found")
    void getById_notFound() throws Exception {
        when(transactionService.getById(99L)).thenThrow(new ResourceNotFoundException("Transaction not found"));

        mockMvc.perform(get("/api/transactions/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/transactions creates transaction")
    void create_success() throws Exception {
        CreateTransactionRequest request = validCreateRequest();

        when(transactionService.create(any(CreateTransactionRequest.class)))
                .thenReturn(TransactionResponse.builder()
                        .id(1L)
                        .type(CreateTransactionRequest.TransactionTypeDto.EXPENSE)
                        .amount(new BigDecimal("1500"))
                        .category("Office")
                        .transactionDate(LocalDate.of(2026, 6, 1))
                        .build());

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("POST /api/transactions rejects invalid amount")
    void create_validationError() throws Exception {
        CreateTransactionRequest request = validCreateRequest();
        request.setAmount(BigDecimal.ZERO);

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/transactions/{id} updates transaction")
    void update_success() throws Exception {
        UpdateTransactionRequest request = new UpdateTransactionRequest();
        request.setType(CreateTransactionRequest.TransactionTypeDto.INCOME);
        request.setAmount(new BigDecimal("2000"));
        request.setCategory("Services");
        request.setTransactionDate(LocalDate.of(2026, 6, 2));

        when(transactionService.update(eq(1L), any(UpdateTransactionRequest.class)))
                .thenReturn(TransactionResponse.builder().id(1L).build());

        mockMvc.perform(put("/api/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/transactions/{id} returns 204")
    void delete_success() throws Exception {
        mockMvc.perform(delete("/api/transactions/1"))
                .andExpect(status().isNoContent());

        verify(transactionService).delete(1L);
    }

    private CreateTransactionRequest validCreateRequest() {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setType(CreateTransactionRequest.TransactionTypeDto.EXPENSE);
        request.setAmount(new BigDecimal("1500"));
        request.setCategory("Office");
        request.setTransactionDate(LocalDate.of(2026, 6, 1));
        return request;
    }
}
