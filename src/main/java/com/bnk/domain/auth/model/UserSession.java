package com.bnk.domain.auth.model;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {
    private Long sessionId;
    private Long userId;
    private String refreshToken;
    private String deviceInfo;
    private String ipAddress;
    private String userAgent;
    private String revokedYn;       // Y / N
    private LocalDateTime revokedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
