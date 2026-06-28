package com.flowiq.unit.security;

import com.flowiq.entity.User;
import com.flowiq.security.CustomUserDetailsService;
import com.flowiq.security.JwtAuthenticationFilter;
import com.flowiq.security.JwtService;
import com.flowiq.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("no Authorization header passes request through without authentication")
    void noAuthHeader_passesThrough() throws Exception {
        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).isAccessToken(any());
    }

    @Test
    @DisplayName("invalid token clears security context and continues filter chain")
    void invalidToken_clearsContext() throws Exception {
        request.addHeader("Authorization", "Bearer invalid-token");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("stale@user.com", null));

        doThrow(new RuntimeException("invalid jwt")).when(jwtService).isAccessToken("invalid-token");

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("valid access token sets authentication in security context")
    void validToken_setsAuthentication() throws Exception {
        String token = "valid-access-token";
        String email = "user@test.flowiq";
        UserPrincipal principal = userPrincipal(email);

        request.addHeader("Authorization", "Bearer " + token);

        when(jwtService.isAccessToken(token)).thenReturn(true);
        when(jwtService.extractUsername(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(principal);
        when(jwtService.isTokenValid(token, principal)).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(principal);
        verify(filterChain).doFilter(request, response);
        verify(jwtService).isTokenValid(eq(token), eq(principal));
    }

    private static UserPrincipal userPrincipal(String email) {
        User user = new User();
        user.setId(1L);
        user.setEmail(email);
        user.setPassword("encoded");
        user.setName("Test User");
        user.setRole(User.Role.USER);
        user.setActive(true);
        return UserPrincipal.from(user);
    }
}
