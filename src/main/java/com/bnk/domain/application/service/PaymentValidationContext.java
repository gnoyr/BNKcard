package com.bnk.domain.application.service;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentValidationContext {
    private final String cardStatus;         // ACTIVE / LOST / STOPPED
    private final String accountStatus;      // NORMAL / LIMIT_ACCOUNT / SUSPENDED
    private final long   balance;            // 현재 잔액
    private final long   paymentAmount;      // 결제 요청 금액
    private final long   todayUsedAmount;    // 오늘 사용 금액
    private final long   monthUsedAmount;    // 이번 달 사용 금액
    private final long   dailyLimit;         // 일 한도
    private final long   monthlyLimit;       // 월 한도
    private final long   limitAccountDailyLimit; // 한도제한계좌 일 한도
}