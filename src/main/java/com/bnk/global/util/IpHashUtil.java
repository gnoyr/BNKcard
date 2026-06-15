package com.bnk.global.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * IP 주소 SHA-256 해시 유틸.
 *
 * AES-256-GCM은 IV가 매번 달라 동일 IP도 다른 암호문 생성 → WHERE 직접 비교 불가.
 * Watchlist의 ci_value_hash / birth_date_hash 패턴과 동일하게
 * SHA-256 결정론적 해시를 ip_address_hash 컬럼에 병행 저장하여 인덱스 조회.
 *
 * CddService.sha256Hex() 구현과 동일.
 */
@Slf4j
@Component
public class IpHashUtil {

    public String hash(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            throw new IllegalArgumentException("IP 주소는 null이거나 빈 값일 수 없습니다.");
        }
        try {
            byte[] hashBytes = MessageDigest.getInstance("SHA-256")
                    .digest(ipAddress.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            log.error("[IpHash] SHA-256 해시 생성 실패 ip={}", ipAddress, e);
            throw new IllegalStateException("IP 해시 생성 실패", e);
        }
    }
}
