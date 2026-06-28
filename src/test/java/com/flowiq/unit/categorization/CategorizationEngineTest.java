package com.flowiq.unit.categorization;

import com.flowiq.categorization.CategorizationEngine;
import com.flowiq.categorization.CategorizationProvider;
import com.flowiq.categorization.CategorizationResult;
import com.flowiq.categorization.CategorizationSource;
import com.flowiq.entity.Transaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("CategorizationEngine tests")
class CategorizationEngineTest {

    @Test
    @DisplayName("matches rule-based category from description keywords")
    void categorize_ruleMatch() {
        CategorizationEngine engine = new CategorizationEngine(List.of());

        CategorizationResult result = engine.categorize("AWS cloud hosting invoice", Transaction.Type.EXPENSE, null);

        assertThat(result.getCategory()).isEqualTo("Infrastructure");
        assertThat(result.getSource()).isEqualTo(CategorizationSource.RULES);
    }

    @Test
    @DisplayName("uses AI provider when no rule matches")
    void categorize_aiProvider() {
        CategorizationProvider provider = mock(CategorizationProvider.class);
        when(provider.categorize(anyString(), any(), any()))
                .thenReturn(Optional.of(CategorizationResult.builder()
                        .type(Transaction.Type.EXPENSE)
                        .category("Marketing")
                        .autoCategorized(true)
                        .source(CategorizationSource.AI)
                        .build()));

        CategorizationEngine engine = new CategorizationEngine(List.of(provider));

        CategorizationResult result = engine.categorize("Random unmatched vendor charge", Transaction.Type.EXPENSE, null);

        assertThat(result.getCategory()).isEqualTo("Marketing");
        assertThat(result.getSource()).isEqualTo(CategorizationSource.AI);
    }

    @Test
    @DisplayName("falls back to hint category when valid")
    void categorize_validHintCategory() {
        CategorizationEngine engine = new CategorizationEngine(List.of());

        CategorizationResult result = engine.categorize("", Transaction.Type.REVENUE, "Services");

        assertThat(result.getCategory()).isEqualTo("Services");
        assertThat(result.getSource()).isEqualTo(CategorizationSource.MANUAL);
    }

    @Test
    @DisplayName("falls back to Other for invalid hint category")
    void categorize_invalidHintCategory() {
        CategorizationEngine engine = new CategorizationEngine(List.of());

        CategorizationResult result = engine.categorize(null, null, "InvalidCategory");

        assertThat(result.getCategory()).isEqualTo("Other");
        assertThat(result.getType()).isEqualTo(Transaction.Type.EXPENSE);
    }
}
