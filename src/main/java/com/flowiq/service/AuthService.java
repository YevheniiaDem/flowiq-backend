package com.flowiq.service;

import com.flowiq.audit.AuditEventType;
import com.flowiq.audit.AuditOutcome;
import com.flowiq.audit.ResourceType;
import com.flowiq.audit.dto.AuditEventRequest;
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
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;
    private final AuthenticationManager authenticationManager;
    private final AuditService auditService;
    private final SessionService sessionService;
    private final FopProfileService fopProfileService;

    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        String email = request.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email is already registered");
        }

        String name = request.getName().trim();
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(name);
        user.setFirstName(splitFirstName(name));
        user.setLastName(splitLastName(name));
        user.setCompany(request.getCompany());
        user.setRole(User.Role.USER);
        user.setActive(true);
        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);
        fopProfileService.getOrCreateForUser(savedUser);
        UserPrincipal principal = UserPrincipal.from(savedUser);

        auditService.log(AuditEventRequest.builder()
                .actorUserId(savedUser.getId())
                .actorEmail(savedUser.getEmail())
                .actorRole(savedUser.getRole().name())
                .eventType(AuditEventType.AUTH_REGISTER)
                .outcome(AuditOutcome.SUCCESS)
                .resourceType(ResourceType.USER)
                .resourceId(savedUser.getId())
                .metadata(Map.of("email", email, "role", savedUser.getRole().name()))
                .build());

        return buildAuthResponse(savedUser, principal, httpRequest);
    }

    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase().trim(),
                        request.getPassword()
                )
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findByEmail(principal.getEmail())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        auditService.log(AuditEventRequest.builder()
                .actorUserId(user.getId())
                .actorEmail(user.getEmail())
                .actorRole(user.getRole().name())
                .eventType(AuditEventType.AUTH_LOGIN_SUCCESS)
                .outcome(AuditOutcome.SUCCESS)
                .resourceType(ResourceType.USER)
                .resourceId(user.getId())
                .metadata(Map.of("email", user.getEmail()))
                .build());

        return buildAuthResponse(user, principal, httpRequest);
    }

    @Transactional
    public RefreshTokenResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken().trim();

        jwtService.validateRefreshToken(refreshToken);
        sessionService.assertRefreshTokenMatchesSession(refreshToken);

        String sessionId = jwtService.extractSessionId(refreshToken);
        sessionService.assertSessionActive(sessionId);

        String email = jwtService.extractUsername(refreshToken);
        UserDetails userDetails;
        try {
            userDetails = customUserDetailsService.loadUserByUsername(email);
        } catch (UsernameNotFoundException e) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        if (!userDetails.isEnabled()) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }
        if (!jwtService.isRefreshToken(refreshToken) || !jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        String newRefreshToken = jwtService.generateRefreshToken(userDetails, sessionId);
        sessionService.rotateRefreshToken(sessionId, newRefreshToken);

        auditService.log(AuditEventRequest.builder()
                .actorUserId(((UserPrincipal) userDetails).getId())
                .actorEmail(userDetails.getUsername())
                .actorRole(((UserPrincipal) userDetails).getRole().name())
                .eventType(AuditEventType.AUTH_REFRESH)
                .outcome(AuditOutcome.SUCCESS)
                .resourceType(ResourceType.SESSION)
                .resourceId(((UserPrincipal) userDetails).getId())
                .metadata(Map.of("sessionId", sessionId))
                .build());

        return RefreshTokenResponse.builder()
                .token(jwtService.generateAccessToken(userDetails, sessionId))
                .refreshToken(newRefreshToken)
                .build();
    }

    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("Not authenticated");
        }

        User user = userRepository.findByEmail(principal.getEmail())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        return UserResponse.fromEntity(user);
    }

    @Transactional
    public void logout(HttpServletRequest httpRequest) {
        String sessionId = sessionService.resolveCurrentSessionId(httpRequest);
        if (sessionId != null) {
            sessionService.revokeSession(sessionId);
        }
    }

    private AuthResponse buildAuthResponse(User user, UserPrincipal principal, HttpServletRequest httpRequest) {
        String refreshToken = jwtService.generateRefreshToken(principal);
        UserSession session = sessionService.createSession(user, refreshToken, httpRequest);
        String accessToken = jwtService.generateAccessToken(principal, session.getId());
        refreshToken = jwtService.generateRefreshToken(principal, session.getId());
        sessionService.rotateRefreshToken(session.getId(), refreshToken);

        return AuthResponse.builder()
                .user(UserResponse.fromEntity(user))
                .token(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    private String splitFirstName(String name) {
        int space = name.indexOf(' ');
        return space > 0 ? name.substring(0, space).trim() : name;
    }

    private String splitLastName(String name) {
        int space = name.indexOf(' ');
        return space > 0 ? name.substring(space + 1).trim() : "";
    }
}
