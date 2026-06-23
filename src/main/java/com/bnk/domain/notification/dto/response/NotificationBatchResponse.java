package com.bnk.domain.notification.dto.response;
 
import com.bnk.domain.notification.model.NotificationBatch;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
 
@Getter
@Builder
public class NotificationBatchResponse {
    private Long          batchId;
    private String        notificationCategory;
    private String        title;
    private String        message;
    private String        linkUrl;
    private String        channels;
    private String        targetType;
    private Long          targetRefId;
    private String        status;
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private int           recipientCount;
    private int           successCount;
    private int           failCount;
    private String        createdByName;
    private String        sentByName;
    private LocalDateTime createdAt;
 
    public static NotificationBatchResponse from(NotificationBatch b) {
        return NotificationBatchResponse.builder()
                .batchId(b.getBatchId())
                .notificationCategory(b.getNotificationCategory())
                .title(b.getTitle())
                .message(b.getMessage())
                .linkUrl(b.getLinkUrl())
                .channels(b.getChannels())
                .targetType(b.getTargetType())
                .targetRefId(b.getTargetRefId())
                .status(b.getStatus())
                .scheduledAt(b.getScheduledAt())
                .sentAt(b.getSentAt())
                .recipientCount(b.getRecipientCount())
                .successCount(b.getSuccessCount())
                .failCount(b.getFailCount())
                .createdByName(b.getCreatedByName())
                .sentByName(b.getSentByName())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
 