package com.bnk.domain.application.policy;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CheckCardLimitPolicy {

    // 성인 기본
    ADULT_BASIC(5_000_000L, 20_000_000L, "성인 기본 한도"),
    // 성인 우수
    ADULT_PREMIUM(6_000_000L, 30_000_000L, "성인 우수 한도"),
    // 청소년 기본
    TEEN_BASIC(30_000L, 300_000L, "청소년 기본 한도"),
    // 청소년 실적 보유
    TEEN_ACTIVE(100_000L, 1_000_000L, "청소년 실적 한도"),
    // 한도제한 기본
    LIMIT_ACCOUNT_BASIC(300_000L, 300_000L, "한도제한 기본"),
    // 한도제한 완화
    LIMIT_ACCOUNT_RELAXED(1_000_000L, 1_000_000L, "한도제한 완화");

    private final long   dailyLimit;
    private final long   monthlyLimit;
    private final String description;
}