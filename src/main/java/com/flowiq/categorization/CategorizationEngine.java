package com.flowiq.categorization;

import com.flowiq.entity.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Component
public class CategorizationEngine {

    private static final Set<String> INCOME_CATEGORIES = Set.of(
            "Services", "Consulting", "Software", "Sales", "Subscription", "Other"
    );
    private static final Set<String> EXPENSE_CATEGORIES = Set.of(
            "Marketing", "Salary", "Infrastructure", "Equipment", "Office", "Taxes", "Software", "Other"
    );

    private final List<CategoryRule> rules = DefaultCategoryRules.rules();
    private final List<CategorizationProvider> aiProviders;

    public CategorizationEngine(@Autowired(required = false) List<CategorizationProvider> aiProviders) {
        this.aiProviders = aiProviders != null ? aiProviders : List.of();
    }

    public CategorizationResult categorize(
            String description,
            Transaction.Type hintType,
            String hintCategory
    ) {
        Transaction.Type safeType = hintType != null ? hintType : Transaction.Type.EXPENSE;
        String normalized = normalize(description);

        for (CategoryRule rule : rules) {
            if (rule.matches(normalized)) {
                Transaction.Type type = rule.getTypeOverride() != null
                        ? rule.getTypeOverride()
                        : safeType;
                return CategorizationResult.fromRule(type, rule.getCategory());
            }
        }

        for (CategorizationProvider provider : aiProviders) {
            Optional<CategorizationResult> aiResult = provider.categorize(description, safeType, hintCategory);
            if (aiResult.isPresent() && aiResult.get().isAutoCategorized()) {
                return aiResult.get();
            }
        }

        String category = resolveFallbackCategory(safeType, hintCategory);
        return CategorizationResult.fallback(safeType, category);
    }

    private String resolveFallbackCategory(Transaction.Type type, String hintCategory) {
        if (hintCategory != null && !hintCategory.isBlank() && isValidCategory(type, hintCategory.trim())) {
            return hintCategory.trim();
        }
        return "Other";
    }

    private boolean isValidCategory(Transaction.Type type, String category) {
        Set<String> allowed = type == Transaction.Type.REVENUE ? INCOME_CATEGORIES : EXPENSE_CATEGORIES;
        return allowed.contains(category);
    }

    private String normalize(String description) {
        if (description == null || description.isBlank()) {
            return "";
        }
        return description.trim().toUpperCase(Locale.ROOT);
    }
}
