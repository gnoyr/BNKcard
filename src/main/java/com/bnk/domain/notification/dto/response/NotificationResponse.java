package com.bnk.domain.notification.dto.response;
 
import com.bnk.domain.notification.model.Notification;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
 
@Getter
@Builder
public class NotificationResponse {
	private Long notificationId;
	private String notificationCategory;
	private String channel;
	private String title;
	private String message;
	private String linkUrl;
	private String readYn;
	private LocalDateTime createdAt;

    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
                .notificationId(n.getNotificationId())
                .notificationCategory(n.getNotificationCategory())
                .channel(n.getChannel())
                .title(n.getTitle())
                .message(n.getMessage())
                .linkUrl(n.getLinkUrl())
                .readYn(n.getReadYn())
                .createdAt(n.getCreatedAt())
                .build();
    }
}