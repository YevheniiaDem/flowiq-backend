package com.flowiq.unit.service;

import com.flowiq.entity.Transaction;
import com.flowiq.entity.User;
import com.flowiq.repository.TransactionRepository;
import com.flowiq.service.TransactionSeedService;
import com.flowiq.unit.support.SecurityTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TransactionSeedService unit tests")
class TransactionSeedServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionSeedService transactionSeedService;

    @Test
    @DisplayName("seedIfEmpty creates six months of transactions when user has none")
    void seedIfEmpty_createsTransactions() {
        User user = SecurityTestSupport.testUser(USER_ID, "seed@test.flowiq");
        when(transactionRepository.existsByUserId(USER_ID)).thenReturn(false);

        transactionSeedService.seedIfEmpty(user);

        ArgumentCaptor<List<Transaction>> captor = ArgumentCaptor.forClass(List.class);
        verify(transactionRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).isNotEmpty();
        assertThat(captor.getValue().stream().map(Transaction::getUser).distinct())
                .containsExactly(user);
    }

    @Test
    @DisplayName("seedIfEmpty delegates to history backfill when user already has transactions")
    void seedIfEmpty_existingUser() {
        User user = SecurityTestSupport.testUser(USER_ID, "seed@test.flowiq");
        when(transactionRepository.existsByUserId(USER_ID)).thenReturn(true);
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                anyLong(), eq(Transaction.Type.REVENUE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("1000"));

        transactionSeedService.seedIfEmpty(user);

        verify(transactionRepository).existsByUserId(USER_ID);
        verify(transactionRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("ensureSixMonthHistory backfills months with zero revenue")
    void ensureSixMonthHistory_backfillsMissingMonths() {
        User user = SecurityTestSupport.testUser(USER_ID, "seed@test.flowiq");
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                eq(USER_ID), eq(Transaction.Type.REVENUE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(BigDecimal.ZERO);

        transactionSeedService.ensureSixMonthHistory(user);

        verify(transactionRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("ensureSixMonthHistory skips save when all months have revenue")
    void ensureSixMonthHistory_noGap() {
        User user = SecurityTestSupport.testUser(USER_ID, "seed@test.flowiq");
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                anyLong(), eq(Transaction.Type.REVENUE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("1000"));

        transactionSeedService.ensureSixMonthHistory(user);

        verify(transactionRepository, never()).saveAll(anyList());
    }

    private void stubZeroMonthlyRevenue() {
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                anyLong(), eq(Transaction.Type.REVENUE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(BigDecimal.ZERO);
    }
}
