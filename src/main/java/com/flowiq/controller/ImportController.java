package com.flowiq.controller;

import com.flowiq.config.OpenApiConfig;
import com.flowiq.config.openapi.ApiErrorResponses;
import com.flowiq.dto.response.ImportJobResponse;
import com.flowiq.dto.response.ImportListResponse;
import com.flowiq.service.ImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Imports", description = "Bank statement CSV import operations")
@RestController
@RequestMapping("/api/imports")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ImportController {

    private final ImportService importService;

    @Operation(
            summary = "Upload CSV file",
            description = "Uploads a bank statement CSV file for parsing and transaction import. Supports Monobank, PrivatBank, and universal formats."
    )
    @ApiResponse(responseCode = "201", description = "Import job created",
            content = @Content(schema = @Schema(implementation = ImportJobResponse.class)))
    @ApiErrorResponses
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportJobResponse> upload(
            @Parameter(description = "Bank statement CSV file", required = true)
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(importService.upload(file));
    }

    @Operation(summary = "List import jobs", description = "Returns all CSV import jobs for the authenticated user.")
    @ApiResponse(responseCode = "200", description = "Import job list",
            content = @Content(schema = @Schema(implementation = ImportListResponse.class)))
    @ApiErrorResponses
    @GetMapping
    public ResponseEntity<ImportListResponse> getImports() {
        return ResponseEntity.ok(importService.getImports());
    }

    @Operation(summary = "Get import job by ID", description = "Returns details and status of a specific import job.")
    @ApiResponse(responseCode = "200", description = "Import job details",
            content = @Content(schema = @Schema(implementation = ImportJobResponse.class)))
    @ApiErrorResponses
    @GetMapping("/{id}")
    public ResponseEntity<ImportJobResponse> getById(@Parameter(description = "Import job ID") @PathVariable Long id) {
        return ResponseEntity.ok(importService.getById(id));
    }
}
