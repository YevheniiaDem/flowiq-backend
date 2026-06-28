package com.flowiq.unit.notifications;

import com.flowiq.entity.User;
import com.flowiq.exception.ResourceNotFoundException;
import com.flowiq.notifications.dto.NotificationPageResponse;
import com.flowiq.notifications.entity.Notification;
import com.flowiq.notifications.entity.NotificationSeverity;
import com.flowiq.notifications.entity.NotificationType;
import com.flowiq.notifications.repository.NotificationRepository;
import com.flowiq.notifications.service.NotificationService;
import com.flowiq.repository.UserRepository;
import com.flowiq.unit.support.SecurityTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService unit tests")
class NotificationServiceTest {

    private static final Long USER_ID = 1L;
    private static final String EMAIL = "notify@test.flowiq";

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User user;

    @BeforeEach
    void setUp() {
        user = SecurityTestSupport.testUser(USER_ID, EMAIL);
        SecurityTestSupport.authenticate(user);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    }

    @AfterEach
    void tearDown() {
        SecurityTestSupport.clearSecurityContext();
    }

    @Test
    @DisplayName("getNotifications returns paginated list")
    void getNotifications_success() {
        Notification notification = sampleNotification(1L);
        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(notificationRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        NotificationPageResponse response = notificationService.getNotifications(0, 10, null, null, null);

        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getUnreadCount returns unread total")
    void getUnreadCount_success() {
        when(notificationRepository.countByUserIdAndIsReadFalse(USER_ID)).thenReturn(3L);

        assertThat(notificationService.getUnreadCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("markAsRead updates notification")
    void markAsRead_success() {
        Notification notification = sampleNotification(2L);
        when(notificationRepository.findByIdAndUserId(2L, USER_ID)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        var response = notificationService.markAsRead(2L);

        assertThat(response.isRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("markAsRead throws when notification not found")
    void markAsRead_notFound() {
        when(notificationRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("delete removes owned notification")
    void delete_success() {
        Notification notification = sampleNotification(3L);
        when(notificationRepository.findByIdAndUserId(3L, USER_ID)).thenReturn(Optional.of(notification));

        notificationService.delete(3L);

        verify(notificationRepository).delete(notification);
    }

    private Notification sampleNotification(Long id) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setUserId(USER_ID);
        notification.setTitle("Test");
        notification.setMessage("Message");
        notification.setType(NotificationType.FINANCIAL);
        notification.setSeverity(NotificationSeverity.INFO);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        return notification;
    }
}
