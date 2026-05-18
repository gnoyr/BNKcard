package com.bnk.domain.notification.model;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class Notification {
    private Long notificationId;
    private Long userId;
    private String notificationType;    // EMAIL / SMS / PUSH / INAPP
    private String title;
    private String message;
    private String sentYn;
    private LocalDateTime sentAt;
    private String readYn;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
