package com.flowiq.controller;

import com.flowiq.dto.response.ImportJobResponse;
import com.flowiq.dto.response.ImportListResponse;
import com.flowiq.exception.ResourceNotFoundException;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.service.ImportService;
import com.flowiq.unit.support.ControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImportController tests")
class ImportControllerTest {

    @Mock
    private ImportService importService;

    @InjectMocks
    private ImportController importController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.standaloneMockMvc(importController);
    }

    @Test
    @DisplayName("POST /api/imports/upload creates import job")
    void upload_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "statement.csv", "text/csv", "date,amount\n2026-06-01,100".getBytes());

        when(importService.upload(any())).thenReturn(
                ImportJobResponse.builder().id(1L).fileName("statement.csv").status("COMPLETED").build());

        mockMvc.perform(multipart("/api/imports/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fileName").value("statement.csv"));
    }

    @Test
    @DisplayName("GET /api/imports returns import job list")
    void list_success() throws Exception {
        when(importService.getImports()).thenReturn(
                ImportListResponse.builder()
                        .jobs(List.of(ImportJobResponse.builder().id(1L).fileName("statement.csv").build()))
                        .build());

        mockMvc.perform(get("/api/imports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs[0].id").value(1));
    }

    @Test
    @DisplayName("GET /api/imports/{id} returns import job")
    void getById_success() throws Exception {
        when(importService.getById(1L)).thenReturn(
                ImportJobResponse.builder().id(1L).fileName("statement.csv").status("COMPLETED").build());

        mockMvc.perform(get("/api/imports/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("GET /api/imports/{id} returns 404 when not found")
    void getById_notFound() throws Exception {
        when(importService.getById(99L)).thenThrow(new ResourceNotFoundException("Import job not found"));

        mockMvc.perform(get("/api/imports/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/imports returns 401 when unauthorized")
    void list_unauthorized() throws Exception {
        doThrow(new UnauthorizedException("Not authenticated")).when(importService).getImports();

        mockMvc.perform(get("/api/imports"))
                .andExpect(status().isUnauthorized());
    }
}
