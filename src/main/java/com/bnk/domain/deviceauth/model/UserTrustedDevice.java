package com.bnk.domain.deviceauth.model;

import lombok.*;
import java.time.LocalDateTime;

/**
 * USER_TRUSTED_DEVICES 테이블 매핑 모델 (MyBatis)
 *
 * [신뢰 판정 전략]
 * 기존 IP 해시 대신 기기 식별자(device_id) 해시로 신뢰 여부를 판정한다.
 * 모바일 IP 변동(WiFi↔LTE)에도 같은 기기는 재인증하지 않는다.
 *
 * [암호화 컬럼]
 * lastIpAddress : AES-256-GCM 암호화 저장
 *                 DeviceTrustMapper.xml resultMap → typeHandler=AesTypeHandler → 조회 시 자동 복호화
 * deviceIdHash  : SHA-256 hex — TypeHandler 없음, 평문 저장/조회 (WHERE 조건 및 신뢰 판정 키)
 * lastIpHash    : SHA-256 hex — 새 IP 접속 감지용
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTrustedDevice {

    private Long          deviceTrustId;
    private Long          userId;
    private String        deviceIdHash;     // SHA-256(기기 UUID) — 신뢰 판정 키
    private String        deviceName;       // 표시용 기기명 (사용자 편집 가능)
    private String        platformCode;     // IOS / ANDROID / WEB / UNKNOWN
    private String        lastIpAddress;    // AES 복호화 후 평문
    private String        lastIpHash;       // SHA-256 hex
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
