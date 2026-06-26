package com.flowiq.profile.service;

import com.flowiq.entity.User;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.profile.entity.UserSession;
import com.flowiq.profile.repository.UserSessionRepository;
import com.flowiq.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final UserSessionRepository userSessionRepository;
    private final JwtService jwtService;

    @Transactional
    public UserSession createSession(User user, String refreshToken, HttpServletRequest request) {
        UserSession session = new UserSession();
        session.setId(UUID.randomUUID().toString());
        session.setUser(user);
        session.setRefreshTokenHash(hashToken(refreshToken));
        session.setUserAgent(request.getHeader("User-Agent"));
        session.setIpAddress(resolveClientIp(request));
        session.setBrowser(UserAgentParser.resolveBrowser(session.getUserAgent()));
        session.setDeviceLabel(UserAgentParser.resolveDeviceLabel(session.getUserAgent()));
        LocalDateTime now = LocalDateTime.now();
        session.setLoginAt(now);
        session.setLastActivityAt(now);
        return userSessionRepository.save(session);
    }

    @Transactional
    public UserSession rotateRefreshToken(String sessionId, String newRefreshToken) {
        UserSession session = userSessionRepository.findByIdAndRevokedAtIsNull(sessionId)
                .orElseThrow(() -> new UnauthorizedException("Session expired or revoked"));
        session.setRefreshTokenHash(hashToken(newRefreshToken));
        session.setLastActivityAt(LocalDateTime.now());
        return userSessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public void assertSessionActive(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new UnauthorizedException("Session expired or revoked");
        }
        userSessionRepository.findByIdAndRevokedAtIsNull(sessionId)
                .orElseThrow(() -> new UnauthorizedException("Session expired or revoked"));
    }

    @Transactional(readOnly = true)
    public void assertRefreshTokenMatchesSession(String refreshToken) {
        userSessionRepository.findByRefreshTokenHashAndRevokedAtIsNull(hashToken(refreshToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));
    }

    @Transactional
    public void touchSession(String sessionId) {
        userSessionRepository.findByIdAndRevokedAtIsNull(sessionId).ifPresent(session -> {
            session.setLastActivityAt(LocalDateTime.now());
            userSessionRepository.save(session);
        });
    }

    @Transactional(readOnly = true)
    public List<UserSession> listActiveSessions(Long userId) {
        return userSessionRepository.findByUserIdAndRevokedAtIsNullOrderByLastActivityAtDesc(userId);
    }

    @Transactional
    public void revokeSession(String sessionId) {
        userSessionRepository.revokeById(sessionId, LocalDateTime.now());
    }

    @Transactional
    public void revokeAllSessions(Long userId) {
        userSessionRepository.revokeAllForUser(userId, LocalDateTime.now());
    }

    @Transactional
    public void revokeOtherSessions(Long userId, String currentSessionId) {
        userSessionRepository.revokeAllExcept(userId, currentSessionId, LocalDateTime.now());
    }

    public String resolveCurrentSessionId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        try {
            return jwtService.extractSessionId(token);
        } catch (Exception e) {
            return null;
        }
    }

    public static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
