package com.flowiq.categorization;

import com.flowiq.entity.Transaction;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CategorizationResult {

    Transaction.Type type;
    String category;
    boolean autoCategorized;
    CategorizationSource source;

    public static CategorizationResult fromRule(Transaction.Type type, String category) {
        return CategorizationResult.builder()
                .type(type)
                .category(category)
                .autoCategorized(true)
                .source(CategorizationSource.RULES)
                .build();
    }

    public static CategorizationResult fallback(Transaction.Type type, String category) {
        return CategorizationResult.builder()
                .type(type)
                .category(category)
                .autoCategorized(false)
                .source(CategorizationSource.MANUAL)
                .build();
    }
}
