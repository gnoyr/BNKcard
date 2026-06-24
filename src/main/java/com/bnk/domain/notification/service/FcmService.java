package com.bnk.domain.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.bnk.domain.user.mapper.UserMapper;

/**
 * FCM 푸시 발송 서비스.
 *
 * <p>{@code fcm.enabled=true} 일 때만 빈으로 등록된다(FirebaseMessaging 의존).
 * 비활성 시 NotificationService 는 이 빈 없이 동작하므로
 * ObjectProvider 로 주입해 null-safe 하게 호출할 것.</p>
 *
 * <p>도메인 모델 com.bnk.domain.notification.model.Notification 과
 * FCM 의 com.google.firebase.messaging.Notification 이 이름 충돌하므로,
 * FCM 알림 페이로드는 정규화된 이름으로 사용한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "fcm.enabled", havingValue = "true")
public class FcmService {

    private final FirebaseMessaging firebaseMessaging;
    private final UserMapper userMapper;

    /**
     * 단일 디바이스 토큰으로 푸시 발송.
     *
     * @return 성공 여부. 만료/무효 토큰(UNREGISTERED 등)이면 DB에서 토큰을 정리하고 false 반환.
     */
    public boolean sendToToken(Long userId, String token,
                               String title, String body, String linkUrl) {
        if (token == null || token.isBlank()) return false;

        Message message = Message.builder()
                .setToken(token)
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(title)
                        .setBody(truncate(body))
                        .build())
                // 앱이 탭 시 라우팅에 사용할 데이터
                .putData("linkUrl", linkUrl == null ? "" : linkUrl)
                .build();

        try {
            String messageId = firebaseMessaging.send(message);
            log.debug("[FCM] 발송 성공 userId={} messageId={}", userId, messageId);
            return true;
        } catch (FirebaseMessagingException e) {
            MessagingErrorCode code = e.getMessagingErrorCode();
            // 만료/미등록 토큰 → 더 이상 유효하지 않으므로 정리
            if (code == MessagingErrorCode.UNREGISTERED
                    || code == MessagingErrorCode.INVALID_ARGUMENT) {
                log.info("[FCM] 무효 토큰 정리 userId={} code={}", userId, code);
                if (userId != null) {
                    try {
                        userMapper.clearPushToken(userId);
                    } catch (Exception ignore) {
                        // 정리 실패는 무시(다음 폴링/발송 시 재시도)
                    }
                }
            } else {
                log.warn("[FCM] 발송 실패 userId={} code={} msg={}", userId, code, e.getMessage());
            }
            return false;
        }
    }

    /** FCM 본문 길이 안전장치(과도한 길이 방지). */
    private String truncate(String body) {
        if (body == null) return "";
        return body.length() > 240 ? body.substring(0, 240) + "…" : body;
    }
}
