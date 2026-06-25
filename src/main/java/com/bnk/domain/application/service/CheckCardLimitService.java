package com.bnk.domain.application.service;

import com.bnk.domain.application.policy.CheckCardLimitContext;
import com.bnk.domain.application.policy.CheckCardLimitPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckCardLimitService {

    /**
     * 체크카드 한도 산정
     * @param ctx 한도 산정에 필요한 조건 정보
     * @return 적용할 한도 정책
     */
    public CheckCardLimitPolicy determineLimit(CheckCardLimitContext ctx) {

        // ── 1. 한도제한계좌 처리 ──────────────────────────────────
        if ("LIMIT_ACCOUNT".equals(ctx.getAccountStatus())) {
            return handleLimitAccount(ctx);
        }

        // ── 2. 연령 기준 처리 ────────────────────────────────────
        if (ctx.getAge() >= 19) {
            return handleAdult(ctx);
        }
        if (ctx.getAge() >= 12) {
            return handleTeen(ctx);
        }

        // 만 12세 미만 → 발급 불가 (호출 전에 차단해야 함)
        throw new IllegalStateException("만 12세 미만은 체크카드 발급 불가");
    }

    // ── 한도제한계좌 ──────────────────────────────────────────────
    private CheckCardLimitPolicy handleLimitAccount(CheckCardLimitContext ctx) {
        int condCount = ctx.limitRelaxConditionCount();

        // 조건 2개 이상 → 일반계좌 전환 → 연령 기준 적용
        if (condCount >= 2) {
            log.info("[LimitAccount] 일반계좌 전환 조건 충족. count={}", condCount);
            // 실제 구현 시 ACCOUNTS.account_status 업데이트 필요
            return ctx.getAge() >= 19
                    ? handleAdult(ctx)
                    : handleTeen(ctx);
        }

        // 조건 1개 이상 → 완화 한도
        if (condCount >= 1) {
            log.info("[LimitAccount] 완화 한도 적용. count={}", condCount);
            return CheckCardLimitPolicy.LIMIT_ACCOUNT_RELAXED;
        }

        // 조건 없음 → 제한 한도
        log.info("[LimitAccount] 제한 한도 적용.");
        return CheckCardLimitPolicy.LIMIT_ACCOUNT_BASIC;
    }

    // ── 성인 ──────────────────────────────────────────────────────
    private CheckCardLimitPolicy handleAdult(CheckCardLimitContext ctx) {
        int condCount = ctx.premiumConditionCount();

        if (condCount >= 2) {
            log.info("[Adult] 우수 한도 적용. count={}", condCount);
            return CheckCardLimitPolicy.ADULT_PREMIUM;
        }

        log.info("[Adult] 기본 한도 적용. count={}", condCount);
        return CheckCardLimitPolicy.ADULT_BASIC;
    }

    // ── 청소년 ────────────────────────────────────────────────────
    private CheckCardLimitPolicy handleTeen(CheckCardLimitContext ctx) {
        if (ctx.isHasCardUsage3M()) {
            log.info("[Teen] 실적 한도 적용.");
            return CheckCardLimitPolicy.TEEN_ACTIVE;
        }

        log.info("[Teen] 기본 한도 적용.");
        return CheckCardLimitPolicy.TEEN_BASIC;
    }
}