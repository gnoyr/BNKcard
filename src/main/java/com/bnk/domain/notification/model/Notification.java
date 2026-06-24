package com.bnk.domain.notification.model;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
	private Long notificationId;
	private Long userId;
	private String notificationCategory; // TERMS_CHANGED / CARD_UPDATED / EVENT / NOTICE / SYSTEM
	private String channel; // INAPP / PUSH / EMAIL / SMS
	private String title;
	private String message;
	private String linkUrl;
	private String sentYn;
	private LocalDateTime sentAt;
	private String readYn;
	private LocalDateTime readAt;
	private Long batchId;
	private LocalDateTime createdAt;
}