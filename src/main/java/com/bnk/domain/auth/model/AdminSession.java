package com.bnk.domain.auth.model;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSession {
    private Long          sessionId;
    private Long          adminId;
    private String        refreshToken;
    private String        ipAddress;
    private String        userAgent;
    private String        revokedYn;
    private LocalDateTime revokedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}