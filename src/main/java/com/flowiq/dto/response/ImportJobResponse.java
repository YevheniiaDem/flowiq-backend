package com.flowiq.dto.response;

import com.flowiq.entity.ImportJob;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportJobResponse {

    private Long id;
    private String fileName;
    private Long fileSize;
    private String status;
    private int rowsProcessed;
    private int rowsImported;
    private int errorsCount;
    private String bankFormat;
    private LocalDateTime createdAt;

    public static ImportJobResponse fromEntity(ImportJob job) {
        return ImportJobResponse.builder()
                .id(job.getId())
                .fileName(job.getFileName())
                .fileSize(job.getFileSize())
                .status(job.getStatus().name())
                .rowsProcessed(job.getRowsProcessed())
                .rowsImported(job.getRowsImported())
                .errorsCount(job.getErrorsCount())
                .bankFormat(job.getBankFormat())
                .createdAt(job.getCreatedAt())
                .build();
    }
}
