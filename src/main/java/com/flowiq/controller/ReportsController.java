package com.flowiq.controller;

import com.flowiq.config.OpenApiConfig;
import com.flowiq.config.openapi.ApiErrorResponses;
import com.flowiq.dto.request.GenerateReportRequest;
import com.flowiq.dto.response.ReportJobResponse;
import com.flowiq.dto.response.ReportListResponse;
import com.flowiq.dto.response.ReportPreviewResponse;
import com.flowiq.service.ReportsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Reports", description = "Financial report generation, preview, and download")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ReportsController {

    private final ReportsService reportsService;

    @Operation(summary = "List reports", description = "Returns all generated reports for the authenticated user.")
    @ApiResponse(responseCode = "200", description = "Report list",
            content = @Content(schema = @Schema(implementation = ReportListResponse.class)))
    @ApiErrorResponses
    @GetMapping
    public ResponseEntity<ReportListResponse> getReports() {
        return ResponseEntity.ok(reportsService.getReports());
    }

    @Operation(
            summary = "Report preview",
            description = "Returns a preview of report data for a given period. Use periodPreset (THIS_MONTH, LAST_MONTH, QUARTER, YEAR) or custom dateFrom/dateTo."
    )
    @ApiResponse(responseCode = "200", description = "Report preview data",
            content = @Content(schema = @Schema(implementation = ReportPreviewResponse.class)))
    @ApiErrorResponses
    @GetMapping("/preview")
    public ResponseEntity<ReportPreviewResponse> getPreview(
            @Parameter(description = "Start date (ISO format)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @Parameter(description = "End date (ISO format)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @Parameter(description = "Period preset: THIS_MONTH, LAST_MONTH, QUARTER, YEAR") @RequestParam(required = false) String periodPreset
    ) {
        if (periodPreset != null && !periodPreset.isBlank()) {
            var range = resolvePreviewRange(periodPreset, dateFrom, dateTo);
            return ResponseEntity.ok(reportsService.getPreview(range.from(), range.to()));
        }
        return ResponseEntity.ok(reportsService.getPreview(dateFrom, dateTo));
    }

    @Operation(summary = "Generate report", description = "Generates a new financial report in PDF, CSV, or Excel format.")
    @ApiResponse(responseCode = "201", description = "Report generation started",
            content = @Content(schema = @Schema(implementation = ReportJobResponse.class)))
    @ApiErrorResponses
    @PostMapping("/generate")
    public ResponseEntity<ReportJobResponse> generate(@Valid @RequestBody GenerateReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportsService.generate(request));
    }

    @Operation(summary = "Get report by ID", description = "Returns metadata and status of a generated report.")
    @ApiResponse(responseCode = "200", description = "Report job details",
            content = @Content(schema = @Schema(implementation = ReportJobResponse.class)))
    @ApiErrorResponses
    @GetMapping("/{id}")
    public ResponseEntity<ReportJobResponse> getById(@Parameter(description = "Report job ID") @PathVariable Long id) {
        return ResponseEntity.ok(reportsService.getById(id));
    }

    @Operation(summary = "Download report", description = "Downloads the generated report file (PDF, CSV, or Excel).")
    @ApiResponse(responseCode = "200", description = "Report file download",
            content = @Content(mediaType = "application/octet-stream", schema = @Schema(type = "string", format = "binary")))
    @ApiErrorResponses
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@Parameter(description = "Report job ID") @PathVariable Long id) {
        Resource resource = reportsService.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(reportsService.getDownloadContentType(id)))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + reportsService.getDownloadFileName(id) + "\"")
                .body(resource);
    }

    private record DateRange(LocalDate from, LocalDate to) {}

    private DateRange resolvePreviewRange(String preset, LocalDate dateFrom, LocalDate dateTo) {
        java.time.YearMonth current = java.time.YearMonth.now();
        return switch (preset.toUpperCase()) {
            case "THIS_MONTH" -> new DateRange(current.atDay(1), current.atEndOfMonth());
            case "LAST_MONTH" -> {
                java.time.YearMonth last = current.minusMonths(1);
                yield new DateRange(last.atDay(1), last.atEndOfMonth());
            }
            case "QUARTER" -> {
                int quarter = (current.getMonthValue() - 1) / 3;
                java.time.YearMonth qStart = java.time.YearMonth.of(current.getYear(), quarter * 3 + 1);
                yield new DateRange(qStart.atDay(1), current.atEndOfMonth());
            }
            case "YEAR" -> new DateRange(
                    LocalDate.of(current.getYear(), 1, 1),
                    LocalDate.of(current.getYear(), 12, 31));
            default -> new DateRange(
                    dateFrom != null ? dateFrom : current.atDay(1),
                    dateTo != null ? dateTo : current.atEndOfMonth());
        };
    }
}
