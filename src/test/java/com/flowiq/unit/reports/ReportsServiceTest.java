package com.flowiq.unit.reports;

import com.flowiq.dto.request.GenerateReportRequest;
import com.flowiq.dto.response.FopInsightsResponse;
import com.flowiq.dto.response.ReportJobResponse;
import com.flowiq.dto.response.ReportListResponse;
import com.flowiq.dto.response.ReportPreviewResponse;
import com.flowiq.entity.ReportJob;
import com.flowiq.entity.Transaction;
import com.flowiq.entity.User;
import com.flowiq.exception.BadRequestException;
import com.flowiq.exception.ResourceNotFoundException;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.notifications.service.NotificationGeneratorService;
import com.flowiq.repository.ReportJobRepository;
import com.flowiq.repository.TransactionRepository;
import com.flowiq.repository.UserRepository;
import com.flowiq.reports.ReportData;
import com.flowiq.reports.ReportFileGenerator;
import com.flowiq.service.AnalyticsService;
import com.flowiq.service.ReportsService;
import com.flowiq.service.TransactionSeedService;
import com.flowiq.tasks.service.TaskGeneratorService;
import com.flowiq.unit.support.SecurityTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ReportsService unit tests")
class ReportsServiceTest {

    private static final Long USER_ID = 99L;
    private static final String EMAIL = "reports@test.flowiq";

    @Mock
    private ReportJobRepository reportJobRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TransactionSeedService transactionSeedService;
    @Mock
    private AnalyticsService analyticsService;
    @Mock
    private ReportFileGenerator reportFileGenerator;
    @Mock
    private NotificationGeneratorService notificationGeneratorService;
    @Mock
    private TaskGeneratorService taskGeneratorService;

    @InjectMocks
    private ReportsService reportsService;

    private User user;

    @BeforeEach
    void setUp() {
        user = SecurityTestSupport.testUser(USER_ID, EMAIL);
        SecurityTestSupport.authenticate(user);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        stubTransactionSums(new BigDecimal("50000"), new BigDecimal("20000"));
        stubFopInsights();
    }

    @AfterEach
    void tearDown() {
        SecurityTestSupport.clearSecurityContext();
    }

    @Test
    @DisplayName("getReports returns user reports with dashboard stats")
    void getReports_happyPath() {
        ReportJob job = completedJob(1L);
        when(reportJobRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of(job));
        when(reportJobRepository.countByUserId(USER_ID)).thenReturn(1L);
        when(reportJobRepository.countByUserIdAndCreatedAtAfter(eq(USER_ID), any(LocalDateTime.class)))
                .thenReturn(1L);
        when(reportJobRepository.findFirstByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(Optional.of(job));
        when(reportJobRepository.findMostUsedReportType(USER_ID))
                .thenReturn(Optional.of(ReportJob.ReportType.PROFIT_AND_LOSS));

        ReportListResponse response = reportsService.getReports();

        assertThat(response.getReports()).hasSize(1);
        assertThat(response.getStats().getGeneratedReports()).isEqualTo(1L);
        assertThat(response.getStats().getMostUsedReportType()).isEqualTo("PROFIT_AND_LOSS");
    }

    @Test
    @DisplayName("getById returns report for authenticated user")
    void getById_happyPath() {
        ReportJob job = completedJob(5L);
        when(reportJobRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.of(job));

        ReportJobResponse response = reportsService.getById(5L);

        assertThat(response.getId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("getById throws when report not found")
    void getById_notFound() {
        when(reportJobRepository.findByIdAndUserId(999L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportsService.getById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getPreview builds revenue, expenses and profit for custom range")
    void getPreview_customRange() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);

        ReportPreviewResponse preview = reportsService.getPreview(from, to);

        assertThat(preview.getRevenue()).isEqualByComparingTo("50000");
        assertThat(preview.getExpenses()).isEqualByComparingTo("20000");
        assertThat(preview.getProfit()).isEqualByComparingTo("30000");
        verify(transactionSeedService).seedIfEmpty(user);
    }

    @Test
    @DisplayName("getPreview defaults to current month when dates are null")
    void getPreview_nullDates_defaultsToCurrentMonth() {
        ReportPreviewResponse preview = reportsService.getPreview(null, null);

        assertThat(preview).isNotNull();
        assertThat(preview.getChartData()).isNotEmpty();
    }

    @Test
    @DisplayName("getPreview throws when dateFrom is after dateTo")
    void getPreview_invalidRange() {
        assertThatThrownBy(() -> reportsService.getPreview(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 1, 1)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("dateFrom");
    }

    @Test
    @DisplayName("generate creates completed report and notifies user")
    void generate_happyPath() {
        GenerateReportRequest request = new GenerateReportRequest();
        request.setReportType(ReportJob.ReportType.PROFIT_AND_LOSS);
        request.setFormat(ReportJob.Format.PDF);
        request.setPeriodPreset("THIS_MONTH");

        ReportJob savedJob = completedJob(10L);
        savedJob.setStatus(ReportJob.Status.GENERATING);

        when(reportJobRepository.save(any(ReportJob.class))).thenAnswer(invocation -> {
            ReportJob job = invocation.getArgument(0);
            if (job.getId() == null) {
                job.setId(10L);
            }
            return job;
        });
        when(reportFileGenerator.generate(any(ReportData.class), eq(ReportJob.Format.PDF)))
                .thenReturn(new byte[]{1, 2, 3});
        when(reportFileGenerator.resolveFileName(any(ReportData.class), eq(ReportJob.Format.PDF)))
                .thenReturn("report.pdf");

        ReportJobResponse response = reportsService.generate(request);

        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        verify(notificationGeneratorService).notifyReportCompleted(eq(USER_ID), eq(10L), eq("report.pdf"), eq("PDF"));
        verify(taskGeneratorService).createReportReviewTask(eq(USER_ID), eq(10L), eq("report.pdf"));
    }

    @Test
    @DisplayName("generate marks job as failed when file generation throws")
    void generate_generationFailure() {
        GenerateReportRequest request = new GenerateReportRequest();
        request.setReportType(ReportJob.ReportType.CASH_FLOW);
        request.setFormat(ReportJob.Format.EXCEL);
        request.setDateFrom(LocalDate.of(2026, 1, 1));
        request.setDateTo(LocalDate.of(2026, 1, 31));

        when(reportJobRepository.save(any(ReportJob.class))).thenAnswer(invocation -> {
            ReportJob job = invocation.getArgument(0);
            job.setId(11L);
            return job;
        });
        when(reportFileGenerator.generate(any(), any()))
                .thenThrow(new RuntimeException("render error"));

        assertThatThrownBy(() -> reportsService.generate(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Failed to generate report");

        ArgumentCaptor<ReportJob> captor = ArgumentCaptor.forClass(ReportJob.class);
        verify(reportJobRepository, atLeast(2)).save(captor.capture());
        assertThat(captor.getAllValues().stream()
                .anyMatch(j -> j.getStatus() == ReportJob.Status.FAILED)).isTrue();
    }

    @Test
    @DisplayName("download returns file resource for completed report")
    void download_happyPath() {
        ReportJob job = completedJob(3L);
        job.setFileContent(new byte[]{10, 20});
        when(reportJobRepository.findByIdAndUserId(3L, USER_ID)).thenReturn(Optional.of(job));

        Resource resource = reportsService.download(3L);

        assertThat(resource).isInstanceOf(ByteArrayResource.class);
        assertThat(((ByteArrayResource) resource).getByteArray()).hasSize(2);
    }

    @Test
    @DisplayName("download throws when report is not completed")
    void download_notCompleted() {
        ReportJob job = completedJob(4L);
        job.setStatus(ReportJob.Status.GENERATING);
        job.setFileContent(null);
        when(reportJobRepository.findByIdAndUserId(4L, USER_ID)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> reportsService.download(4L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not available");
    }

    @Test
    @DisplayName("generate with YEAR preset uses full calendar year range")
    void generate_yearPreset() {
        GenerateReportRequest request = new GenerateReportRequest();
        request.setReportType(ReportJob.ReportType.TAX_SUMMARY);
        request.setFormat(ReportJob.Format.PDF);
        request.setPeriodPreset("YEAR");

        when(reportJobRepository.save(any(ReportJob.class))).thenAnswer(invocation -> {
            ReportJob job = invocation.getArgument(0);
            job.setId(12L);
            job.setStatus(ReportJob.Status.COMPLETED);
            return job;
        });
        when(reportFileGenerator.generate(any(), any())).thenReturn(new byte[]{1});
        when(reportFileGenerator.resolveFileName(any(), any())).thenReturn("tax.pdf");

        reportsService.generate(request);

        ArgumentCaptor<ReportJob> captor = ArgumentCaptor.forClass(ReportJob.class);
        verify(reportJobRepository, atLeastOnce()).save(captor.capture());
        ReportJob saved = captor.getAllValues().get(0);
        assertThat(saved.getPeriodFrom()).isEqualTo(LocalDate.of(LocalDate.now().getYear(), 1, 1));
        assertThat(saved.getPeriodTo()).isEqualTo(LocalDate.of(LocalDate.now().getYear(), 12, 31));
    }

    @Test
    @DisplayName("throws UnauthorizedException when not authenticated")
    void getReports_unauthenticated() {
        SecurityTestSupport.clearSecurityContext();

        assertThatThrownBy(() -> reportsService.getReports())
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("getDownloadContentType delegates to file generator")
    void getDownloadContentType_happyPath() {
        ReportJob job = completedJob(6L);
        when(reportJobRepository.findByIdAndUserId(6L, USER_ID)).thenReturn(Optional.of(job));
        when(reportFileGenerator.resolveContentType(ReportJob.Format.PDF)).thenReturn("application/pdf");

        assertThat(reportsService.getDownloadContentType(6L)).isEqualTo("application/pdf");
    }

    private ReportJob completedJob(Long id) {
        ReportJob job = new ReportJob();
        job.setId(id);
        job.setUserId(USER_ID);
        job.setReportType(ReportJob.ReportType.PROFIT_AND_LOSS);
        job.setFormat(ReportJob.Format.PDF);
        job.setStatus(ReportJob.Status.COMPLETED);
        job.setFileName("report.pdf");
        job.setFileSize(100L);
        job.setPeriodFrom(YearMonth.now().atDay(1));
        job.setPeriodTo(YearMonth.now().atEndOfMonth());
        job.setCreatedAt(LocalDateTime.now());
        job.setFileContent(new byte[]{1});
        return job;
    }

    private void stubTransactionSums(BigDecimal revenue, BigDecimal expenses) {
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                eq(USER_ID), eq(Transaction.Type.REVENUE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(revenue);
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                eq(USER_ID), eq(Transaction.Type.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(expenses);
        when(transactionRepository.sumRevenueByCategory(eq(USER_ID), any(), any()))
                .thenReturn(List.of());
        when(transactionRepository.sumExpensesByCategory(eq(USER_ID), any(), any()))
                .thenReturn(List.of());
    }

    private void stubFopInsights() {
        FopInsightsResponse fop = FopInsightsResponse.builder()
                .currentFopGroup("FOP Group 2")
                .fopGroupNumber(2)
                .annualIncome(new BigDecimal("300000"))
                .incomeLimit(new BigDecimal("5328000"))
                .incomeLimitUsagePercent(5.6)
                .estimatedTaxLoad(new BigDecimal("15000"))
                .taxForecast(new BigDecimal("180000"))
                .build();
        when(analyticsService.getFopInsights()).thenReturn(fop);
    }
}
