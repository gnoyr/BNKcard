package com.bnk.domain.notification.service;

import com.bnk.domain.notification.dto.request.NotificationBatchRequest;
import com.bnk.domain.notification.dto.response.NotificationBatchResponse;
import com.bnk.domain.notification.dto.response.NotificationListResponse;
import com.bnk.domain.notification.dto.response.NotificationResponse;
import com.bnk.domain.notification.mapper.NotificationMapper;
import com.bnk.domain.notification.model.Notification;
import com.bnk.domain.notification.model.NotificationBatch;
import com.bnk.global.email.EmailService;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final ObjectProvider<FcmService> fcmServiceProvider;
    private final EmailService emailService;

    private static final int BATCH_CHUNK = 500; // INSERT ALL / IN-list(Oracle 1000) 제한 고려

    // ================================================================
    // 사용자 API — 알림 목록 / 읽음 처리
    // ================================================================

    @Transactional(readOnly = true)
    public NotificationListResponse getMyNotifications(Long userId) {
        List<Notification> list = notificationMapper.findByUserId(userId);
        int unread = notificationMapper.countUnreadByUserId(userId);
        return NotificationListResponse.builder()
                .unreadCount(unread)
                .notifications(list.stream().map(NotificationResponse::from).collect(Collectors.toList()))
                .build();
    }

    @Transactional(readOnly = true)
    public int getUnreadCount(Long userId) {
        return notificationMapper.countUnreadByUserId(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        int updated = notificationMapper.markAsRead(notificationId, userId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "해당 알림을 찾을 수 없습니다.");
        }
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationMapper.markAllAsRead(userId);
    }

    // ================================================================
    // 자동 트리거 — 약관 변경 / 카드 변경 / 이벤트 등록
    // ================================================================

    /**
     * 약관 변경 알림 (ApprovalService.handleTermsApprove() 에서 호출)
     * 대상: 해당 약관에 동의한 전체 회원
     */
    @Transactional
    public void notifyTermsChanged(Long termsId, String termsTitle) {
        List<Long> userIds = notificationMapper.findUserIdsByTermsAgreement(termsId);
        if (userIds.isEmpty()) {
            log.info("[Notification] 약관 변경 알림 대상 없음 termsId={}", termsId);
            return;
        }
        String title   = "약관이 변경되었습니다";
        String message = "'" + termsTitle + "' 약관이 변경되었습니다. 확인 후 동의 여부를 선택해 주세요.";
        String linkUrl = "/terms/" + termsId;

        dispatchToUsers(userIds, "TERMS_CHANGED", title, message, linkUrl, List.of("INAPP"), null);
        log.info("[Notification] 약관 변경 알림 발송 termsId={}, 대상={}명", termsId, userIds.size());
    }

    /**
     * 카드 정보 변경 알림 (ApprovalService.handleCardApprove() 에서 호출)
     * 대상: 해당 카드 보유 회원 + push_enabled='Y'
     */
    @Transactional
    public void notifyCardUpdated(Long cardId, String cardName) {
        List<Long> userIds = notificationMapper.findUserIdsByOwnedCard(cardId);
        if (userIds.isEmpty()) return;
        String title   = "카드 정보가 변경되었습니다";
        String message = "'" + cardName + "' 카드의 혜택 또는 이용 조건이 업데이트되었습니다.";
        String linkUrl = "/cards/" + cardId;

        dispatchToUsers(userIds, "CARD_UPDATED", title, message, linkUrl, List.of("INAPP", "PUSH"), null);
        log.info("[Notification] 카드 변경 알림 발송 cardId={}, 대상={}명", cardId, userIds.size());
    }

    /**
     * 이벤트 등록 알림 (EventService.create() 등에서 호출)
     * 대상: marketing_agree='Y' 회원
     */
    @Transactional
    public void notifyEventCreated(String eventTitle, String eventDescription, String linkUrl) {
        List<Long> userIds = notificationMapper.findMarketingAgreeUserIds();
        if (userIds.isEmpty()) return;
        String title   = "새 이벤트가 등록되었습니다";
        String message = "'" + eventTitle + "' 이벤트를 확인해 보세요!\n" + eventDescription;

        dispatchToUsers(userIds, "EVENT", title, message, linkUrl, List.of("INAPP", "PUSH"), null);
        log.info("[Notification] 이벤트 알림 발송 eventTitle={}, 대상={}명", eventTitle, userIds.size());
    }

    // ================================================================
    // 관리자 배치 발송
    // ================================================================

    /** 배치 초안 저장 */
    @Transactional
    public NotificationBatchResponse createBatch(NotificationBatchRequest req, Long adminId) {
        NotificationBatch batch = NotificationBatch.builder()
                .notificationCategory(req.getNotificationCategory())
                .title(req.getTitle())
                .message(req.getMessage())
                .linkUrl(req.getLinkUrl())
                .channels(req.getChannels())
                .targetType(req.getTargetType())
                .targetRefId(req.getTargetRefId())
                .status(req.getScheduledAt() != null ? "SCHEDULED" : "DRAFT")
                .scheduledAt(req.getScheduledAt())
                .createdBy(adminId)
                .build();
        notificationMapper.insertBatch(batch);
        return NotificationBatchResponse.from(batch);
    }

    /** 배치 즉시 발송 */
    @Transactional
    public NotificationBatchResponse sendBatch(Long batchId, Long adminId) {
        NotificationBatch batch = notificationMapper.findBatchById(batchId);
        if (batch == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "배치를 찾을 수 없습니다.");
        }
        if ("DONE".equals(batch.getStatus()) || "SENDING".equals(batch.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미 발송됐거나 발송 중인 배치입니다.");
        }

        // SENDING 상태로 전환
        notificationMapper.updateBatchStatusOnly(batchId, "SENDING");

        // 수신자 조회
        List<Long> userIds = resolveTargetUserIds(batch);

        // 채널 파싱
        List<String> channels = Arrays.asList(batch.getChannels().split(","));

        // 알림 생성 및 발송
        int success = 0, fail = 0;
        try {
            dispatchToUsers(userIds, batch.getNotificationCategory(),
                    batch.getTitle(), batch.getMessage(), batch.getLinkUrl(),
                    channels, batchId);
            success = userIds.size();
        } catch (Exception e) {
            log.error("[Notification] 배치 발송 오류 batchId={}", batchId, e);
            fail = userIds.size();
        }

        // 완료 상태 갱신
        notificationMapper.updateBatchStatus(batchId, "DONE",
                userIds.size(), success, fail, adminId);

        return NotificationBatchResponse.from(notificationMapper.findBatchById(batchId));
    }

    /** 배치 목록 조회 */
    @Transactional(readOnly = true)
    public PageResponse<NotificationBatchResponse> getBatches(int page, int size) {
        int offset = page * size;
        List<NotificationBatch> list = notificationMapper.findBatches(offset, size);
        int total = notificationMapper.countBatches();
        List<NotificationBatchResponse> content = list.stream()
                .map(NotificationBatchResponse::from)
                .collect(Collectors.toList());
        return PageResponse.of(content, (long) total, page, size);
    }

    /** 배치 상세 */
    @Transactional(readOnly = true)
    public NotificationBatchResponse getBatch(Long batchId) {
        NotificationBatch batch = notificationMapper.findBatchById(batchId);
        if (batch == null) throw new BusinessException(ErrorCode.INVALID_INPUT, "배치를 찾을 수 없습니다.");
        return NotificationBatchResponse.from(batch);
    }

    // ================================================================
    // private helpers
    // ================================================================

    /** target_type → 수신자 userId 목록 변환 */
    private List<Long> resolveTargetUserIds(NotificationBatch batch) {
        return switch (batch.getTargetType()) {
            case "ALL"             -> notificationMapper.findAllActiveUserIds();
            case "TERMS_AGREED"    -> notificationMapper.findUserIdsByTermsAgreement(batch.getTargetRefId());
            case "CARD_OWNER"      -> notificationMapper.findUserIdsByOwnedCard(batch.getTargetRefId());
            case "MARKETING_AGREE" -> notificationMapper.findMarketingAgreeUserIds();
            default                -> notificationMapper.findAllPushEnabledUserIds();
        };
    }

    /**
     * 실제 알림 저장 + 채널별 발송
     * INAPP: DB INSERT
     * PUSH:  push_enabled='Y' + 토큰 보유자에게 FCM 발송 (fcm.enabled=false 시 자동 skip)
     * EMAIL: 대상자 이메일로 EmailService 발송 (Teal 테마 HTML)
     */
    private void dispatchToUsers(List<Long> userIds,
                                  String category,
                                  String title,
                                  String message,
                                  String linkUrl,
                                  List<String> channels,
                                  Long batchId) {
        if (userIds == null || userIds.isEmpty()) return;

        // INAPP 채널: DB에 알림 기록
        if (channels.contains("INAPP")) {
            List<Notification> batch = new ArrayList<>();
            for (Long uid : userIds) {
                batch.add(Notification.builder()
                        .userId(uid)
                        .notificationCategory(category)
                        .channel("INAPP")
                        .title(title)
                        .message(message)
                        .linkUrl(linkUrl)
                        .batchId(batchId)
                        .build());
                if (batch.size() >= BATCH_CHUNK) {
                    notificationMapper.batchInsertNotifications(batch);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                notificationMapper.batchInsertNotifications(batch);
            }
        }

        // PUSH 채널: push_enabled='Y' 필터 후 FCM 발송
        if (channels.contains("PUSH")) {
            FcmService fcm = fcmServiceProvider.getIfAvailable();
            if (fcm == null) {
                log.info("[Notification] PUSH 비활성(fcm.enabled=false) — {}명 건너뜀", userIds.size());
            } else {
                int sent = 0, total = 0;
                for (int i = 0; i < userIds.size(); i += BATCH_CHUNK) {
                    List<Long> chunk = userIds.subList(i, Math.min(i + BATCH_CHUNK, userIds.size()));
                    var targets = notificationMapper.findPushTargets(chunk);
                    total += targets.size();
                    for (var t : targets) {
                        if (fcm.sendToToken(t.getUserId(), t.getPushToken(), title, message, linkUrl)) sent++;
                    }
                }
                log.info("[Notification] PUSH 발송 완료 {}/{}건", sent, total);
            }
        }

        // EMAIL 채널: 대상자 이메일로 발송 (각 건은 EmailService 내부에서 @Async 처리)
        if (channels.contains("EMAIL")) {
            int sent = 0;
            for (int i = 0; i < userIds.size(); i += BATCH_CHUNK) {
                List<Long> chunk = userIds.subList(i, Math.min(i + BATCH_CHUNK, userIds.size()));
                List<String> emails = notificationMapper.findEmailsByUserIds(chunk);
                for (String email : emails) {
                    emailService.sendNotificationEmail(email, title, message, linkUrl);
                    sent++;
                }
            }
            log.info("[Notification] EMAIL 발송 요청 {}건 (비동기 큐잉)", sent);
        }
    }
}