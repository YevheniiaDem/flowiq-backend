package com.flowiq.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AppPreferencesFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        AppPreferences preferences = new AppPreferences();

        String language = request.getHeader("X-App-Language");
        if (language != null && !language.isBlank()) {
            preferences.setLanguage(language);
        }

        String currency = request.getHeader("X-App-Currency");
        if (currency != null && !currency.isBlank()) {
            preferences.setCurrency(currency.toUpperCase());
        }

        AppPreferences.set(preferences);
        try {
            filterChain.doFilter(request, response);
        } finally {
            AppPreferences.clear();
        }
    }
}
