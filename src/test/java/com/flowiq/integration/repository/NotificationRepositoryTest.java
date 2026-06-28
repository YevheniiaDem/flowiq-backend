package com.flowiq.integration.repository;

import com.flowiq.entity.User;
import com.flowiq.integration.support.AbstractPostgresIntegrationTest;
import com.flowiq.notifications.entity.Notification;
import com.flowiq.notifications.entity.NotificationSeverity;
import com.flowiq.notifications.entity.NotificationType;
import com.flowiq.notifications.repository.NotificationRepository;
import com.flowiq.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NotificationRepository integration tests")
class NotificationRepositoryTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("repo-notif-" + System.nanoTime() + "@test.flowiq");
        user.setPassword("encoded");
        user.setName("Notification User");
        user.setRole(User.Role.USER);
        user.setActive(true);
        user = userRepository.save(user);
    }

    @Test
    @DisplayName("save persists notification with generated id")
    @Transactional
    void save_persistsNotification() {
        Notification saved = notificationRepository.save(sampleNotification("Tax reminder", false));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(user.getId());
        assertThat(saved.getTitle()).isEqualTo("Tax reminder");
        assertThat(saved.isRead()).isFalse();
    }

    @Test
    @DisplayName("findByIdAndUserId returns notification for owner")
    @Transactional
    void findByIdAndUserId_returnsNotification() {
        Notification saved = notificationRepository.save(sampleNotification("FOP limit alert", false));

        assertThat(notificationRepository.findByIdAndUserId(saved.getId(), user.getId()))
                .isPresent()
                .get()
                .extracting(Notification::getTitle, Notification::getSeverity)
                .containsExactly("FOP limit alert", NotificationSeverity.WARNING);
    }

    @Test
    @DisplayName("countByUserIdAndIsReadFalse counts unread notifications")
    @Transactional
    void countByUserIdAndIsReadFalse_countsUnread() {
        notificationRepository.save(sampleNotification("Unread 1", false));
        notificationRepository.save(sampleNotification("Unread 2", false));
        notificationRepository.save(sampleNotification("Read", true));

        assertThat(notificationRepository.countByUserIdAndIsReadFalse(user.getId())).isEqualTo(2);
        assertThat(notificationRepository.countByUserId(user.getId())).isEqualTo(3);
    }

    private Notification sampleNotification(String title, boolean read) {
        Notification notification = new Notification();
        notification.setUserId(user.getId());
        notification.setTitle(title);
        notification.setMessage("Notification body for " + title);
        notification.setType(NotificationType.TAX);
        notification.setSeverity(NotificationSeverity.WARNING);
        notification.setRead(read);
        notification.setDeduplicationKey("notif-" + System.nanoTime());
        return notification;
    }
}
