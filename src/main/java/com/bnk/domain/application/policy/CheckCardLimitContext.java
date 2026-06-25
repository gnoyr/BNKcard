package com.bnk.domain.application.policy;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CheckCardLimitContext {

    private final int     age;               // 만 나이
    private final String  accountStatus;     // NORMAL / LIMIT_ACCOUNT

    // 우수 거래 조건
    private final boolean hasSalaryTransfer;      // 급여이체 등록
    private final boolean hasAutoTransfer3M;       // 최근 3개월 자동이체 실적
    private final boolean hasCardUsage3M;          // 최근 3개월 카드 사용 실적
    private final boolean hasSavingsProduct;       // 적금/예금 가입

    // 한도제한 완화 조건
    private final boolean hasSalaryTransfer1M;     // 급여이체 1개월 이상
    private final boolean hasAutoTransfer3Count;   // 자동이체 3건 이상
    private final boolean hasSavingsAutoTransfer3; // 적금 자동이체 3회 이상
    private final boolean hasCardUsage3M2;         // 카드 사용 실적 3개월 이상

    // 조건 카운트 계산
    public int premiumConditionCount() {
        int count = 0;
        if (hasSalaryTransfer)   count++;
        if (hasAutoTransfer3M)   count++;
        if (hasCardUsage3M)      count++;
        if (hasSavingsProduct)   count++;
        return count;
    }

    public int limitRelaxConditionCount() {
        int count = 0;
        if (hasSalaryTransfer1M)      count++;
        if (hasAutoTransfer3Count)    count++;
        if (hasSavingsAutoTransfer3)  count++;
        if (hasCardUsage3M2)          count++;
        return count;
    }
}