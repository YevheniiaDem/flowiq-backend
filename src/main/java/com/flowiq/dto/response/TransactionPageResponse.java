package com.flowiq.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Paginated list of transactions")
public class TransactionPageResponse {

    private List<TransactionResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
