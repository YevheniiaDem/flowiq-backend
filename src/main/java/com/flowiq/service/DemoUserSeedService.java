package com.flowiq.service;

import com.flowiq.entity.User;
import com.flowiq.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DemoUserSeedService implements ApplicationRunner {

    public static final String DEMO_EMAIL = "demo@flowiq.ai";
    public static final String DEMO_PASSWORD = "demo123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(DEMO_EMAIL)) {
            return;
        }

        User user = new User();
        user.setEmail(DEMO_EMAIL);
        user.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
        user.setName("Demo User");
        user.setCompany("Flowiq");
        user.setRole(User.Role.USER);
        user.setActive(true);
        user.setEmailVerified(true);

        userRepository.save(user);
        log.info("Demo user created: {} / {}", DEMO_EMAIL, DEMO_PASSWORD);
    }
}
