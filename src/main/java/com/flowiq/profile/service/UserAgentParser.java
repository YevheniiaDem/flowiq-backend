package com.flowiq.profile.service;

final class UserAgentParser {

    private UserAgentParser() {
    }

    static String resolveBrowser(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown browser";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("edg/")) return "Microsoft Edge";
        if (ua.contains("chrome/") && !ua.contains("edg/")) return "Google Chrome";
        if (ua.contains("firefox/")) return "Mozilla Firefox";
        if (ua.contains("safari/") && !ua.contains("chrome/")) return "Safari";
        if (ua.contains("opr/") || ua.contains("opera")) return "Opera";
        return "Unknown browser";
    }

    static String resolveDeviceLabel(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown device";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("iphone")) return "iPhone";
        if (ua.contains("ipad")) return "iPad";
        if (ua.contains("android") && ua.contains("mobile")) return "Android phone";
        if (ua.contains("android")) return "Android tablet";
        if (ua.contains("macintosh") || ua.contains("mac os")) return "Mac";
        if (ua.contains("windows")) return "Windows PC";
        if (ua.contains("linux")) return "Linux PC";
        return "Desktop";
    }
}
