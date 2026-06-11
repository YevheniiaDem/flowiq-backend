package com.flowiq.dto.response;

import com.flowiq.dto.request.CreateTransactionRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Transaction details")
public class TransactionResponse {

    @Schema(description = "Transaction ID", example = "42")
    private Long id;

    @Schema(description = "Transaction type", example = "EXPENSE")
    private CreateTransactionRequest.TransactionTypeDto type;

    @Schema(description = "Amount in UAH", example = "1500.00")
    private BigDecimal amount;

    @Schema(description = "Category", example = "Оренда")
    private String category;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Transaction date", example = "2026-06-01")
    private LocalDate transactionDate;

    @Schema(description = "Whether category was assigned automatically", example = "false")
    private boolean autoCategorized;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}
