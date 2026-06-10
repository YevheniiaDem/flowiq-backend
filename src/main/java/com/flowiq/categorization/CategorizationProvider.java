package com.flowiq.categorization;

import com.flowiq.entity.Transaction;

import java.util.Optional;

/**
 * Extension point for categorization backends.
 * Rules engine is built into {@link CategorizationEngine}; future AI providers
 * can implement this interface and be injected as Spring beans.
 */
public interface CategorizationProvider {

    Optional<CategorizationResult> categorize(
            String description,
            Transaction.Type hintType,
            String hintCategory
    );
}
