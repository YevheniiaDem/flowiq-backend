package com.flowiq.unit.service;

import com.flowiq.dto.request.CreateTransactionRequest;
import com.flowiq.dto.request.UpdateTransactionRequest;
import com.flowiq.entity.Transaction;
import com.flowiq.entity.User;
import com.flowiq.exception.BadRequestException;
import com.flowiq.exception.ResourceNotFoundException;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.repository.TransactionRepository;
import com.flowiq.repository.UserRepository;
import com.flowiq.service.TransactionService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TransactionService unit tests")
class TransactionServiceTest {

    private static final Long USER_ID = 1L;
    private static final String EMAIL = "tx@test.flowiq";

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User user;

    @BeforeEach
    void setUp() {
        user = SecurityTestSupport.testUser(USER_ID, EMAIL);
        SecurityTestSupport.authenticate(user);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    }

    @AfterEach
    void tearDown() {
        SecurityTestSupport.clearSecurityContext();
    }

    @Test
    @DisplayName("create persists valid expense transaction")
    void create_success() {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setType(CreateTransactionRequest.TransactionTypeDto.EXPENSE);
        request.setAmount(new BigDecimal("1500"));
        request.setCategory("Office");
        request.setTransactionDate(LocalDate.of(2026, 6, 1));

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            tx.setId(10L);
            return tx;
        });

        var response = transactionService.create(request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getCategory()).isEqualTo("Office");
    }

    @Test
    @DisplayName("create rejects invalid category for type")
    void create_invalidCategory() {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setType(CreateTransactionRequest.TransactionTypeDto.INCOME);
        request.setAmount(new BigDecimal("100"));
        request.setCategory("Office");
        request.setTransactionDate(LocalDate.of(2026, 6, 1));

        assertThatThrownBy(() -> transactionService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid category for transaction type");
    }

    @Test
    @DisplayName("getById returns owned transaction")
    void getById_success() {
        Transaction transaction = sampleTransaction(5L);
        when(transactionRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.of(transaction));

        var response = transactionService.getById(5L);

        assertThat(response.getId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("getById throws when transaction not found")
    void getById_notFound() {
        when(transactionRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("delete removes owned transaction")
    void delete_success() {
        Transaction transaction = sampleTransaction(7L);
        when(transactionRepository.findByIdAndUserId(7L, USER_ID)).thenReturn(Optional.of(transaction));

        transactionService.delete(7L);

        verify(transactionRepository).delete(transaction);
    }

    @Test
    @DisplayName("getTransactions returns paginated results")
    void getTransactions_success() {
        Transaction transaction = sampleTransaction(1L);
        Page<Transaction> page = new PageImpl<>(List.of(transaction));
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        var response = transactionService.getTransactions(null, 0, 10, null, null, null, null);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("update modifies existing transaction")
    void update_success() {
        Transaction existing = sampleTransaction(3L);
        when(transactionRepository.findByIdAndUserId(3L, USER_ID)).thenReturn(Optional.of(existing));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateTransactionRequest request = new UpdateTransactionRequest();
        request.setType(CreateTransactionRequest.TransactionTypeDto.INCOME);
        request.setAmount(new BigDecimal("5000"));
        request.setCategory("Services");
        request.setTransactionDate(LocalDate.of(2026, 6, 3));

        var response = transactionService.update(3L, request);

        assertThat(response.getCategory()).isEqualTo("Services");
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(Transaction.Type.REVENUE);
    }

    @Test
    @DisplayName("rejects unauthenticated access")
    void rejectsUnauthenticated() {
        SecurityTestSupport.clearSecurityContext();

        assertThatThrownBy(() -> transactionService.getById(1L))
                .isInstanceOf(UnauthorizedException.class);
    }

    private Transaction sampleTransaction(Long id) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setUser(user);
        transaction.setType(Transaction.Type.EXPENSE);
        transaction.setAmount(new BigDecimal("100"));
        transaction.setCategory("Office");
        transaction.setTransactionDate(LocalDate.of(2026, 6, 1));
        return transaction;
    }
}
