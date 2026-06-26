package com.flowiq.service;

import com.flowiq.categorization.CategorizationEngine;
import com.flowiq.categorization.CategorizationResult;
import com.flowiq.dto.response.ImportJobResponse;
import com.flowiq.dto.response.ImportListResponse;
import com.flowiq.dto.response.ImportStatsResponse;
import com.flowiq.entity.ImportJob;
import com.flowiq.entity.Transaction;
import com.flowiq.entity.User;
import com.flowiq.exception.BadRequestException;
import com.flowiq.exception.ResourceNotFoundException;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.importcsv.CsvImportStrategy;
import com.flowiq.importcsv.CsvImportStrategyResolver;
import com.flowiq.importcsv.CsvParseException;
import com.flowiq.importcsv.ParsedTransactionRow;
import com.flowiq.repository.ImportJobRepository;
import com.flowiq.repository.TransactionRepository;
import com.flowiq.repository.UserRepository;
import com.flowiq.security.UserPrincipal;
import com.flowiq.notifications.service.NotificationGeneratorService;
import com.flowiq.tasks.service.TaskGeneratorService;
import com.flowiq.util.TransactionDateValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImportService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private final ImportJobRepository importJobRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CsvImportStrategyResolver strategyResolver;
    private final CategorizationEngine categorizationEngine;
    private final NotificationGeneratorService notificationGeneratorService;
    private final TaskGeneratorService taskGeneratorService;

    @Transactional(readOnly = true)
    public ImportListResponse getImports() {
        User user = getCurrentUserEntity();
        List<ImportJobResponse> jobs = importJobRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(ImportJobResponse::fromEntity)
                .toList();

        return ImportListResponse.builder()
                .jobs(jobs)
                .stats(buildStats(user.getId()))
                .build();
    }

    @Transactional(readOnly = true)
    public ImportJobResponse getById(Long id) {
        User user = getCurrentUserEntity();
        ImportJob job = importJobRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Import job not found"));
        return ImportJobResponse.fromEntity(job);
    }

    @Transactional
    public ImportJobResponse upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".csv")) {
            throw new BadRequestException("Only CSV files are supported");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size must not exceed 10 MB");
        }

        User user = getCurrentUserEntity();

        ImportJob job = new ImportJob();
        job.setUserId(user.getId());
        job.setFileName(originalName);
        job.setFileSize(file.getSize());
        job.setStatus(ImportJob.Status.PROCESSING);
        job.setRowsProcessed(0);
        job.setRowsImported(0);
        job.setErrorsCount(0);
        job = importJobRepository.save(job);
        notificationGeneratorService.notifyImportProcessing(user.getId(), job.getId(), file.getOriginalFilename());

        try {
            String csvContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            processImport(user, job, csvContent);
        } catch (IOException e) {
            job.setStatus(ImportJob.Status.FAILED);
            importJobRepository.save(job);
            throw new BadRequestException("Failed to read file");
        } catch (CsvParseException e) {
            job.setStatus(ImportJob.Status.FAILED);
            importJobRepository.save(job);
            throw new BadRequestException(e.getMessage());
        }

        return ImportJobResponse.fromEntity(importJobRepository.save(job));
    }

    private void processImport(User user, ImportJob job, String csvContent) {
        CsvImportStrategy strategy = strategyResolver.resolve(csvContent);
        job.setBankFormat(strategy.getBankName());

        int dataRows = Math.max(csvContent.split("\\r?\\n").length - 1, 0);
        job.setRowsProcessed(dataRows);

        List<ParsedTransactionRow> parsedRows = strategy.parse(csvContent);
        int imported = 0;

        List<Transaction> toSave = new ArrayList<>();
        for (ParsedTransactionRow row : parsedRows) {
            try {
                TransactionDateValidator.validate(row.getTransactionDate());

                if (transactionRepository.existsDuplicate(
                        user.getId(),
                        row.getTransactionDate(),
                        row.getAmount(),
                        row.getType(),
                        row.getDescription()
                )) {
                    continue;
                }

                CategorizationResult categorization = categorizationEngine.categorize(
                        row.getDescription(),
                        row.getType(),
                        row.getCategory()
                );

                Transaction transaction = new Transaction();
                transaction.setUser(user);
                transaction.setType(categorization.getType());
                transaction.setAmount(row.getAmount());
                transaction.setCategory(categorization.getCategory());
                transaction.setDescription(row.getDescription());
                transaction.setTransactionDate(row.getTransactionDate());
                transaction.setAutoCategorized(categorization.isAutoCategorized());
                toSave.add(transaction);
                imported++;
            } catch (Exception e) {
                // skip invalid row
            }
        }

        if (!toSave.isEmpty()) {
            transactionRepository.saveAll(toSave);
        }

        job.setRowsImported(imported);
        job.setErrorsCount(Math.max(0, dataRows - imported));

        int errors = job.getErrorsCount();
        if (imported == 0 && errors > 0) {
            job.setStatus(ImportJob.Status.FAILED);
        } else if (errors > 0) {
            job.setStatus(ImportJob.Status.PARTIAL);
        } else {
            job.setStatus(ImportJob.Status.COMPLETED);
        }

        if (imported > 0) {
            notificationGeneratorService.notifyImportCompleted(user.getId(), job.getId(), imported);
            taskGeneratorService.createImportReviewTask(user.getId(), job.getId(), imported);
        } else if (job.getStatus() == ImportJob.Status.FAILED) {
            notificationGeneratorService.notifyImportFailed(user.getId(), job.getId(), job.getFileName());
        } else if (job.getStatus() == ImportJob.Status.PARTIAL) {
            notificationGeneratorService.notifyImportPartial(user.getId(), job.getId(), imported, errors);
        }
    }

    private ImportStatsResponse buildStats(Long userId) {
        long totalFiles = importJobRepository.countByUserId(userId);
        long totalTransactions = importJobRepository.sumRowsImportedByUserId(userId);
        long successful = importJobRepository.countSuccessfulByUserId(userId);
        double successRate = totalFiles == 0 ? 0.0 : (successful * 100.0) / totalFiles;

        return ImportStatsResponse.builder()
                .importedFiles(totalFiles)
                .importedTransactions(totalTransactions)
                .lastImport(importJobRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                        .map(ImportJob::getCreatedAt)
                        .orElse(null))
                .successRate(Math.round(successRate * 10.0) / 10.0)
                .build();
    }

    private User getCurrentUserEntity() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("Not authenticated");
        }
        return userRepository.findByEmail(principal.getEmail())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }
}
