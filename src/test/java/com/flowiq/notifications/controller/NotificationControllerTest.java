package com.flowiq.notifications.controller;

import com.flowiq.exception.ResourceNotFoundException;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.notifications.dto.NotificationPageResponse;
import com.flowiq.notifications.dto.NotificationResponse;
import com.flowiq.notifications.dto.NotificationSummaryResponse;
import com.flowiq.notifications.service.NotificationService;
import com.flowiq.unit.support.ControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationController tests")
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.standaloneMockMvc(notificationController);
    }

    @Test
    @DisplayName("GET /api/notifications returns paginated notifications")
    void list_success() throws Exception {
        when(notificationService.getNotifications(0, 20, null, null, null))
                .thenReturn(NotificationPageResponse.builder()
                        .content(List.of(NotificationResponse.builder().id(1L).title("Tax reminder").build()))
                        .page(0)
                        .size(20)
                        .totalElements(1)
                        .build());

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Tax reminder"));
    }

    @Test
    @DisplayName("GET /api/notifications/unread-count returns count")
    void unreadCount_success() throws Exception {
        when(notificationService.getUnreadCount()).thenReturn(5L);

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5));
    }

    @Test
    @DisplayName("GET /api/notifications/summary returns summary")
    void summary_success() throws Exception {
        when(notificationService.getSummary()).thenReturn(
                NotificationSummaryResponse.builder().total(10).unread(3).build());

        mockMvc.perform(get("/api/notifications/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unread").value(3));
    }

    @Test
    @DisplayName("PUT /api/notifications/{id}/read marks notification as read")
    void markAsRead_success() throws Exception {
        when(notificationService.markAsRead(1L)).thenReturn(
                NotificationResponse.builder().id(1L).read(true).build());

        mockMvc.perform(put("/api/notifications/1/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    @DisplayName("PUT /api/notifications/{id}/read returns 404 when not found")
    void markAsRead_notFound() throws Exception {
        when(notificationService.markAsRead(99L)).thenThrow(new ResourceNotFoundException("Notification not found"));

        mockMvc.perform(put("/api/notifications/99/read"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/notifications/read-all marks all as read")
    void markAllAsRead_success() throws Exception {
        when(notificationService.markAllAsRead()).thenReturn(4);

        mockMvc.perform(put("/api/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(4));
    }

    @Test
    @DisplayName("DELETE /api/notifications/{id} returns 204")
    void delete_success() throws Exception {
        mockMvc.perform(delete("/api/notifications/1"))
                .andExpect(status().isNoContent());

        verify(notificationService).delete(1L);
    }

    @Test
    @DisplayName("GET /api/notifications returns 401 when unauthorized")
    void list_unauthorized() throws Exception {
        doThrow(new UnauthorizedException("Not authenticated"))
                .when(notificationService)
                .getNotifications(0, 20, null, null, null);

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }
}
