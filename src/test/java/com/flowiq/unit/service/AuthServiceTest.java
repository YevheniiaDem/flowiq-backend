package com.flowiq.unit.service;

import com.flowiq.audit.service.AuditService;
import com.flowiq.dto.request.RefreshTokenRequest;
import com.flowiq.dto.response.RefreshTokenResponse;
import com.flowiq.entity.User;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.repository.UserRepository;
import com.flowiq.security.CustomUserDetailsService;
import com.flowiq.security.JwtService;
import com.flowiq.security.UserPrincipal;
import com.flowiq.service.AuthService;
import com.flowiq.unit.support.SecurityTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService refresh unit tests")
class AuthServiceTest {

    private static final String EMAIL = "user@test.flowiq";
    private static final String REFRESH_TOKEN = "valid.refresh.token";
    private static final String NEW_ACCESS = "new.access.token";
    private static final String NEW_REFRESH = "new.refresh.token";

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private CustomUserDetailsService customUserDetailsService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("refresh returns new token pair for valid refresh token")
    void refresh_returnsNewTokenPair() {
        User user = SecurityTestSupport.testUser(1L, EMAIL);
        UserPrincipal principal = UserPrincipal.from(user);
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(REFRESH_TOKEN);

        when(jwtService.extractUsername(REFRESH_TOKEN)).thenReturn(EMAIL);
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);
        when(jwtService.isRefreshToken(REFRESH_TOKEN)).thenReturn(true);
        when(jwtService.isTokenValid(REFRESH_TOKEN, principal)).thenReturn(true);
        when(jwtService.generateAccessToken(principal)).thenReturn(NEW_ACCESS);
        when(jwtService.generateRefreshToken(principal)).thenReturn(NEW_REFRESH);

        RefreshTokenResponse response = authService.refresh(request);

        assertThat(response.getToken()).isEqualTo(NEW_ACCESS);
        assertThat(response.getRefreshToken()).isEqualTo(NEW_REFRESH);
        verify(jwtService).validateRefreshToken(REFRESH_TOKEN);
    }

    @Test
    @DisplayName("refresh rejects access token used as refresh token")
    void refresh_rejectsAccessToken() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("access.token");

        doThrow(new UnauthorizedException("Invalid or expired refresh token"))
                .when(jwtService).validateRefreshToken("access.token");

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid or expired refresh token");

        verify(customUserDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    @DisplayName("refresh rejects when user is inactive")
    void refresh_rejectsInactiveUser() {
        User user = SecurityTestSupport.testUser(1L, EMAIL);
        user.setActive(false);
        UserPrincipal principal = UserPrincipal.from(user);
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(REFRESH_TOKEN);

        when(jwtService.extractUsername(REFRESH_TOKEN)).thenReturn(EMAIL);
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid or expired refresh token");
    }

    @Test
    @DisplayName("refresh rejects when user does not exist")
    void refresh_rejectsMissingUser() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(REFRESH_TOKEN);

        when(jwtService.extractUsername(REFRESH_TOKEN)).thenReturn(EMAIL);
        when(customUserDetailsService.loadUserByUsername(EMAIL))
                .thenThrow(new UsernameNotFoundException("User not found"));

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid or expired refresh token");
    }

    @Test
    @DisplayName("refresh rejects expired refresh token")
    void refresh_rejectsExpiredToken() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(REFRESH_TOKEN);

        doThrow(new UnauthorizedException("Invalid or expired refresh token"))
                .when(jwtService).validateRefreshToken(REFRESH_TOKEN);

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid or expired refresh token");

        verify(customUserDetailsService, never()).loadUserByUsername(eq(EMAIL));
    }
}
