package com.flowiq.unit.support;

import com.flowiq.entity.User;
import com.flowiq.security.UserPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityTestSupport {

    private SecurityTestSupport() {
    }

    public static User testUser(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPassword("password");
        user.setName("Test User");
        user.setRole(User.Role.USER);
        user.setActive(true);
        return user;
    }

    public static void authenticate(User user) {
        UserPrincipal principal = UserPrincipal.from(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public static void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}
