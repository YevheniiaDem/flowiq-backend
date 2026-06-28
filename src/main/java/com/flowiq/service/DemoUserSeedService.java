package com.flowiq.service;

import com.flowiq.entity.User;
import com.flowiq.profile.service.FopProfileService;
import com.flowiq.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "flowiq.demo-seed.enabled", havingValue = "true", matchIfMissing = true)
public class DemoUserSeedService implements ApplicationRunner {

    public static final String DEMO_EMAIL = "demo@flowiq.ai";
    public static final String DEMO_PASSWORD = "demo123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FopProfileService fopProfileService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(DEMO_EMAIL)) {
            userRepository.findByEmail(DEMO_EMAIL).ifPresent(fopProfileService::getOrCreateForUser);
            return;
        }

        User user = new User();
        user.setEmail(DEMO_EMAIL);
        user.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
        user.setName("Demo User");
        user.setFirstName("Demo");
        user.setLastName("User");
        user.setCompany("Flowiq");
        user.setRole(User.Role.USER);
        user.setActive(true);
        user.setEmailVerified(true);

        User saved = userRepository.save(user);
        fopProfileService.getOrCreateForUser(saved);
        log.info("Demo user created: {}", DEMO_EMAIL);
    }
}
