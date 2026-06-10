package com.flowiq.controller;

import com.flowiq.dto.response.ImportJobResponse;
import com.flowiq.dto.response.ImportListResponse;
import com.flowiq.service.ImportService;
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

@RestController
@RequestMapping("/api/imports")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportJobResponse> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(importService.upload(file));
    }

    @GetMapping
    public ResponseEntity<ImportListResponse> getImports() {
        return ResponseEntity.ok(importService.getImports());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImportJobResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(importService.getById(id));
    }
}
