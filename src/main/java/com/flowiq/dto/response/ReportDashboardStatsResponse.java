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
@Schema(description = "Report dashboard statistics")
public class ReportDashboardStatsResponse {

    private long generatedReports;
    private long reportsThisMonth;
    private LocalDateTime lastGeneratedAt;
    private String mostUsedReportType;
}
