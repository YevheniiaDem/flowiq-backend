package com.flowiq.unit.security;

import com.flowiq.entity.User;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.security.JwtService;
import com.flowiq.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtService unit tests")
class JwtServiceTest {

    private static final String SECRET = "flowiq-dev-secret-key-change-in-production-min-256-bits-long!!";

    private JwtService jwtService;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 86_400_000L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", 604_800_000L);

        User user = new User();
        user.setId(1L);
        user.setEmail("user@test.flowiq");
        user.setPassword("password");
        user.setName("Test User");
        user.setRole(User.Role.USER);
        user.setActive(true);
        principal = UserPrincipal.from(user);
    }

    @Test
    @DisplayName("generateAccessToken produces access type token")
    void generateAccessToken_producesAccessType() {
        String token = jwtService.generateAccessToken(principal);

        assertThat(jwtService.isAccessToken(token)).isTrue();
        assertThat(jwtService.isRefreshToken(token)).isFalse();
        assertThat(jwtService.extractUsername(token)).isEqualTo(principal.getEmail());
        assertThat(jwtService.isTokenValid(token, principal)).isTrue();
    }

    @Test
    @DisplayName("generateRefreshToken produces refresh type token")
    void generateRefreshToken_producesRefreshType() {
        String token = jwtService.generateRefreshToken(principal);

        assertThat(jwtService.isRefreshToken(token)).isTrue();
        assertThat(jwtService.isAccessToken(token)).isFalse();
        assertThat(jwtService.extractUsername(token)).isEqualTo(principal.getEmail());
        assertThat(jwtService.isTokenValid(token, principal)).isTrue();
    }

    @Test
    @DisplayName("validateRefreshToken accepts valid refresh token")
    void validateRefreshToken_acceptsValidRefreshToken() {
        String refreshToken = jwtService.generateRefreshToken(principal);

        assertThatCode(() -> jwtService.validateRefreshToken(refreshToken))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateRefreshToken rejects access token")
    void validateRefreshToken_rejectsAccessToken() {
        String accessToken = jwtService.generateAccessToken(principal);

        assertThatThrownBy(() -> jwtService.validateRefreshToken(accessToken))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid or expired refresh token");
    }

    @Test
    @DisplayName("validateRefreshToken rejects invalid signature")
    void validateRefreshToken_rejectsInvalidSignature() {
        String refreshToken = jwtService.generateRefreshToken(principal) + "invalid";

        assertThatThrownBy(() -> jwtService.validateRefreshToken(refreshToken))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid or expired refresh token");
    }

    @Test
    @DisplayName("validateRefreshToken rejects expired refresh token")
    void validateRefreshToken_rejectsExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", -1_000L);
        String expiredRefresh = jwtService.generateRefreshToken(principal);

        assertThatThrownBy(() -> jwtService.validateRefreshToken(expiredRefresh))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid or expired refresh token");
    }
}
