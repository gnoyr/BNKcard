package com.bnk.domain.ipauth.model;

import lombok.*;
import java.time.LocalDateTime;

/**
 * USER_TRUSTED_IPS 테이블 매핑 모델 (MyBatis)
 *
 * [암호화 컬럼]
 * ipAddress     : AES-256-GCM 암호화 저장
 *                 IpTrustMapper.xml resultMap → typeHandler=AesTypeHandler → 조회 시 자동 복호화
 * ipAddressHash : SHA-256(평문 IP) hex — TypeHandler 없음, 평문 저장/조회
 *                 WHERE 조건 및 UNIQUE 제약 사용 (Watchlist.ciValueHash 패턴 동일)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTrustedIp {

    private Long          trustId;
    private Long          userId;
    private String        ipAddress;        // AES 복호화 후 평문
    private String        ipAddressHash;    // SHA-256 hex — 인덱스/UNIQUE 제약용
    private String        nickname;
    private String        isInitial;        // Y / N
    private String        statusCode;       // ACTIVE / DISABLED
    private LocalDateTime lastUsedAt;
    private String        registeredVia;    // SIGNUP / EMAIL_VERIFY / CI_VERIFY / ADMIN
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String        deletedYn;
    private LocalDateTime deletedAt;

    public boolean isInitialDevice() {
        return "Y".equals(this.isInitial);
    }
}
