package com.flowiq.dto.request;

import com.flowiq.entity.ReportJob;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@Schema(description = "Request to generate a financial report")
public class GenerateReportRequest {

    @NotNull(message = "Report type is required")
    @Schema(description = "Type of report to generate", example = "PROFIT_AND_LOSS", requiredMode = Schema.RequiredMode.REQUIRED)
    private ReportJob.ReportType reportType;

    @NotNull(message = "Format is required")
    @Schema(description = "Output format", example = "PDF", requiredMode = Schema.RequiredMode.REQUIRED)
    private ReportJob.Format format;

    @Schema(description = "Period preset: THIS_MONTH, LAST_MONTH, QUARTER, YEAR", example = "THIS_MONTH")
    private String periodPreset;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "Custom period start date", example = "2026-01-01")
    private LocalDate dateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "Custom period end date", example = "2026-06-30")
    private LocalDate dateTo;
}
