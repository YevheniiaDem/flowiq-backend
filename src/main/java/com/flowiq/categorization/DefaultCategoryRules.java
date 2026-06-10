package com.flowiq.categorization;

import com.flowiq.entity.Transaction;

import java.util.List;

public final class DefaultCategoryRules {

    private DefaultCategoryRules() {
    }

    public static List<CategoryRule> rules() {
        return List.of(
                // Marketing
                new CategoryRule(List.of("GOOGLE ADS"), "Marketing"),
                new CategoryRule(List.of("FACEBOOK ADS"), "Marketing"),
                new CategoryRule(List.of("META ADS"), "Marketing"),

                // Infrastructure
                new CategoryRule(List.of("AWS"), "Infrastructure"),
                new CategoryRule(List.of("DIGITALOCEAN"), "Infrastructure"),
                new CategoryRule(List.of("VULTR"), "Infrastructure"),
                new CategoryRule(List.of("HETZNER"), "Infrastructure"),

                // Software
                new CategoryRule(List.of("APPLE"), "Software"),
                new CategoryRule(List.of("GOOGLE PLAY"), "Software"),
                new CategoryRule(List.of("FIGMA"), "Software"),
                new CategoryRule(List.of("NOTION"), "Software"),

                // Taxes
                new CategoryRule(List.of("ДПС"), "Taxes"),
                new CategoryRule(List.of("TAX"), "Taxes"),
                new CategoryRule(List.of("ЄСВ"), "Taxes"),

                // Income / Services
                new CategoryRule(List.of("PAYMENT FROM CLIENT"), "Services", Transaction.Type.REVENUE),
                new CategoryRule(List.of("INVOICE"), "Services", Transaction.Type.REVENUE)
        );
    }
}
