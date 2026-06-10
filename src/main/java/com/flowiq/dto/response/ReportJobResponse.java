package com.flowiq.dto.response;

import com.flowiq.entity.ReportJob;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportJobResponse {

    private Long id;
    private String reportType;
    private String format;
    private String status;
    private String fileName;
    private Long fileSize;
    private LocalDate periodFrom;
    private LocalDate periodTo;
    private LocalDateTime createdAt;

    public static ReportJobResponse fromEntity(ReportJob job) {
        return ReportJobResponse.builder()
                .id(job.getId())
                .reportType(job.getReportType().name())
                .format(job.getFormat().name())
                .status(job.getStatus().name())
                .fileName(job.getFileName())
                .fileSize(job.getFileSize())
                .periodFrom(job.getPeriodFrom())
                .periodTo(job.getPeriodTo())
                .createdAt(job.getCreatedAt())
                .build();
    }
}
