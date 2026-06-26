package com.flowiq.notifications.preferences;

import com.flowiq.notifications.entity.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    List<NotificationPreference> findByUser_Id(Long userId);

    Optional<NotificationPreference> findByUser_IdAndNotificationTypeAndChannel(
            Long userId,
            NotificationPreferenceKey notificationType,
            NotificationChannel channel
    );

    void deleteByUser_Id(Long userId);
}
