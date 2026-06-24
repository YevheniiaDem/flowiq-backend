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
import com.flowiq.repository.UserRepository;
import com.flowiq.security.CustomUserDetailsService;
import com.flowiq.security.JwtService;
import com.flowiq.security.UserPrincipal;
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

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email is already registered");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName().trim());
        user.setCompany(request.getCompany());
        user.setRole(User.Role.USER);
        user.setActive(true);
        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);
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

        return buildAuthResponse(savedUser, principal);
    }

    public AuthResponse login(LoginRequest request) {
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

        return buildAuthResponse(user, principal);
    }

    public RefreshTokenResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken().trim();

        jwtService.validateRefreshToken(refreshToken);

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

        UserPrincipal principal = (UserPrincipal) userDetails;
        return RefreshTokenResponse.builder()
                .token(jwtService.generateAccessToken(principal))
                .refreshToken(jwtService.generateRefreshToken(principal))
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

    private AuthResponse buildAuthResponse(User user, UserPrincipal principal) {
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);

        return AuthResponse.builder()
                .user(UserResponse.fromEntity(user))
                .token(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
