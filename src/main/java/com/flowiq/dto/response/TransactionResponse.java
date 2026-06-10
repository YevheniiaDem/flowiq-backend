package com.flowiq.dto.response;

import com.flowiq.dto.request.CreateTransactionRequest;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionResponse {

    private Long id;
    private CreateTransactionRequest.TransactionTypeDto type;
    private BigDecimal amount;
    private String category;
    private String description;
    private LocalDate transactionDate;
    private boolean autoCategorized;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
