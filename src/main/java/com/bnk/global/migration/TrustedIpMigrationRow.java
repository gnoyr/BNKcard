package com.bnk.global.migration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * USER_TRUSTED_IPS 암호화 마이그레이션 전용 DTO.
 * TypeHandler 없이 raw String 그대로 수신.
 */
@Getter
@Setter
@NoArgsConstructor
public class TrustedIpMigrationRow {
    private Long   trustId;
    private String ipAddress;
}