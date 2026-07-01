package com.bnk.domain.application.log;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.bnk.global.log.model.CardEventLog;
import com.bnk.global.log.model.EventLog;
import com.bnk.global.log.service.EventLogService;
import com.bnk.global.util.TimeConstants;

import lombok.RequiredArgsConstructor;

/**
 * 카드 발급 프로세스 분기별 로깅 헬퍼.
 *
 * 신청·본인확인·제출·심사·한도·추가심사·발급의 각 분기에서
 * EventLog(부모) + CardEventLog(자식)를 남긴다.
 * 저장은 {@link EventLogService}가 {@code @Async + REQUIRES_NEW}로 처리하므로
 * 메인 트랜잭션과 독립적이며(실패해도 본 로직에 영향 없음), 로그가 지연·롤백되지 않는다.
 *
 * [컬럼 제약] action_detail VARCHAR2(200) / result_code VARCHAR2(100) → 방어적 truncate.
 */
@Component
@RequiredArgsConstructor
public class CardApplicationEventLogger {

    private final EventLogService eventLogService;

    public static final String TYPE_CREDIT = "CREDIT_CARD_APPLICATION";
    public static final String TYPE_CHECK  = "CHECK_CARD_APPLICATION";

    /** 정상 분기 (event_status=SUCCESS) */
    public void log(String eventType, Long userId, Long cardId, Long appId,
                    String resultCode, String detail) {
        save(eventType, "SUCCESS", userId, cardId, appId, resultCode, detail, null);
    }

    /** 실패·거절 분기 (event_status=FAILURE) */
    public void logFailure(String eventType, Long userId, Long cardId, Long appId,
                           String resultCode, String detail, String errorMessage) {
        save(eventType, "FAILURE", userId, cardId, appId, resultCode, detail, errorMessage);
    }

    private void save(String eventType, String status, Long userId, Long cardId, Long appId,
                      String resultCode, String detail, String errorMessage) {
        EventLog parent = EventLog.builder()
                .eventType(eventType)
                .eventStatus(status)
                .userId(userId)
                .durationMs(null)
                .createdAt(LocalDateTime.now(TimeConstants.KST))
                .build();

        String action = "appId=" + appId + (detail != null && !detail.isBlank() ? " | " + detail : "");
        CardEventLog child = CardEventLog.builder()
                .cardId(cardId != null ? String.valueOf(cardId) : null)
                .actionDetail(truncateBytes(action, 200))       // action_detail VARCHAR2(200)
                .resultCode(truncateBytes(resultCode, 100))     // result_code   VARCHAR2(100)
                .errorMessage(truncateBytes(errorMessage, 2000))// error_message VARCHAR2(2000)
                .build();

        eventLogService.saveCardLog(parent, child);
    }

    /**
     * UTF-8 바이트 길이 기준 truncate. Oracle VARCHAR2(n)은 기본 BYTE 시맨틱이므로
     * 한글(3바이트)이 섞이면 문자 수가 아닌 바이트 수로 잘라야 ORA-12899를 피한다.
     * 멀티바이트 문자 중간에서 잘리지 않도록 경계를 보정한다.
     */
    private static String truncateBytes(String s, int maxBytes) {
        if (s == null) return null;
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        if (b.length <= maxBytes) return s;
        int end = maxBytes;
        while (end > 0 && (b[end] & 0xC0) == 0x80) end--; // 연속 바이트면 경계까지 뒤로
        return new String(b, 0, end, StandardCharsets.UTF_8);
    }
}
