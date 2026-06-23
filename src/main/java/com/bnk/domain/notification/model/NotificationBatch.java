package com.bnk.domain.notification.model;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationBatch {
    private Long          batchId;
    private String        notificationCategory;  // TERMS_CHANGED / CARD_UPDATED / EVENT / NOTICE / SYSTEM
    private String        title;
    private String        message;
    private String        linkUrl;
    private String        channels;              // "INAPP,PUSH,EMAIL" 콤마 구분
    private String        targetType;            // ALL / TERMS_AGREED / CARD_OWNER / MARKETING_AGREE
    private Long          targetRefId;           // 약관ID 또는 카드ID
    private String        status;               // DRAFT / SCHEDULED / SENDING / DONE / FAILED
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private int           recipientCount;
    private int           successCount;
    private int           failCount;
    private Long          createdBy;
    private String        createdByName;         // JOIN용
    private Long          sentBy;
    private String        sentByName;            // JOIN용
    private LocalDateTime createdAt;
}
