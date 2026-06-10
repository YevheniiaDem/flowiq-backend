package com.flowiq.categorization;

import com.flowiq.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Locale;

@Getter
@AllArgsConstructor
public class CategoryRule {

    private final List<String> keywords;
    private final String category;
    private final Transaction.Type typeOverride;

    public CategoryRule(List<String> keywords, String category) {
        this(keywords, category, null);
    }

    public boolean matches(String normalizedDescription) {
        if (normalizedDescription == null || normalizedDescription.isBlank()) {
            return false;
        }
        for (String keyword : keywords) {
            if (containsKeyword(normalizedDescription, keyword.toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private boolean containsKeyword(String text, String keyword) {
        int index = text.indexOf(keyword);
        while (index >= 0) {
            boolean startOk = index == 0 || !Character.isLetterOrDigit(text.charAt(index - 1));
            int end = index + keyword.length();
            boolean endOk = end >= text.length() || !Character.isLetterOrDigit(text.charAt(end));
            if (startOk && endOk) {
                return true;
            }
            index = text.indexOf(keyword, index + 1);
        }
        return false;
    }
}
