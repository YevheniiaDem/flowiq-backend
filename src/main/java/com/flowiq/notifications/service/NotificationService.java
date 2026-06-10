package com.flowiq.notifications.service;

import com.flowiq.notifications.dto.NotificationPageResponse;
import com.flowiq.notifications.dto.NotificationResponse;
import com.flowiq.notifications.dto.NotificationSummaryResponse;
import com.flowiq.notifications.entity.Notification;
import com.flowiq.notifications.entity.NotificationSeverity;
import com.flowiq.notifications.entity.NotificationType;
import com.flowiq.notifications.repository.NotificationRepository;
import com.flowiq.entity.User;
import com.flowiq.exception.ResourceNotFoundException;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.repository.UserRepository;
import com.flowiq.security.UserPrincipal;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public NotificationPageResponse getNotifications(
            int page,
            int size,
            Boolean unreadOnly,
            NotificationType type,
            NotificationSeverity severity
    ) {
        User user = getCurrentUserEntity();
        Pageable pageable = buildPageable(page, size);
        Specification<Notification> spec = buildSpecification(user.getId(), unreadOnly, type, severity);
        Page<Notification> result = notificationRepository.findAll(spec, pageable);

        return NotificationPageResponse.builder()
                .content(result.getContent().stream().map(NotificationResponse::fromEntity).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {
        User user = getCurrentUserEntity();
        return notificationRepository.countByUserIdAndIsReadFalse(user.getId());
    }

    @Transactional(readOnly = true)
    public NotificationSummaryResponse getSummary() {
        User user = getCurrentUserEntity();
        Long userId = user.getId();
        LocalDateTime monthStart = YearMonth.now().atDay(1).atStartOfDay();

        return NotificationSummaryResponse.builder()
                .total(notificationRepository.countByUserId(userId))
                .unread(notificationRepository.countByUserIdAndIsReadFalse(userId))
                .critical(notificationRepository.countByUserIdAndSeverity(userId, NotificationSeverity.CRITICAL))
                .warnings(notificationRepository.countWarningsByUserId(userId))
                .success(notificationRepository.countSuccessByUserId(userId))
                .thisMonth(notificationRepository.countByUserIdAndCreatedAtAfter(userId, monthStart))
                .build();
    }

    @Transactional
    public NotificationResponse markAsRead(Long id) {
        User user = getCurrentUserEntity();
        Notification notification = notificationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }

        return NotificationResponse.fromEntity(notification);
    }

    @Transactional
    public int markAllAsRead() {
        User user = getCurrentUserEntity();
        return notificationRepository.markAllReadByUserId(user.getId(), LocalDateTime.now());
    }

    @Transactional
    public void delete(Long id) {
        User user = getCurrentUserEntity();
        Notification notification = notificationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notificationRepository.delete(notification);
    }

    private Pageable buildPageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private Specification<Notification> buildSpecification(
            Long userId,
            Boolean unreadOnly,
            NotificationType type,
            NotificationSeverity severity
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), userId));

            if (Boolean.TRUE.equals(unreadOnly)) {
                predicates.add(cb.isFalse(root.get("isRead")));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (severity != null) {
                predicates.add(cb.equal(root.get("severity"), severity));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private User getCurrentUserEntity() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("Not authenticated");
        }
        return userRepository.findByEmail(principal.getEmail())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }
}
