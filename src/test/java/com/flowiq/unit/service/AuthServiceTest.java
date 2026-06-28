package com.flowiq.unit.service;

import com.flowiq.audit.service.AuditService;
import com.flowiq.dto.request.LoginRequest;
import com.flowiq.dto.request.RefreshTokenRequest;
import com.flowiq.dto.request.RegisterRequest;
import com.flowiq.dto.response.AuthResponse;
import com.flowiq.dto.response.RefreshTokenResponse;
import com.flowiq.dto.response.UserResponse;
import com.flowiq.entity.User;
import com.flowiq.exception.BadRequestException;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.profile.entity.UserSession;
import com.flowiq.profile.service.FopProfileService;
import com.flowiq.profile.service.SessionService;
import com.flowiq.repository.UserRepository;
import com.flowiq.security.CustomUserDetailsService;
import com.flowiq.security.JwtService;
import com.flowiq.security.UserPrincipal;
import com.flowiq.service.AuthService;
import com.flowiq.unit.support.SecurityTestSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService unit tests")
class AuthServiceTest {

    private static final String EMAIL = "user@test.flowiq";
    private static final String REFRESH_TOKEN = "valid.refresh.token";
    private static final String SESSION_ID = "session-uuid-1";
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
    @Mock
    private SessionService sessionService;
    @Mock
    private FopProfileService fopProfileService;
    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private AuthService authService;

    @AfterEach
    void tearDown() {
        SecurityTestSupport.clearSecurityContext();
    }

    @Test
    @DisplayName("register creates user and returns auth response")
    void register_createsUser() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(EMAIL);
        request.setPassword("Password1!");
        request.setName("Test User");
        request.setCompany("FlowIQ");

        User savedUser = SecurityTestSupport.testUser(1L, EMAIL);
        UserPrincipal principal = UserPrincipal.from(savedUser);
        UserSession session = new UserSession();
        session.setId(SESSION_ID);

        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode("Password1!")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateRefreshToken(any(UserPrincipal.class))).thenReturn(REFRESH_TOKEN, NEW_REFRESH);
        when(jwtService.generateRefreshToken(any(UserPrincipal.class), anyString())).thenReturn(NEW_REFRESH);
        when(jwtService.generateAccessToken(any(UserPrincipal.class), anyString())).thenReturn(NEW_ACCESS);
        when(sessionService.createSession(eq(savedUser), anyString(), eq(httpServletRequest))).thenReturn(session);

        AuthResponse response = authService.register(request, httpServletRequest);

        assertThat(response.getToken()).isEqualTo(NEW_ACCESS);
        assertThat(response.getRefreshToken()).isEqualTo(NEW_REFRESH);
        assertThat(response.getUser().getEmail()).isEqualTo(EMAIL);
        verify(fopProfileService).getOrCreateForUser(savedUser);
    }

    @Test
    @DisplayName("register rejects duplicate email")
    void register_rejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(EMAIL);
        request.setPassword("Password1!");
        request.setName("Test User");

        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request, httpServletRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Email is already registered");
    }

    @Test
    @DisplayName("login returns auth response for valid credentials")
    void login_returnsAuthResponse() {
        LoginRequest request = new LoginRequest();
        request.setEmail(EMAIL);
        request.setPassword("Password1!");

        User user = SecurityTestSupport.testUser(1L, EMAIL);
        UserPrincipal principal = UserPrincipal.from(user);
        UserSession session = new UserSession();
        session.setId(SESSION_ID);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(jwtService.generateRefreshToken(any(UserPrincipal.class))).thenReturn(REFRESH_TOKEN, NEW_REFRESH);
        when(jwtService.generateRefreshToken(any(UserPrincipal.class), anyString())).thenReturn(NEW_REFRESH);
        when(jwtService.generateAccessToken(any(UserPrincipal.class), anyString())).thenReturn(NEW_ACCESS);
        when(sessionService.createSession(eq(user), anyString(), eq(httpServletRequest))).thenReturn(session);

        AuthResponse response = authService.login(request, httpServletRequest);

        assertThat(response.getToken()).isEqualTo(NEW_ACCESS);
        assertThat(response.getUser().getEmail()).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("refresh returns new token pair for valid refresh token")
    void refresh_returnsNewTokenPair() {
        User user = SecurityTestSupport.testUser(1L, EMAIL);
        UserPrincipal principal = UserPrincipal.from(user);
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(REFRESH_TOKEN);

        when(jwtService.extractSessionId(REFRESH_TOKEN)).thenReturn(SESSION_ID);
        when(jwtService.extractUsername(REFRESH_TOKEN)).thenReturn(EMAIL);
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);
        when(jwtService.isRefreshToken(REFRESH_TOKEN)).thenReturn(true);
        when(jwtService.isTokenValid(REFRESH_TOKEN, principal)).thenReturn(true);
        when(jwtService.generateAccessToken(principal, SESSION_ID)).thenReturn(NEW_ACCESS);
        when(jwtService.generateRefreshToken(principal, SESSION_ID)).thenReturn(NEW_REFRESH);

        RefreshTokenResponse response = authService.refresh(request);

        assertThat(response.getToken()).isEqualTo(NEW_ACCESS);
        assertThat(response.getRefreshToken()).isEqualTo(NEW_REFRESH);
        verify(jwtService).validateRefreshToken(REFRESH_TOKEN);
        verify(sessionService).assertRefreshTokenMatchesSession(REFRESH_TOKEN);
        verify(sessionService).assertSessionActive(SESSION_ID);
        verify(sessionService).rotateRefreshToken(SESSION_ID, NEW_REFRESH);
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

        verify(customUserDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    @DisplayName("refresh rejects when user is inactive")
    void refresh_rejectsInactiveUser() {
        User user = SecurityTestSupport.testUser(1L, EMAIL);
        user.setActive(false);
        UserPrincipal principal = UserPrincipal.from(user);
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(REFRESH_TOKEN);

        when(jwtService.extractSessionId(REFRESH_TOKEN)).thenReturn(SESSION_ID);
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

        when(jwtService.extractSessionId(REFRESH_TOKEN)).thenReturn(SESSION_ID);
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

    @Test
    @DisplayName("getCurrentUser returns authenticated user profile")
    void getCurrentUser_returnsProfile() {
        User user = SecurityTestSupport.testUser(1L, EMAIL);
        SecurityTestSupport.authenticate(user);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        UserResponse response = authService.getCurrentUser();

        assertThat(response.getEmail()).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("getCurrentUser rejects unauthenticated request")
    void getCurrentUser_rejectsUnauthenticated() {
        assertThatThrownBy(() -> authService.getCurrentUser())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Not authenticated");
    }

    @Test
    @DisplayName("logout revokes current session when present")
    void logout_revokesSession() {
        when(sessionService.resolveCurrentSessionId(httpServletRequest)).thenReturn(SESSION_ID);

        authService.logout(httpServletRequest);

        verify(sessionService).revokeSession(SESSION_ID);
    }
}
