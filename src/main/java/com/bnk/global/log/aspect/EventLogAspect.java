package com.bnk.global.log.aspect;

import java.time.LocalDateTime;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.bnk.domain.ai.dto.AiChatResponse;
import com.bnk.global.auth.CustomAdminDetails;
import com.bnk.global.auth.CustomUserDetails;
import com.bnk.global.log.annotation.Loggable;
import com.bnk.global.log.model.CardEventLog;
import com.bnk.global.log.model.ChatEventLog;
import com.bnk.global.log.model.EventLog;
import com.bnk.global.log.model.TermsEventLog;
import com.bnk.global.log.service.EventLogService;
import com.bnk.global.util.TimeConstants;

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
        long start  = System.currentTimeMillis();
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

    // ─── 성공 로그 ────────────────────────────────────────────────────
    private void saveSuccess(Loggable loggable, ProceedingJoinPoint pjp,
                             Object result, Long userId, String ip, long duration) {
        
    	// 임시 디버그
        log.warn("[DEBUG-SAVE] eventType={}, targetType={}, actionDetail={}",
            loggable.eventType(), loggable.targetType(), loggable.actionDetail());
    	
    	
    	EventLog parent = buildParent(loggable.eventType(), "SUCCESS", userId, ip, duration);

        switch (loggable.targetType()) {
<<<<<<< HEAD
	        case "CARD" -> {
	            String cardId = null;
	            if (!loggable.cardIdParam().isEmpty()) {
	                cardId = extractByName(pjp, loggable.cardIdParam());
	            }
	
	            // CARD_COMPARE일 때 비교 카드 목록을 actionDetail에 추가
	            String actionDetail = loggable.actionDetail();
	            if ("CARD_COMPARE".equals(loggable.eventType())) {
	                String compareIds = extractCompareCardIds(pjp);
	                if (compareIds != null) {
	                    actionDetail = "카드비교: " + compareIds;  // 예: "카드비교: [10101001, 10201001]"
	                }
	            }
	
	            CardEventLog child = CardEventLog.builder()
	                    .cardId(cardId)
	                    .actionDetail(actionDetail)
	                    .resultCode("SUCCESS")
	                    .build();
	            eventLogService.saveCardLog(parent, child);
	        }
	        case "TERMS" -> {
	            // termsId 파라미터 직접 없으면 request 안에서 첫 번째 termsId 추출
	            String termsIdStr = extractByName(pjp, "termsId");
	            Long termsId = null;
	            if (termsIdStr != null) {
	                termsId = Long.valueOf(termsIdStr);
	            } else {
	                // TermsAgreementRequest에서 첫 번째 termsId 꺼내기
	                termsId = extractTermsIdFromRequest(pjp);
	            }
	            TermsEventLog child = TermsEventLog.builder()
	                    .termsId(termsId)
	                    .actionDetail(loggable.actionDetail())
	                    .build();
	            eventLogService.saveTermsLog(parent, child);
	        }
=======
            case "CARD" -> {
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
                String termsIdStr = extractByName(pjp, "termsId");
                Long termsId = termsIdStr != null ? Long.valueOf(termsIdStr) : null;
                TermsEventLog child = TermsEventLog.builder()
                        .termsId(termsId)
                        .actionDetail(loggable.actionDetail())
                        .build();
                eventLogService.saveTermsLog(parent, child);
            }
>>>>>>> main
            case "CHAT" -> {
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

    // ─── 실패 로그 ────────────────────────────────────────────────────
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
                // 실패 시 에러 메시지를 eventStatus에 포함해서 추적 가능하게
                String failStatus = "FAILURE|" + truncate(e.getMessage(), 190);
                EventLog parentWithError = buildParent(loggable.eventType(), failStatus, userId, ip, duration);
                eventLogService.saveParentOnly(parentWithError);
            }
        }
    }

    // ─── 공통 유틸 ───────────────────────────────────────────────────
    private EventLog buildParent(String type, String status,
                                 Long userId, String ip, long duration) {
        return EventLog.builder()
                .eventType(type)
                .eventStatus(status)
                .userId(userId)
                .requestIp(ip)
                .durationMs(duration)
                .createdAt(LocalDateTime.now(TimeConstants.KST))
                .build();
    }

    private String extractByName(ProceedingJoinPoint pjp, String paramName) {
        if (paramName == null || paramName.isEmpty()) return null;
        
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String[] names = sig.getParameterNames();
        Object[] args  = pjp.getArgs();
        
        // 임시 디버그 로그
        log.warn("[DEBUG] 메서드: {}, 파라미터이름배열: {}", 
            sig.getMethod().getName(), 
            names == null ? "NULL" : String.join(", ", names));
        
        if (names == null) return null;
        
        for (int i = 0; i < names.length; i++) {
            if (paramName.equals(names[i]) && args[i] != null) {
                Object arg = args[i];
                log.warn("[DEBUG-EXTRACT] 파라미터명={}, 값타입={}, 값={}",
                    names[i], arg.getClass().getSimpleName(), arg);
                if (arg instanceof String || arg instanceof Long || arg instanceof Integer) {
                    return String.valueOf(arg);
                } else {
                    log.warn("[DEBUG-EXTRACT] 객체타입 차단됨: {}", arg.getClass().getSimpleName());
                }
            }
        }
        return null;
    }

    /**
     * 현재 인증 주체의 ID 추출.
     * CustomUserDetails(일반 사용자) + CustomAdminDetails(관리자) 모두 처리
     */
    private Long getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return null;
            Object principal = auth.getPrincipal();
            if (principal instanceof CustomUserDetails ud)  return ud.getUserId();
            if (principal instanceof CustomAdminDetails ad) return ad.getAdminId();
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * 클라이언트 IP 추출.
     * application.properties: server.forward-headers-strategy=framework 설정으로
     *    Spring이 XFF를 이미 처리하므로 getRemoteAddr()만 사용 (XFF 스푸핑 방어)
     */
    private String getClientIp() {
        String ip = request.getRemoteAddr();
        return ip != null && ip.length() > 100 ? ip.substring(0, 100) : ip;
    }

    private String truncate(String msg, int maxLen) {
        if (msg == null) return null;
        return msg.length() > maxLen ? msg.substring(0, maxLen) : msg;
    }
    
    private String extractCompareCardIds(ProceedingJoinPoint pjp) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String[] names = sig.getParameterNames();
        Object[] args  = pjp.getArgs();
        if (names == null) return null;
        for (int i = 0; i < names.length; i++) {
            // CardCompareRequest 타입 파라미터 찾기
            if (args[i] != null && args[i].getClass().getSimpleName().equals("CardCompareRequest")) {
                try {
                    // getCardIds() 리플렉션으로 호출
                    Object cardIds = args[i].getClass()
                            .getMethod("getCardIds")
                            .invoke(args[i]);
                    return String.valueOf(cardIds);  // "[10101001, 10201001]"
                } catch (Exception ignored) {}
            }
        }
        return null;
    }
    
    private Long extractTermsIdFromRequest(ProceedingJoinPoint pjp) {
        for (Object arg : pjp.getArgs()) {
            if (arg == null) continue;
            try {
                // getAgreedTerms().get(0).getTermsId() 시도
                Object agreedTerms = arg.getClass()
                        .getMethod("getAgreedTerms")
                        .invoke(arg);
                if (agreedTerms instanceof java.util.List<?> list && !list.isEmpty()) {
                    Object firstItem = list.get(0);
                    Object termsId = firstItem.getClass()
                            .getMethod("getTermsId")
                            .invoke(firstItem);
                    if (termsId instanceof Long l) return l;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }
}