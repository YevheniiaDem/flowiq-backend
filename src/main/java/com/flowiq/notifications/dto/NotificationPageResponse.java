package com.flowiq.notifications.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class NotificationPageResponse {

    private List<NotificationResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
