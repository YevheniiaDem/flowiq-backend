package com.flowiq.dto.request;

import com.flowiq.entity.ReportJob;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class GenerateReportRequest {

    @NotNull(message = "Report type is required")
    private ReportJob.ReportType reportType;

    @NotNull(message = "Format is required")
    private ReportJob.Format format;

    private String periodPreset;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateTo;
}
