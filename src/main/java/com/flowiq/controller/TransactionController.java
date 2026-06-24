package com.flowiq.controller;

import com.flowiq.audit.AuditEventType;
import com.flowiq.audit.ResourceType;
import com.flowiq.audit.aspect.Auditable;
import com.flowiq.config.OpenApiConfig;
import com.flowiq.config.openapi.ApiErrorResponses;
import com.flowiq.dto.request.CreateTransactionRequest;
import com.flowiq.dto.request.UpdateTransactionRequest;
import com.flowiq.dto.response.TransactionPageResponse;
import com.flowiq.dto.response.TransactionResponse;
import com.flowiq.dto.response.TransactionSummaryResponse;
import com.flowiq.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Transactions", description = "Manage revenue and expense transactions")
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class TransactionController {

    private final TransactionService transactionService;

    @Operation(
            summary = "List transactions",
            description = "Returns a paginated list of transactions with optional search, type, and date filters."
    )
    @ApiResponse(responseCode = "200", description = "Paginated transaction list",
            content = @Content(schema = @Schema(implementation = TransactionPageResponse.class)))
    @ApiErrorResponses
    @GetMapping
    public ResponseEntity<TransactionPageResponse> getTransactions(
            @Parameter(description = "Search by category or description") @RequestParam(required = false) String search,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field and direction, e.g. transactionDate,desc") @RequestParam(required = false) String sort,
            @Parameter(description = "Filter by transaction type") @RequestParam(required = false) CreateTransactionRequest.TransactionTypeDto type,
            @Parameter(description = "Start date (ISO format)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @Parameter(description = "End date (ISO format)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        return ResponseEntity.ok(transactionService.getTransactions(
                search, page, size, sort, type, dateFrom, dateTo));
    }

    @Operation(
            summary = "Transaction summary",
            description = "Returns aggregated totals for transactions within an optional date range and type filter."
    )
    @ApiResponse(responseCode = "200", description = "Transaction summary",
            content = @Content(schema = @Schema(implementation = TransactionSummaryResponse.class)))
    @ApiErrorResponses
    @GetMapping("/summary")
    public ResponseEntity<TransactionSummaryResponse> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) CreateTransactionRequest.TransactionTypeDto type
    ) {
        return ResponseEntity.ok(transactionService.getSummary(dateFrom, dateTo, type));
    }

    @Operation(summary = "Get transaction by ID", description = "Returns a single transaction owned by the authenticated user.")
    @ApiResponse(responseCode = "200", description = "Transaction details",
            content = @Content(schema = @Schema(implementation = TransactionResponse.class)))
    @ApiErrorResponses
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getById(@Parameter(description = "Transaction ID") @PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getById(id));
    }

    @Operation(summary = "Create transaction", description = "Creates a new revenue or expense transaction.")
    @ApiResponse(responseCode = "201", description = "Transaction created",
            content = @Content(schema = @Schema(implementation = TransactionResponse.class)))
    @ApiErrorResponses
    @Auditable(value = AuditEventType.TRANSACTION_CREATE, resourceType = ResourceType.TRANSACTION, resourceId = "#result.id")
    @PostMapping
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody CreateTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.create(request));
    }

    @Operation(summary = "Update transaction", description = "Updates an existing transaction by ID.")
    @ApiResponse(responseCode = "200", description = "Transaction updated",
            content = @Content(schema = @Schema(implementation = TransactionResponse.class)))
    @ApiErrorResponses
    @Auditable(value = AuditEventType.TRANSACTION_UPDATE, resourceType = ResourceType.TRANSACTION, resourceId = "#id")
    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> update(
            @Parameter(description = "Transaction ID") @PathVariable Long id,
            @Valid @RequestBody UpdateTransactionRequest request
    ) {
        return ResponseEntity.ok(transactionService.update(id, request));
    }

    @Operation(summary = "Delete transaction", description = "Permanently deletes a transaction by ID.")
    @ApiResponse(responseCode = "204", description = "Transaction deleted")
    @ApiErrorResponses
    @Auditable(value = AuditEventType.TRANSACTION_DELETE, resourceType = ResourceType.TRANSACTION, resourceId = "#id")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "Transaction ID") @PathVariable Long id) {
        transactionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
