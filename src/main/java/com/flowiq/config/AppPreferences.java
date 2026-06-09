package com.flowiq.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppPreferences {
    private String language = "uk";
    private String currency = "UAH";

    private static final ThreadLocal<AppPreferences> CONTEXT = ThreadLocal.withInitial(AppPreferences::new);

    public static AppPreferences current() {
        return CONTEXT.get();
    }

    public static void set(AppPreferences preferences) {
        CONTEXT.set(preferences);
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public boolean isUkrainian() {
        return "uk".equalsIgnoreCase(language);
    }
}
