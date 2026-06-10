package com.flowiq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportStatsResponse {

    private long importedFiles;
    private long importedTransactions;
    private LocalDateTime lastImport;
    private double successRate;
}
