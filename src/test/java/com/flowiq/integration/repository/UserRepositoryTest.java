package com.flowiq.integration.repository;

import com.flowiq.entity.User;
import com.flowiq.integration.support.AbstractPostgresIntegrationTest;
import com.flowiq.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserRepository integration tests")
class UserRepositoryTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("save persists user with generated id")
    @Transactional
    void save_persistsUser() {
        User user = sampleUser("save-" + System.nanoTime() + "@test.flowiq");

        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo(user.getEmail());
        assertThat(saved.getName()).isEqualTo("Repo User");
        assertThat(saved.getRole()).isEqualTo(User.Role.USER);
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    @DisplayName("findByEmail returns saved user")
    @Transactional
    void findByEmail_returnsSavedUser() {
        String email = "find-" + System.nanoTime() + "@test.flowiq";
        userRepository.save(sampleUser(email));

        assertThat(userRepository.findByEmail(email))
                .isPresent()
                .get()
                .extracting(User::getEmail, User::getName)
                .containsExactly(email, "Repo User");
    }

    private static User sampleUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("encoded");
        user.setName("Repo User");
        user.setRole(User.Role.USER);
        user.setActive(true);
        return user;
    }
}
