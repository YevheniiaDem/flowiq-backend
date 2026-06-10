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
public class ReportDashboardStatsResponse {

    private long generatedReports;
    private long reportsThisMonth;
    private LocalDateTime lastGeneratedAt;
    private String mostUsedReportType;
}
