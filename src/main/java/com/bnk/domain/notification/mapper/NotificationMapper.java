package com.bnk.domain.notification.mapper;

import com.bnk.domain.notification.model.Notification;
import com.bnk.domain.notification.model.NotificationBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationMapper {

    // ── 알림 이력 ────────────────────────────────────────────────
    int insertNotification(Notification notification);

    int batchInsertNotifications(@Param("list") List<Notification> list);

    List<Notification> findByUserId(@Param("userId") Long userId);

    /** 미읽음 개수 (헤더 뱃지용) */
    int countUnreadByUserId(@Param("userId") Long userId);

    int markAsRead(@Param("notificationId") Long notificationId,
                   @Param("userId") Long userId);

    int markAllAsRead(@Param("userId") Long userId);

    // ── 발송 배치 (관리자) ────────────────────────────────────────
    int insertBatch(NotificationBatch batch);

    NotificationBatch findBatchById(@Param("batchId") Long batchId);

    List<NotificationBatch> findBatches(@Param("offset") int offset,
                                        @Param("limit")  int limit);

    int countBatches();

    int updateBatchStatus(@Param("batchId")        Long   batchId,
                          @Param("status")          String status,
                          @Param("recipientCount")  int    recipientCount,
                          @Param("successCount")    int    successCount,
                          @Param("failCount")       int    failCount,
                          @Param("sentBy")          Long   sentBy);

    int updateBatchStatusOnly(@Param("batchId") Long   batchId,
                               @Param("status")  String status);

    // ── 수신자 조회 (target_type 별) ─────────────────────────────
    /** push_enabled = 'Y' 전체 회원 ID */
    List<Long> findAllPushEnabledUserIds();

    /** 특정 약관에 동의한 회원 ID */
    List<Long> findUserIdsByTermsAgreement(@Param("termsId") Long termsId);

    /** 특정 카드 보유 회원 ID */
    List<Long> findUserIdsByOwnedCard(@Param("cardId") Long cardId);

    /** marketing_agree = 'Y' 회원 ID */
    List<Long> findMarketingAgreeUserIds();

    /** deleted_yn='N' 전체 회원 ID */
    List<Long> findAllActiveUserIds();
}
