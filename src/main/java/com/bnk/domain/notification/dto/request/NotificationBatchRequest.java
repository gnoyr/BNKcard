package com.bnk.domain.notification.dto.request;
 
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
 
@Getter
@NoArgsConstructor
public class NotificationBatchRequest {
 
    /** TERMS_CHANGED / CARD_UPDATED / EVENT / NOTICE / SYSTEM */
    @NotBlank(message = "알림 카테고리는 필수입니다.")
    private String notificationCategory;
 
    @NotBlank(message = "알림 제목은 필수입니다.")
    @Size(max = 200, message = "제목은 200자 이내여야 합니다.")
    private String title;
 
    @NotBlank(message = "알림 내용은 필수입니다.")
    private String message;
 
    /** 알림 클릭 시 이동할 URL (선택) */
    private String linkUrl;
 
    /**
     * 발송 채널 (콤마 구분, 예: "INAPP,PUSH")
     * INAPP / PUSH / EMAIL / SMS
     */
    @NotBlank(message = "발송 채널은 필수입니다.")
    private String channels;
 
    /**
     * 수신 대상 타입
     * ALL / TERMS_AGREED / CARD_OWNER / MARKETING_AGREE
     */
    @NotBlank(message = "수신 대상은 필수입니다.")
    private String targetType;
 
    /**
     * targetType이 TERMS_AGREED이면 약관ID,
     * CARD_OWNER이면 카드ID
     */
    private Long targetRefId;
 
    /**
     * 예약 발송 시각 (null이면 즉시 발송)
     */
    private LocalDateTime scheduledAt;
}