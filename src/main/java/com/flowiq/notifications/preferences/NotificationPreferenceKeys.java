package com.flowiq.notifications.preferences;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class NotificationPreferenceKeys {

    private NotificationPreferenceKeys() {
    }

    public static NotificationPreferenceKey taskReminderKey(LocalDate today, LocalDate dueDate) {
        if (dueDate == null) {
            return NotificationPreferenceKey.TASK_REMINDER_WEEK;
        }
        long daysUntil = ChronoUnit.DAYS.between(today, dueDate);
        if (daysUntil < 0) {
            return NotificationPreferenceKey.TASK_REMINDER_OVERDUE;
        }
        if (daysUntil <= 1) {
            return NotificationPreferenceKey.TASK_REMINDER_TODAY;
        }
        if (daysUntil <= 3) {
            return NotificationPreferenceKey.TASK_REMINDER_3_DAYS;
        }
        return NotificationPreferenceKey.TASK_REMINDER_WEEK;
    }
}
