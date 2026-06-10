package com.flowiq.service;

import com.flowiq.dto.request.GenerateReportRequest;
import com.flowiq.dto.response.MonthlyAmountResponse;
import com.flowiq.dto.response.ReportDashboardStatsResponse;
import com.flowiq.dto.response.ReportJobResponse;
import com.flowiq.dto.response.ReportListResponse;
import com.flowiq.dto.response.ReportPreviewResponse;
import com.flowiq.entity.ReportJob;
import com.flowiq.entity.Transaction;
import com.flowiq.entity.User;
import com.flowiq.exception.BadRequestException;
import com.flowiq.exception.ResourceNotFoundException;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.repository.ReportJobRepository;
import com.flowiq.repository.TransactionRepository;
import com.flowiq.repository.UserRepository;
import com.flowiq.reports.ReportData;
import com.flowiq.reports.ReportFileGenerator;
import com.flowiq.security.UserPrincipal;
import com.flowiq.notifications.service.NotificationGeneratorService;
import com.flowiq.dto.response.FopInsightsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportsService {

    private final ReportJobRepository reportJobRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final TransactionSeedService transactionSeedService;
    private final AnalyticsService analyticsService;
    private final ReportFileGenerator reportFileGenerator;
    private final NotificationGeneratorService notificationGeneratorService;

    @Transactional(readOnly = true)
    public ReportListResponse getReports() {
        User user = getCurrentUserEntity();
        List<ReportJobResponse> reports = reportJobRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(ReportJobResponse::fromEntity)
                .toList();

        YearMonth current = YearMonth.now();
        LocalDateTime monthStart = current.atDay(1).atStartOfDay();

        return ReportListResponse.builder()
                .reports(reports)
                .stats(ReportDashboardStatsResponse.builder()
                        .generatedReports(reportJobRepository.countByUserId(user.getId()))
                        .reportsThisMonth(reportJobRepository.countByUserIdAndCreatedAtAfter(user.getId(), monthStart))
                        .lastGeneratedAt(reportJobRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId())
                                .map(ReportJob::getCreatedAt)
                                .orElse(null))
                        .mostUsedReportType(reportJobRepository.findMostUsedReportType(user.getId())
                                .map(Enum::name)
                                .orElse(null))
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public ReportJobResponse getById(Long id) {
        User user = getCurrentUserEntity();
        ReportJob job = reportJobRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        return ReportJobResponse.fromEntity(job);
    }

    @Transactional(readOnly = true)
    public ReportPreviewResponse getPreview(LocalDate dateFrom, LocalDate dateTo) {
        User user = getCurrentUserEntity();
        transactionSeedService.seedIfEmpty(user);
        DateRange range = resolveRange(dateFrom, dateTo, null);
        return buildPreview(user.getId(), range);
    }

    @Transactional
    public ReportJobResponse generate(GenerateReportRequest request) {
        User user = getCurrentUserEntity();
        transactionSeedService.seedIfEmpty(user);

        DateRange range = resolveRange(request.getDateFrom(), request.getDateTo(), request.getPeriodPreset());
        ReportData data = buildReportData(user.getId(), request.getReportType(), range);

        ReportJob job = new ReportJob();
        job.setUserId(user.getId());
        job.setReportType(request.getReportType());
        job.setFormat(request.getFormat());
        job.setStatus(ReportJob.Status.GENERATING);
        job.setPeriodFrom(range.from());
        job.setPeriodTo(range.to());
        job.setFileName("pending");
        job.setFileSize(0L);
        job = reportJobRepository.save(job);

        try {
            byte[] content = reportFileGenerator.generate(data, request.getFormat());
            String fileName = reportFileGenerator.resolveFileName(data, request.getFormat());
            job.setFileName(fileName);
            job.setFileSize((long) content.length);
            job.setFileContent(content);
            job.setStatus(ReportJob.Status.COMPLETED);
        } catch (Exception e) {
            job.setStatus(ReportJob.Status.FAILED);
            reportJobRepository.save(job);
            throw new BadRequestException("Failed to generate report: " + e.getMessage());
        }

        ReportJob saved = reportJobRepository.save(job);
        if (saved.getStatus() == ReportJob.Status.COMPLETED) {
            notificationGeneratorService.notifyReportCompleted(user.getId(), saved.getId(), saved.getFileName());
        }

        return ReportJobResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Resource download(Long id) {
        User user = getCurrentUserEntity();
        ReportJob job = reportJobRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        if (job.getStatus() != ReportJob.Status.COMPLETED || job.getFileContent() == null) {
            throw new BadRequestException("Report file is not available");
        }

        return new ByteArrayResource(job.getFileContent());
    }

    @Transactional(readOnly = true)
    public String getDownloadContentType(Long id) {
        User user = getCurrentUserEntity();
        ReportJob job = reportJobRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        return reportFileGenerator.resolveContentType(job.getFormat());
    }

    @Transactional(readOnly = true)
    public String getDownloadFileName(Long id) {
        User user = getCurrentUserEntity();
        ReportJob job = reportJobRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        return job.getFileName();
    }

    private ReportPreviewResponse buildPreview(Long userId, DateRange range) {
        BigDecimal revenue = sumRange(userId, Transaction.Type.REVENUE, range.from(), range.to());
        BigDecimal expenses = sumRange(userId, Transaction.Type.EXPENSE, range.from(), range.to());
        BigDecimal profit = revenue.subtract(expenses);
        FopInsightsResponse fop = analyticsService.getFopInsights();

        List<MonthlyAmountResponse> chartData = buildMonthlyChart(userId, range);

        return ReportPreviewResponse.builder()
                .revenue(revenue)
                .expenses(expenses)
                .profit(profit)
                .taxBurden(fop.getEstimatedTaxLoad())
                .chartData(chartData)
                .build();
    }

    private ReportData buildReportData(Long userId, ReportJob.ReportType type, DateRange range) {
        BigDecimal revenue = sumRange(userId, Transaction.Type.REVENUE, range.from(), range.to());
        BigDecimal expenses = sumRange(userId, Transaction.Type.EXPENSE, range.from(), range.to());
        BigDecimal profit = revenue.subtract(expenses);
        FopInsightsResponse fop = analyticsService.getFopInsights();

        List<ReportData.CategoryLine> revenueCategories = transactionRepository
                .sumRevenueByCategory(userId, range.from(), range.to()).stream()
                .map(r -> ReportData.CategoryLine.builder()
                        .category(r.getCategory())
                        .amount(r.getAmount())
                        .build())
                .toList();

        List<ReportData.CategoryLine> expenseCategories = transactionRepository
                .sumExpensesByCategory(userId, range.from(), range.to()).stream()
                .map(r -> ReportData.CategoryLine.builder()
                        .category(r.getCategory())
                        .amount(r.getAmount())
                        .build())
                .toList();

        List<ReportData.MonthlyLine> monthlyLines = buildMonthlyLines(userId, range);

        ReportData.ReportDataBuilder builder = ReportData.builder()
                .reportType(type)
                .title(resolveTitle(type))
                .periodFrom(range.from())
                .periodTo(range.to())
                .revenue(revenue)
                .expenses(expenses)
                .profit(profit)
                .taxBurden(fop.getEstimatedTaxLoad())
                .monthlyLines(monthlyLines);

        return switch (type) {
            case REVENUE_SUMMARY -> builder.revenueCategories(revenueCategories).build();
            case EXPENSE_SUMMARY -> builder.expenseCategories(expenseCategories).build();
            case CASH_FLOW -> builder.monthlyLines(monthlyLines).build();
            case TAX_SUMMARY, FOP_SUMMARY -> builder
                    .fopGroup(fop.getCurrentFopGroup())
                    .incomeLimitUsagePercent(fop.getIncomeLimitUsagePercent())
                    .estimatedTax(fop.getEstimatedTaxLoad())
                    .taxForecast(fop.getTaxForecast())
                    .annualIncome(fop.getAnnualIncome())
                    .incomeLimit(fop.getIncomeLimit())
                    .expenseCategories(expenseCategories)
                    .build();
            case PROFIT_AND_LOSS -> builder
                    .revenueCategories(revenueCategories)
                    .expenseCategories(expenseCategories)
                    .build();
        };
    }

    private List<ReportData.MonthlyLine> buildMonthlyLines(Long userId, DateRange range) {
        List<ReportData.MonthlyLine> lines = new ArrayList<>();
        YearMonth start = YearMonth.from(range.from());
        YearMonth end = YearMonth.from(range.to());
        YearMonth cursor = start;

        while (!cursor.isAfter(end)) {
            LocalDate from = cursor.atDay(1);
            LocalDate to = cursor.atEndOfMonth();
            if (from.isBefore(range.from())) {
                from = range.from();
            }
            if (to.isAfter(range.to())) {
                to = range.to();
            }
            BigDecimal rev = sumRange(userId, Transaction.Type.REVENUE, from, to);
            BigDecimal exp = sumRange(userId, Transaction.Type.EXPENSE, from, to);
            lines.add(ReportData.MonthlyLine.builder()
                    .month(cursor.toString())
                    .revenue(rev)
                    .expenses(exp)
                    .profit(rev.subtract(exp))
                    .build());
            cursor = cursor.plusMonths(1);
        }
        return lines;
    }

    private List<MonthlyAmountResponse> buildMonthlyChart(Long userId, DateRange range) {
        return buildMonthlyLines(userId, range).stream()
                .map(m -> MonthlyAmountResponse.builder()
                        .month(m.getMonth())
                        .amount(m.getRevenue())
                        .build())
                .toList();
    }

    private String resolveTitle(ReportJob.ReportType type) {
        return switch (type) {
            case PROFIT_AND_LOSS -> "Profit & Loss Report";
            case CASH_FLOW -> "Cash Flow Report";
            case REVENUE_SUMMARY -> "Revenue Summary Report";
            case EXPENSE_SUMMARY -> "Expense Summary Report";
            case TAX_SUMMARY -> "Tax Summary Report";
            case FOP_SUMMARY -> "FOP Summary Report";
        };
    }

    private DateRange resolveRange(LocalDate dateFrom, LocalDate dateTo, String preset) {
        if (preset != null && !preset.isBlank()) {
            YearMonth current = YearMonth.now();
            return switch (preset.toUpperCase()) {
                case "THIS_MONTH" -> new DateRange(current.atDay(1), current.atEndOfMonth());
                case "LAST_MONTH" -> {
                    YearMonth last = current.minusMonths(1);
                    yield new DateRange(last.atDay(1), last.atEndOfMonth());
                }
                case "QUARTER" -> {
                    int quarter = (current.getMonthValue() - 1) / 3;
                    YearMonth qStart = YearMonth.of(current.getYear(), quarter * 3 + 1);
                    yield new DateRange(qStart.atDay(1), current.atEndOfMonth());
                }
                case "YEAR" -> new DateRange(
                        LocalDate.of(current.getYear(), 1, 1),
                        LocalDate.of(current.getYear(), 12, 31));
                default -> resolveCustom(dateFrom, dateTo);
            };
        }
        return resolveCustom(dateFrom, dateTo);
    }

    private DateRange resolveCustom(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom == null || dateTo == null) {
            YearMonth current = YearMonth.now();
            return new DateRange(current.atDay(1), current.atEndOfMonth());
        }
        if (dateFrom.isAfter(dateTo)) {
            throw new BadRequestException("dateFrom must be before dateTo");
        }
        return new DateRange(dateFrom, dateTo);
    }

    private User getCurrentUserEntity() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("Not authenticated");
        }
        return userRepository.findByEmail(principal.getEmail())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }

    private BigDecimal sumRange(Long userId, Transaction.Type type, LocalDate from, LocalDate to) {
        return transactionRepository.sumByUserAndTypeAndDateRange(userId, type, from, to);
    }

    private record DateRange(LocalDate from, LocalDate to) {}
}
