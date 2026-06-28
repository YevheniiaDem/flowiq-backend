package com.flowiq.controller;

import com.flowiq.dto.request.GenerateReportRequest;
import com.flowiq.dto.response.ReportJobResponse;
import com.flowiq.dto.response.ReportListResponse;
import com.flowiq.dto.response.ReportPreviewResponse;
import com.flowiq.entity.ReportJob;
import com.flowiq.exception.ResourceNotFoundException;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.service.ReportsService;
import com.flowiq.unit.support.ControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportsController tests")
class ReportsControllerTest {

    @Mock
    private ReportsService reportsService;

    @InjectMocks
    private ReportsController reportsController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.standaloneMockMvc(reportsController);
    }

    @Test
    @DisplayName("GET /api/reports returns report list")
    void list_success() throws Exception {
        when(reportsService.getReports()).thenReturn(
                ReportListResponse.builder()
                        .reports(List.of(ReportJobResponse.builder().id(1L).reportType("PROFIT_AND_LOSS").build()))
                        .build());

        mockMvc.perform(get("/api/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reports[0].id").value(1));
    }

    @Test
    @DisplayName("GET /api/reports/preview returns preview for custom date range")
    void preview_customDates_success() throws Exception {
        when(reportsService.getPreview(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30)))
                .thenReturn(ReportPreviewResponse.builder()
                        .revenue(new BigDecimal("10000"))
                        .expenses(new BigDecimal("4000"))
                        .profit(new BigDecimal("6000"))
                        .build());

        mockMvc.perform(get("/api/reports/preview")
                        .param("dateFrom", "2026-01-01")
                        .param("dateTo", "2026-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revenue").value(10000))
                .andExpect(jsonPath("$.profit").value(6000));
    }

    @Test
    @DisplayName("GET /api/reports/preview resolves periodPreset THIS_MONTH")
    void preview_periodPreset_success() throws Exception {
        when(reportsService.getPreview(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(ReportPreviewResponse.builder().revenue(new BigDecimal("5000")).build());

        mockMvc.perform(get("/api/reports/preview").param("periodPreset", "THIS_MONTH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revenue").value(5000));

        verify(reportsService).getPreview(any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("POST /api/reports/generate creates report job")
    void generate_success() throws Exception {
        GenerateReportRequest request = validGenerateRequest();

        when(reportsService.generate(any(GenerateReportRequest.class)))
                .thenReturn(ReportJobResponse.builder().id(1L).status("PENDING").build());

        mockMvc.perform(post("/api/reports/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestSupport.OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("POST /api/reports/generate rejects missing report type")
    void generate_validationError() throws Exception {
        GenerateReportRequest request = new GenerateReportRequest();
        request.setFormat(ReportJob.Format.PDF);

        mockMvc.perform(post("/api/reports/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestSupport.OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/reports/{id} returns report job")
    void getById_success() throws Exception {
        when(reportsService.getById(1L)).thenReturn(
                ReportJobResponse.builder().id(1L).reportType("PROFIT_AND_LOSS").status("COMPLETED").build());

        mockMvc.perform(get("/api/reports/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("GET /api/reports/{id} returns 404 when not found")
    void getById_notFound() throws Exception {
        when(reportsService.getById(99L)).thenThrow(new ResourceNotFoundException("Report not found"));

        mockMvc.perform(get("/api/reports/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/reports/{id}/download returns report file")
    void download_success() throws Exception {
        when(reportsService.download(1L)).thenReturn(new ByteArrayResource("report-data".getBytes()));
        when(reportsService.getDownloadContentType(1L)).thenReturn("application/pdf");
        when(reportsService.getDownloadFileName(1L)).thenReturn("report.pdf");

        mockMvc.perform(get("/api/reports/1/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"report.pdf\""));
    }

    @Test
    @DisplayName("GET /api/reports returns 401 when unauthorized")
    void list_unauthorized() throws Exception {
        doThrow(new UnauthorizedException("Not authenticated")).when(reportsService).getReports();

        mockMvc.perform(get("/api/reports"))
                .andExpect(status().isUnauthorized());
    }

    private GenerateReportRequest validGenerateRequest() {
        GenerateReportRequest request = new GenerateReportRequest();
        request.setReportType(ReportJob.ReportType.PROFIT_AND_LOSS);
        request.setFormat(ReportJob.Format.PDF);
        request.setPeriodPreset("THIS_MONTH");
        return request;
    }
}
