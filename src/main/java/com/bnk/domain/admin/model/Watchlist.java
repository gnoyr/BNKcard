package com.bnk.domain.admin.model;

import lombok.*;
import java.time.LocalDateTime;

/**
 * WATCHLIST 테이블 모델.
 * 미가입 요주의 인물 사전 차단 목록.
 *
 * [해시 컬럼 설명]
 * ciValue, birthDate는 AES-256-GCM 암호화 저장 → DB 직접 비교 불가.
 * ciValueHash, birthDateHash에 SHA-256 결정론적 해시를 함께 저장하여
 * 인덱스 기반 조회를 가능하게 한다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Watchlist {
    private Long          watchlistId;
    private String        name;
    private String        birthDate;      // AES 복호화 후 "YYYY-MM-DD"
    private String        ciValue;        // AES 복호화 후 원본 CI값
    private String        ciValueHash;    // SHA-256(ciValue)   — 인덱스 조회용
    private String        birthDateHash;  // SHA-256(birthDate) — 인덱스 조회용
    private String        reason;
    private String        riskLevel;      // HIGH / MEDIUM
    private LocalDateTime registeredAt;
    private Long          registeredBy;
    private String        deletedYn;
}