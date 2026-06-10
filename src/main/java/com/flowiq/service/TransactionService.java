package com.flowiq.service;

import com.flowiq.dto.request.CreateTransactionRequest;
import com.flowiq.dto.request.UpdateTransactionRequest;
import com.flowiq.dto.response.TransactionPageResponse;
import com.flowiq.dto.response.TransactionResponse;
import com.flowiq.dto.response.TransactionSummaryResponse;
import com.flowiq.entity.Transaction;
import com.flowiq.entity.User;
import com.flowiq.exception.BadRequestException;
import com.flowiq.exception.ResourceNotFoundException;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.repository.TransactionRepository;
import com.flowiq.repository.UserRepository;
import com.flowiq.security.UserPrincipal;
import com.flowiq.util.TransactionDateValidator;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final Set<String> INCOME_CATEGORIES = Set.of(
            "Services", "Consulting", "Software", "Sales", "Subscription", "Other"
    );
    private static final Set<String> EXPENSE_CATEGORIES = Set.of(
            "Marketing", "Salary", "Infrastructure", "Equipment", "Office", "Taxes", "Software", "Other"
    );
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "transactionDate", "amount", "category", "type", "createdAt"
    );

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public TransactionPageResponse getTransactions(
            String search,
            int page,
            int size,
            String sort,
            CreateTransactionRequest.TransactionTypeDto type,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        User user = getCurrentUserEntity();
        Pageable pageable = buildPageable(page, size, sort);
        Specification<Transaction> spec = buildSpecification(user.getId(), search, type, dateFrom, dateTo);

        Page<Transaction> result = transactionRepository.findAll(spec, pageable);

        return TransactionPageResponse.builder()
                .content(result.getContent().stream().map(this::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(Long id) {
        User user = getCurrentUserEntity();
        Transaction transaction = findOwnedTransaction(id, user.getId());
        return toResponse(transaction);
    }

    @Transactional
    public TransactionResponse create(CreateTransactionRequest request) {
        User user = getCurrentUserEntity();
        validateCategory(request.getType(), request.getCategory());
        TransactionDateValidator.validate(request.getTransactionDate());

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setType(toEntityType(request.getType()));
        transaction.setAmount(request.getAmount());
        transaction.setCategory(request.getCategory().trim());
        transaction.setDescription(trimToNull(request.getDescription()));
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setAutoCategorized(false);

        return toResponse(transactionRepository.save(transaction));
    }

    @Transactional
    public TransactionResponse update(Long id, UpdateTransactionRequest request) {
        User user = getCurrentUserEntity();
        validateCategory(request.getType(), request.getCategory());
        TransactionDateValidator.validate(request.getTransactionDate());

        Transaction transaction = findOwnedTransaction(id, user.getId());
        transaction.setType(toEntityType(request.getType()));
        transaction.setAmount(request.getAmount());
        transaction.setCategory(request.getCategory().trim());
        transaction.setDescription(trimToNull(request.getDescription()));
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setAutoCategorized(false);

        return toResponse(transactionRepository.save(transaction));
    }

    @Transactional
    public void delete(Long id) {
        User user = getCurrentUserEntity();
        Transaction transaction = findOwnedTransaction(id, user.getId());
        transactionRepository.delete(transaction);
    }

    @Transactional(readOnly = true)
    public TransactionSummaryResponse getSummary(
            LocalDate dateFrom,
            LocalDate dateTo,
            CreateTransactionRequest.TransactionTypeDto type
    ) {
        User user = getCurrentUserEntity();
        Specification<Transaction> spec = buildSpecification(user.getId(), null, type, dateFrom, dateTo);

        List<Transaction> transactions = transactionRepository.findAll(spec);

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (Transaction transaction : transactions) {
            if (transaction.getType() == Transaction.Type.REVENUE) {
                totalRevenue = totalRevenue.add(transaction.getAmount());
            } else {
                totalExpenses = totalExpenses.add(transaction.getAmount());
            }
        }

        return TransactionSummaryResponse.builder()
                .totalRevenue(totalRevenue)
                .totalExpenses(totalExpenses)
                .netProfit(totalRevenue.subtract(totalExpenses))
                .transactionCount(transactions.size())
                .build();
    }

    private Specification<Transaction> buildSpecification(
            Long userId,
            String search,
            CreateTransactionRequest.TransactionTypeDto type,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("id"), userId));

            if (type != null) {
                predicates.add(cb.equal(root.get("type"), toEntityType(type)));
            }

            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), dateFrom));
            }

            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("transactionDate"), dateTo));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("category")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Pageable buildPageable(int page, int size, String sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        if (sort == null || sort.isBlank()) {
            return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "transactionDate"));
        }

        String[] parts = sort.split(",");
        String field = parts[0].trim();
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            field = "transactionDate";
        }

        return PageRequest.of(safePage, safeSize, Sort.by(direction, field));
    }

    private void validateCategory(CreateTransactionRequest.TransactionTypeDto type, String category) {
        if (category == null || category.isBlank()) {
            throw new BadRequestException("Category is required");
        }

        Set<String> allowed = type == CreateTransactionRequest.TransactionTypeDto.INCOME
                ? INCOME_CATEGORIES
                : EXPENSE_CATEGORIES;

        if (!allowed.contains(category.trim())) {
            throw new BadRequestException("Invalid category for transaction type");
        }
    }

    private Transaction findOwnedTransaction(Long id, Long userId) {
        return transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
    }

    private User getCurrentUserEntity() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("Not authenticated");
        }
        return userRepository.findByEmail(principal.getEmail())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }

    private Transaction.Type toEntityType(CreateTransactionRequest.TransactionTypeDto type) {
        return type == CreateTransactionRequest.TransactionTypeDto.INCOME
                ? Transaction.Type.REVENUE
                : Transaction.Type.EXPENSE;
    }

    private CreateTransactionRequest.TransactionTypeDto toDtoType(Transaction.Type type) {
        return type == Transaction.Type.REVENUE
                ? CreateTransactionRequest.TransactionTypeDto.INCOME
                : CreateTransactionRequest.TransactionTypeDto.EXPENSE;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .type(toDtoType(transaction.getType()))
                .amount(transaction.getAmount())
                .category(transaction.getCategory())
                .description(transaction.getDescription())
                .transactionDate(transaction.getTransactionDate())
                .autoCategorized(transaction.isAutoCategorized())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }
}
