package com.bnk.global.log.aspect;

import com.bnk.domain.ai.dto.AiChatResponse;
import com.bnk.global.log.annotation.Loggable;
import com.bnk.global.log.model.*;
import com.bnk.global.log.service.EventLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class EventLogAspect {

    private final EventLogService eventLogService;
    private final HttpServletRequest request;

    @Around("@annotation(loggable)")
    public Object around(ProceedingJoinPoint pjp, Loggable loggable) throws Throwable {
        long start = System.currentTimeMillis();
        Long userId = getCurrentUserId();
        String ip   = getClientIp();

        try {
            Object result = pjp.proceed();
            long duration = System.currentTimeMillis() - start;
            saveSuccess(loggable, pjp, result, userId, ip, duration);
            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            saveFailure(loggable, pjp, e, userId, ip, duration);
            throw e;
        }
    }

    // ─── 성공 로그 ───
    private void saveSuccess(Loggable loggable, ProceedingJoinPoint pjp,
                             Object result, Long userId, String ip, long duration) {
        EventLog parent = buildParent(loggable.eventType(), "SUCCESS", userId, ip, duration);

        switch (loggable.targetType()) {
	        case "CARD" -> {
	            // cardIdParam이 비어있으면 cardId 추출 시도 안 함
	            String cardId = null;
	            if (!loggable.cardIdParam().isEmpty()) {
	                cardId = extractByName(pjp, loggable.cardIdParam());
	            }
	            CardEventLog child = CardEventLog.builder()
	                    .cardId(cardId)
	                    .actionDetail(loggable.actionDetail())
	                    .resultCode("SUCCESS")
	                    .build();
	            eventLogService.saveCardLog(parent, child);
	        }
            case "TERMS" -> {
                // termsId 파라미터 이름으로 찾기
                String termsIdStr = extractByName(pjp, "termsId");
                Long termsId = termsIdStr != null ? Long.valueOf(termsIdStr) : null;
                TermsEventLog child = TermsEventLog.builder()
                        .termsId(termsId)
                        .actionDetail(loggable.actionDetail())
                        .build();
                eventLogService.saveTermsLog(parent, child);
            }
            case "CHAT" -> {
                // query 또는 message 파라미터 이름으로 찾기
                String queryText = extractByName(pjp, "query");
                if (queryText == null) queryText = extractByName(pjp, "message");

                ChatEventLog child = ChatEventLog.builder()
                        .usedFallback("N")
                        .build();
                if (result instanceof AiChatResponse res) {
                    child.setSessionId(res.getSessionId());
                }
                child.setQueryText(truncate(queryText, 2000));
                eventLogService.saveChatLog(parent, child);
            }
            case "APPROVAL" -> {
                eventLogService.saveParentOnly(parent);
            }
        }
    }
    // 실시리시리시ㅣㅁㄹㄴㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇ
    // ─── 실패 로그 ───
    private void saveFailure(Loggable loggable, ProceedingJoinPoint pjp,
                             Exception e, Long userId, String ip, long duration) {
        EventLog parent = buildParent(loggable.eventType(), "FAILURE", userId, ip, duration);

        switch (loggable.targetType()) {
	        case "CARD" -> {
	            String cardId = null;
	            if (!loggable.cardIdParam().isEmpty()) {
	                cardId = extractByName(pjp, loggable.cardIdParam());
	            }
	            CardEventLog child = CardEventLog.builder()
	                    .cardId(cardId)
	                    .actionDetail(loggable.actionDetail())
	                    .resultCode("SYSTEM_ERROR")
	                    .errorMessage(truncate(e.getMessage(), 1000))
	                    .build();
	            eventLogService.saveCardLog(parent, child);
	        }
            case "TERMS" -> {
                TermsEventLog child = TermsEventLog.builder()
                        .actionDetail(loggable.actionDetail())
                        .errorMessage(truncate(e.getMessage(), 1000))
                        .build();
                eventLogService.saveTermsLog(parent, child);
            }
            case "CHAT" -> {
                String queryText = extractByName(pjp, "query");
                if (queryText == null) queryText = extractByName(pjp, "message");
                ChatEventLog child = ChatEventLog.builder()
                        .queryText(truncate(queryText, 2000))
                        .errorMessage(truncate(e.getMessage(), 1000))
                        .build();
                eventLogService.saveChatLog(parent, child);
            }
            case "APPROVAL" -> {
                eventLogService.saveParentOnly(parent);
            }
        }
    }

    // ─── 공통 유틸 ───
    private EventLog buildParent(String type, String status,
                                  Long userId, String ip, long duration) {
        return EventLog.builder()
                .eventType(type)
                .eventStatus(status)
                .userId(userId)
                .requestIp(ip)
                .durationMs(duration)
                .build();
    }

    /**
     * 파라미터 이름으로 값 찾기 — Long/String 모두 String으로 반환
     * 타입 기반 extractArg 대신 이걸로 통일
     */
    private String extractByName(ProceedingJoinPoint pjp, String paramName) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String[] names = sig.getParameterNames();
        Object[] args  = pjp.getArgs();
        for (int i = 0; i < names.length; i++) {
            if (paramName.equals(names[i]) && args[i] != null) {
                return String.valueOf(args[i]);
            }
        }
        return null;
    }

    private Long getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return null;
            Object principal = auth.getPrincipal();
            if (principal instanceof com.bnk.global.auth.CustomUserDetails ud) {
                return ud.getUserId();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String getClientIp() {
        String xff = request.getHeader("X-Forwarded-For");
        String ip = (xff != null && !xff.isBlank())
                ? xff.split(",")[0].trim()
                : request.getRemoteAddr();
        // 100자 초과 방지
        return ip != null && ip.length() > 100 ? ip.substring(0, 100) : ip;
    }

    // 길이 제한 truncate — 컬럼별 다른 길이 적용
    private String truncate(String msg, int maxLen) {
        if (msg == null) return null;
        return msg.length() > maxLen ? msg.substring(0, maxLen) : msg;
    }
}