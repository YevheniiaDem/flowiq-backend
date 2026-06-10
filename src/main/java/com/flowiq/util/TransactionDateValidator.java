package com.flowiq.util;

import com.flowiq.exception.BadRequestException;

import java.time.LocalDate;

public final class TransactionDateValidator {

    private static final LocalDate MIN_DATE = LocalDate.of(2000, 1, 1);

    private TransactionDateValidator() {
    }

    public static void validate(LocalDate transactionDate) {
        if (transactionDate == null) {
            throw new BadRequestException("Transaction date is required");
        }

        LocalDate maxDate = LocalDate.now();

        if (transactionDate.isBefore(MIN_DATE)) {
            throw new BadRequestException(
                    "Transaction date must not be before " + MIN_DATE);
        }

        if (transactionDate.isAfter(maxDate)) {
            throw new BadRequestException("Transaction date cannot be in the future");
        }
    }
}
