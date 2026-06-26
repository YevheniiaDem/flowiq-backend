package com.flowiq.profile.dto.response;

import com.flowiq.profile.entity.UserSession;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Active user session")
public class SessionResponse {

    private String id;
    private String deviceLabel;
    private String browser;
    private String ipAddress;
    private String loginAt;
    private String lastActivityAt;
    private boolean current;

    public static SessionResponse fromEntity(UserSession session, boolean current) {
        return SessionResponse.builder()
                .id(session.getId())
                .deviceLabel(session.getDeviceLabel())
                .browser(session.getBrowser())
                .ipAddress(session.getIpAddress())
                .loginAt(session.getLoginAt() != null ? session.getLoginAt().toString() : null)
                .lastActivityAt(session.getLastActivityAt() != null ? session.getLastActivityAt().toString() : null)
                .current(current)
                .build();
    }
}
