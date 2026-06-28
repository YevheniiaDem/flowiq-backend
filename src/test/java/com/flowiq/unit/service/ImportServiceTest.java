package com.flowiq.unit.service;

import com.flowiq.categorization.CategorizationResult;
import com.flowiq.categorization.CategorizationEngine;
import com.flowiq.dto.response.ImportJobResponse;
import com.flowiq.entity.ImportJob;
import com.flowiq.entity.Transaction;
import com.flowiq.entity.User;
import com.flowiq.exception.BadRequestException;
import com.flowiq.exception.ResourceNotFoundException;
import com.flowiq.importcsv.CsvImportStrategy;
import com.flowiq.importcsv.CsvImportStrategyResolver;
import com.flowiq.importcsv.ParsedTransactionRow;
import com.flowiq.importcsv.UniversalCsvStrategy;
import com.flowiq.notifications.service.NotificationGeneratorService;
import com.flowiq.repository.ImportJobRepository;
import com.flowiq.repository.TransactionRepository;
import com.flowiq.repository.UserRepository;
import com.flowiq.service.ImportService;
import com.flowiq.tasks.service.TaskGeneratorService;
import com.flowiq.unit.support.SecurityTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ImportService unit tests")
class ImportServiceTest {

    private static final Long USER_ID = 1L;
    private static final String EMAIL = "import@test.flowiq";

    @Mock
    private ImportJobRepository importJobRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CsvImportStrategyResolver strategyResolver;
    @Mock
    private CategorizationEngine categorizationEngine;
    @Mock
    private NotificationGeneratorService notificationGeneratorService;
    @Mock
    private TaskGeneratorService taskGeneratorService;

    @InjectMocks
    private ImportService importService;

    private User user;
    private CsvImportStrategy strategy;

    @BeforeEach
    void setUp() {
        user = SecurityTestSupport.testUser(USER_ID, EMAIL);
        SecurityTestSupport.authenticate(user);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        strategy = new UniversalCsvStrategy();
    }

    @AfterEach
    void tearDown() {
        SecurityTestSupport.clearSecurityContext();
    }

    @Test
    @DisplayName("upload rejects empty file")
    void upload_rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "data.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> importService.upload(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("File is required");
    }

    @Test
    @DisplayName("upload rejects non-CSV extension")
    void upload_rejectsNonCsv() {
        MockMultipartFile file = new MockMultipartFile("file", "data.txt", "text/plain", "x".getBytes());

        assertThatThrownBy(() -> importService.upload(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Only CSV files are supported");
    }

    @Test
    @DisplayName("upload rejects file larger than 10 MB")
    void upload_rejectsLargeFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.csv", "text/csv", new byte[(int) (10 * 1024 * 1024 + 1)]);

        assertThatThrownBy(() -> importService.upload(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("File size must not exceed 10 MB");
    }

    @Test
    @DisplayName("upload imports valid CSV and marks job completed")
    void upload_success() {
        String csv = """
                date,type,category,amount
                2026-06-01,EXPENSE,Office,1500
                """;
        MockMultipartFile file = new MockMultipartFile("file", "bank.csv", "text/csv", csv.getBytes());

        when(importJobRepository.save(any(ImportJob.class))).thenAnswer(inv -> {
            ImportJob job = inv.getArgument(0);
            if (job.getId() == null) {
                job.setId(5L);
            }
            return job;
        });
        when(strategyResolver.resolve(csv)).thenReturn(strategy);
        when(transactionRepository.existsDuplicate(anyLong(), any(), any(), any(), any())).thenReturn(false);
        when(categorizationEngine.categorize(any(), any(), any()))
                .thenReturn(CategorizationResult.fromRule(Transaction.Type.EXPENSE, "Office"));

        ImportJobResponse response = importService.upload(file);

        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        verify(transactionRepository).saveAll(anyList());
        verify(notificationGeneratorService).notifyImportCompleted(USER_ID, 5L, 1);
    }

    @Test
    @DisplayName("getById throws when import job not found")
    void getById_notFound() {
        when(importJobRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> importService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("upload skips duplicate rows")
    void upload_skipsDuplicates() {
        String csv = """
                date,type,category,amount
                2026-06-01,EXPENSE,Office,1500
                """;
        MockMultipartFile file = new MockMultipartFile("file", "bank.csv", "text/csv", csv.getBytes());

        when(importJobRepository.save(any(ImportJob.class))).thenAnswer(inv -> {
            ImportJob job = inv.getArgument(0);
            job.setId(6L);
            return job;
        });
        when(strategyResolver.resolve(csv)).thenReturn(strategy);
        when(transactionRepository.existsDuplicate(
                eq(USER_ID), eq(LocalDate.of(2026, 6, 1)), eq(new BigDecimal("1500")),
                eq(Transaction.Type.EXPENSE), eq(null)))
                .thenReturn(true);

        ImportJobResponse response = importService.upload(file);

        assertThat(response.getStatus()).isEqualTo("FAILED");
        verify(transactionRepository, never()).saveAll(anyList());
    }
}
