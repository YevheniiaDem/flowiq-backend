package com.flowiq.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "Request to create a new transaction")
public class CreateTransactionRequest {

    @NotNull(message = "Type is required")
    @Schema(description = "Transaction type", example = "EXPENSE", requiredMode = Schema.RequiredMode.REQUIRED)
    private TransactionTypeDto type;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Schema(description = "Transaction amount in UAH", example = "1500.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @NotBlank(message = "Category is required")
    @Size(max = 100, message = "Category must not exceed 100 characters")
    @Schema(description = "Transaction category", example = "Оренда", requiredMode = Schema.RequiredMode.REQUIRED)
    private String category;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    @Schema(description = "Optional description", example = "Оренда офісу за червень")
    private String description;

    @NotNull(message = "Transaction date is required")
    @Schema(description = "Date of the transaction", example = "2026-06-01", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate transactionDate;

    @Schema(description = "Transaction type enum")
    public enum TransactionTypeDto {
        INCOME,
        EXPENSE
    }
}
