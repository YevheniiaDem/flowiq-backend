package com.flowiq.audit.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class AuditMetadataSanitizer {

    private static final Set<String> FORBIDDEN_KEYS = Set.of(
            "password",
            "passwd",
            "secret",
            "token",
            "accesstoken",
            "refreshtoken",
            "refresh_token",
            "access_token",
            "authorization",
            "apikey",
            "api_key",
            "filecontent",
            "file_content",
            "csvcontent",
            "csv_content",
            "content",
            "bytes",
            "file"
    );

    private AuditMetadataSanitizer() {
    }

    public static Map<String, Object> sanitize(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> sanitized = new HashMap<>();
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            String normalizedKey = entry.getKey().toLowerCase(Locale.ROOT);
            if (FORBIDDEN_KEYS.contains(normalizedKey)) {
                continue;
            }
            Object value = entry.getValue();
            if (value instanceof byte[]) {
                continue;
            }
            if (value instanceof Map<?, ?> nested) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) nested;
                sanitized.put(entry.getKey(), sanitize(nestedMap));
            } else if (value instanceof List<?> list) {
                sanitized.put(entry.getKey(), sanitizeList(list));
            } else {
                sanitized.put(entry.getKey(), value);
            }
        }
        return sanitized;
    }

    private static List<?> sanitizeList(List<?> list) {
        return list.stream()
                .filter(item -> !(item instanceof byte[]))
                .toList();
    }

    public static String sha256(String value) {
        if (value == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
