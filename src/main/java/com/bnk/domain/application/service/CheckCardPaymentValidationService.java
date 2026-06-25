package com.bnk.domain.application.service;

import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckCardPaymentValidationService {

    /**
     * 결제 승인 검증 (7단계)
     */
    public void validate(PaymentValidationContext ctx) {

        // 1. 카드 상태 확인
        if (!"ACTIVE".equals(ctx.getCardStatus())) {
            log.warn("[Payment] 카드 상태 이상. status={}", ctx.getCardStatus());
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "사용 불가 카드입니다. 상태: " + ctx.getCardStatus());
        }

        // 2. 계좌 상태 확인
        if ("SUSPENDED".equals(ctx.getAccountStatus())
                || "CLOSED".equals(ctx.getAccountStatus())) {
            log.warn("[Payment] 계좌 상태 이상. status={}", ctx.getAccountStatus());
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "출금 불가 계좌입니다.");
        }

        // 3. 잔액 확인
        if (ctx.getBalance() < ctx.getPaymentAmount()) {
            log.warn("[Payment] 잔액 부족. balance={}, amount={}",
                    ctx.getBalance(), ctx.getPaymentAmount());
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "잔액이 부족합니다.");
        }

        // 4. 1일 사용금액 초과 여부
        long todayUsed = ctx.getTodayUsedAmount();
        long dailyLimit = ctx.getDailyLimit();
        if (todayUsed + ctx.getPaymentAmount() > dailyLimit) {
            log.warn("[Payment] 일 한도 초과. used={}, limit={}",
                    todayUsed, dailyLimit);
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    String.format("1일 한도 초과입니다. (한도: %,d원, 사용: %,d원)",
                            dailyLimit, todayUsed));
        }

        // 5. 월 사용금액 초과 여부
        long monthUsed = ctx.getMonthUsedAmount();
        long monthlyLimit = ctx.getMonthlyLimit();
        if (monthUsed + ctx.getPaymentAmount() > monthlyLimit) {
            log.warn("[Payment] 월 한도 초과. used={}, limit={}",
                    monthUsed, monthlyLimit);
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    String.format("월 한도 초과입니다. (한도: %,d원, 사용: %,d원)",
                            monthlyLimit, monthUsed));
        }

        // 6. 한도제한계좌 추가 검증
        if ("LIMIT_ACCOUNT".equals(ctx.getAccountStatus())) {
            long limitAccountDailyLimit = ctx.getLimitAccountDailyLimit();
            if (todayUsed + ctx.getPaymentAmount() > limitAccountDailyLimit) {
                log.warn("[Payment] 한도제한계좌 일 한도 초과.");
                throw new BusinessException(ErrorCode.INVALID_INPUT,
                        "한도제한계좌 1일 출금 한도를 초과했습니다.");
            }
        }

        // 7. 승인 처리
        log.info("[Payment] 결제 승인. amount={}", ctx.getPaymentAmount());
    }
}