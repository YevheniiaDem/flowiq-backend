package com.flowiq.unit.profile;

import com.flowiq.entity.User;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.profile.entity.UserSession;
import com.flowiq.profile.repository.UserSessionRepository;
import com.flowiq.profile.service.SessionService;
import com.flowiq.security.JwtService;
import com.flowiq.unit.support.SecurityTestSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionService unit tests")
class SessionServiceTest {

    private static final String SESSION_ID = "session-uuid-1";
    private static final String REFRESH_TOKEN = "refresh.token.value";

    @Mock
    private UserSessionRepository userSessionRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private SessionService sessionService;

    @Test
    @DisplayName("createSession persists session with hashed refresh token")
    void createSession_success() {
        User user = SecurityTestSupport.testUser(1L, "session@test.flowiq");
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("JUnit");
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(userSessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserSession session = sessionService.createSession(user, REFRESH_TOKEN, httpServletRequest);

        assertThat(session.getId()).isNotBlank();
        assertThat(session.getRefreshTokenHash()).isEqualTo(SessionService.hashToken(REFRESH_TOKEN));
        assertThat(session.getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("rotateRefreshToken updates hash for active session")
    void rotateRefreshToken_success() {
        UserSession session = activeSession();
        when(userSessionRepository.findByIdAndRevokedAtIsNull(SESSION_ID)).thenReturn(Optional.of(session));
        when(userSessionRepository.save(session)).thenReturn(session);

        UserSession rotated = sessionService.rotateRefreshToken(SESSION_ID, "new.refresh.token");

        assertThat(rotated.getRefreshTokenHash()).isEqualTo(SessionService.hashToken("new.refresh.token"));
    }

    @Test
    @DisplayName("rotateRefreshToken rejects revoked session")
    void rotateRefreshToken_revoked() {
        when(userSessionRepository.findByIdAndRevokedAtIsNull(SESSION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.rotateRefreshToken(SESSION_ID, "token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Session expired or revoked");
    }

    @Test
    @DisplayName("assertSessionActive rejects blank session id")
    void assertSessionActive_blankId() {
        assertThatThrownBy(() -> sessionService.assertSessionActive(" "))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Session expired or revoked");
    }

    @Test
    @DisplayName("assertRefreshTokenMatchesSession validates token hash")
    void assertRefreshTokenMatchesSession_success() {
        when(userSessionRepository.findByRefreshTokenHashAndRevokedAtIsNull(SessionService.hashToken(REFRESH_TOKEN)))
                .thenReturn(Optional.of(activeSession()));

        sessionService.assertRefreshTokenMatchesSession(REFRESH_TOKEN);
    }

    @Test
    @DisplayName("assertRefreshTokenMatchesSession rejects unknown token")
    void assertRefreshTokenMatchesSession_invalid() {
        when(userSessionRepository.findByRefreshTokenHashAndRevokedAtIsNull(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.assertRefreshTokenMatchesSession("bad-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid or expired refresh token");
    }

    @Test
    @DisplayName("listActiveSessions returns sessions for user")
    void listActiveSessions_success() {
        when(userSessionRepository.findByUserIdAndRevokedAtIsNullOrderByLastActivityAtDesc(1L))
                .thenReturn(List.of(activeSession()));

        assertThat(sessionService.listActiveSessions(1L)).hasSize(1);
    }

    @Test
    @DisplayName("revokeSession delegates to repository")
    void revokeSession_success() {
        sessionService.revokeSession(SESSION_ID);

        verify(userSessionRepository).revokeById(eq(SESSION_ID), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("resolveCurrentSessionId extracts session from bearer token")
    void resolveCurrentSessionId_success() {
        when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer access.token");
        when(jwtService.extractSessionId("access.token")).thenReturn(SESSION_ID);

        assertThat(sessionService.resolveCurrentSessionId(httpServletRequest)).isEqualTo(SESSION_ID);
    }

    @Test
    @DisplayName("resolveCurrentSessionId returns null without bearer header")
    void resolveCurrentSessionId_missingHeader() {
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);

        assertThat(sessionService.resolveCurrentSessionId(httpServletRequest)).isNull();
    }

    private UserSession activeSession() {
        UserSession session = new UserSession();
        session.setId(SESSION_ID);
        session.setUser(SecurityTestSupport.testUser(1L, "session@test.flowiq"));
        session.setRefreshTokenHash(SessionService.hashToken(REFRESH_TOKEN));
        session.setLoginAt(LocalDateTime.now());
        session.setLastActivityAt(LocalDateTime.now());
        return session;
    }
}
