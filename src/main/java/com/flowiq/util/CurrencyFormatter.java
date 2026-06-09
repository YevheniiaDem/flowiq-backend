package com.flowiq.util;

import com.flowiq.config.AppPreferences;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

public final class CurrencyFormatter {

    private static final Map<String, BigDecimal> RATES_FROM_UAH = Map.of(
            "USD", new BigDecimal("0.02439"),
            "EUR", new BigDecimal("0.02273")
    );

    private CurrencyFormatter() {
    }

    public static String format(BigDecimal amountUah) {
        String currency = AppPreferences.current().getCurrency();
        BigDecimal displayAmount = convertFromUah(amountUah, currency);

        return switch (currency) {
            case "USD" -> String.format(Locale.US, "$%,.0f", displayAmount);
            case "EUR" -> {
                NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.GERMANY);
                formatter.setMaximumFractionDigits(0);
                yield formatter.format(displayAmount);
            }
            default -> formatUah(displayAmount);
        };
    }

    private static BigDecimal convertFromUah(BigDecimal amountUah, String currency) {
        if ("UAH".equals(currency)) {
            return amountUah;
        }
        BigDecimal rate = RATES_FROM_UAH.getOrDefault(currency, BigDecimal.ONE);
        return amountUah.multiply(rate).setScale(0, RoundingMode.HALF_UP);
    }

    private static String formatUah(BigDecimal amount) {
        NumberFormat formatter = NumberFormat.getIntegerInstance(new Locale("uk", "UA"));
        return formatter.format(amount) + " \u20B4";
    }
}
