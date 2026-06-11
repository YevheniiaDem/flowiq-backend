package com.flowiq.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Import job statistics")
public class ImportStatsResponse {

    private long importedFiles;
    private long importedTransactions;
    private LocalDateTime lastImport;
    private double successRate;
}
