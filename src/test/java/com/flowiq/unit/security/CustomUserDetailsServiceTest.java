package com.flowiq.unit.security;

import com.flowiq.entity.User;
import com.flowiq.repository.UserRepository;
import com.flowiq.security.CustomUserDetailsService;
import com.flowiq.security.UserPrincipal;
import com.flowiq.unit.support.SecurityTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService unit tests")
class CustomUserDetailsServiceTest {

    private static final String EMAIL = "security@test.flowiq";

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("loadUserByUsername returns UserPrincipal for existing user")
    void loadUserByUsername_success() {
        User user = SecurityTestSupport.testUser(1L, EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        var details = customUserDetailsService.loadUserByUsername(EMAIL);

        assertThat(details).isInstanceOf(UserPrincipal.class);
        assertThat(details.getUsername()).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("loadUserByUsername throws when user not found")
    void loadUserByUsername_notFound() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(EMAIL))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found: " + EMAIL);
    }
}
