package com.flowiq.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TransactionPageResponse {

    private List<TransactionResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
