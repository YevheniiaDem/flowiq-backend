package com.flowiq.notifications.preferences;

import lombok.Getter;

@Getter
public enum NotificationPreferenceKey {
    // Financial
    FINANCIAL_TAXES(PreferenceCategory.FINANCIAL),
    FINANCIAL_LARGE_EXPENSE(PreferenceCategory.FINANCIAL),
    FINANCIAL_LARGE_INCOME(PreferenceCategory.FINANCIAL),
    FINANCIAL_NEGATIVE_CASH_FLOW(PreferenceCategory.FINANCIAL),
    FINANCIAL_LOW_BALANCE(PreferenceCategory.FINANCIAL),
    FINANCIAL_OVERDUE_PAYMENT(PreferenceCategory.FINANCIAL),
    FINANCIAL_TAX_WARNING(PreferenceCategory.FINANCIAL),

    // Tasks
    TASK_REMINDER_TODAY(PreferenceCategory.TASKS),
    TASK_REMINDER_3_DAYS(PreferenceCategory.TASKS),
    TASK_REMINDER_WEEK(PreferenceCategory.TASKS),
    TASK_REMINDER_OVERDUE(PreferenceCategory.TASKS),

    // AI
    AI_ACCOUNTANT_RECOMMENDATIONS(PreferenceCategory.AI),
    AI_FINANCIAL_TIPS(PreferenceCategory.AI),
    AI_TAX_OPTIMIZATION(PreferenceCategory.AI),
    AI_WARNINGS(PreferenceCategory.AI),
    AI_FORECAST_ANOMALY(PreferenceCategory.AI),

    // Imports
    IMPORT_COMPLETED(PreferenceCategory.IMPORTS),
    IMPORT_FAILED(PreferenceCategory.IMPORTS),
    IMPORT_PARTIAL(PreferenceCategory.IMPORTS),
    IMPORT_CSV_PROCESSING(PreferenceCategory.IMPORTS),

    // Reports
    REPORT_READY(PreferenceCategory.REPORTS),
    REPORT_GENERATION_ERROR(PreferenceCategory.REPORTS),
    REPORT_PDF_AVAILABLE(PreferenceCategory.REPORTS),
    REPORT_EXCEL_AVAILABLE(PreferenceCategory.REPORTS);

    private final PreferenceCategory category;

    NotificationPreferenceKey(PreferenceCategory category) {
        this.category = category;
    }

    public enum PreferenceCategory {
        FINANCIAL,
        TASKS,
        AI,
        IMPORTS,
        REPORTS
    }
}
