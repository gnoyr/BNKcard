package com.bnk.global.log.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.bnk.domain.ai.dto.AiChatResponse;
import com.bnk.global.log.annotation.Loggable;
import com.bnk.global.log.model.CardEventLog;
import com.bnk.global.log.model.ChatEventLog;
import com.bnk.global.log.model.EventLog;
import com.bnk.global.log.model.TermsEventLog;
import com.bnk.global.log.service.EventLogService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
                String cardId = extractArg(pjp, String.class);
                CardEventLog child = CardEventLog.builder()
                        .cardId(cardId)
                        .actionDetail(loggable.actionDetail())
                        .resultCode("SUCCESS")
                        .build();
                eventLogService.saveCardLog(parent, child);
            }
            case "TERMS" -> {
                Long termsId = extractArg(pjp, Long.class);
                TermsEventLog child = TermsEventLog.builder()
                        .termsId(termsId)
                        .actionDetail(loggable.actionDetail())
                        .build();
                eventLogService.saveTermsLog(parent, child);
            }
            case "CHAT" -> {
                // AiChatResponse에서 상세 정보 꺼내기
                ChatEventLog child = ChatEventLog.builder()
                        .usedFallback("N")
                        .build();
                if (result instanceof AiChatResponse res) {
                    child.setSessionId(res.getSessionId());
                    child.setQueryText(extractArg(pjp, String.class));
                }
                eventLogService.saveChatLog(parent, child);
            }
            case "APPROVAL" -> {
                // 자식 테이블 없음 — 부모만 저장
                eventLogService.saveParentOnly(parent);
            }
        }
    }

    // ─── 실패 로그 ───
    private void saveFailure(Loggable loggable, ProceedingJoinPoint pjp,
                             Exception e, Long userId, String ip, long duration) {
        EventLog parent = buildParent(loggable.eventType(), "FAILURE", userId, ip, duration);

        switch (loggable.targetType()) {
            case "CARD" -> {
                String cardId = extractArg(pjp, String.class);
                CardEventLog child = CardEventLog.builder()
                        .cardId(cardId)
                        .actionDetail(loggable.actionDetail())
                        .resultCode("SYSTEM_ERROR")
                        .errorMessage(truncate(e.getMessage()))
                        .build();
                eventLogService.saveCardLog(parent, child);
            }
            case "TERMS" -> {
                TermsEventLog child = TermsEventLog.builder()
                        .actionDetail(loggable.actionDetail())
                        .errorMessage(truncate(e.getMessage()))
                        .build();
                eventLogService.saveTermsLog(parent, child);
            }
            case "CHAT" -> {
                ChatEventLog child = ChatEventLog.builder()
                        .queryText(extractArg(pjp, String.class))
                        .errorMessage(truncate(e.getMessage()))
                        .build();
                eventLogService.saveChatLog(parent, child);
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
        return (xff != null && !xff.isBlank())
                ? xff.split(",")[0].trim()
                : request.getRemoteAddr();
    }

    private <T> T extractArg(ProceedingJoinPoint pjp, Class<T> type) {
        for (Object arg : pjp.getArgs()) {
            if (arg != null && type.isInstance(arg)) return type.cast(arg);
        }
        return null;
    }

    private String truncate(String msg) {
        if (msg == null) return null;
        return msg.length() > 500 ? msg.substring(0, 500) : msg;
    }
}