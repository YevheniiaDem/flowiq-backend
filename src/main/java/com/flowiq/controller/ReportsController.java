package com.flowiq.controller;

import com.flowiq.dto.request.GenerateReportRequest;
import com.flowiq.dto.response.ReportJobResponse;
import com.flowiq.dto.response.ReportListResponse;
import com.flowiq.dto.response.ReportPreviewResponse;
import com.flowiq.service.ReportsService;
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

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportsController {

    private final ReportsService reportsService;

    @GetMapping
    public ResponseEntity<ReportListResponse> getReports() {
        return ResponseEntity.ok(reportsService.getReports());
    }

    @GetMapping("/preview")
    public ResponseEntity<ReportPreviewResponse> getPreview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String periodPreset
    ) {
        if (periodPreset != null && !periodPreset.isBlank()) {
            var range = resolvePreviewRange(periodPreset, dateFrom, dateTo);
            return ResponseEntity.ok(reportsService.getPreview(range.from(), range.to()));
        }
        return ResponseEntity.ok(reportsService.getPreview(dateFrom, dateTo));
    }

    @PostMapping("/generate")
    public ResponseEntity<ReportJobResponse> generate(@Valid @RequestBody GenerateReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportsService.generate(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReportJobResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reportsService.getById(id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
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
