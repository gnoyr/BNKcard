package com.bnk.domain.ipauth.dto.response;

import com.bnk.domain.ipauth.model.UserTrustedIp;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * GET /api/users/me/trusted-ips 응답 DTO
 * ipAddress는 마스킹 처리하여 반환 (평문 IP 미노출)
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrustedIpResponse {

    private Long          trustId;
    private String        ipAddressMasked;  // 192.168.*.100
    private String        nickname;
    private boolean       isInitial;
    private String        statusCode;
    private LocalDateTime lastUsedAt;
    private String        registeredVia;
    private LocalDateTime createdAt;

    public static TrustedIpResponse from(UserTrustedIp model) {
        return TrustedIpResponse.builder()
                .trustId(model.getTrustId())
                .ipAddressMasked(maskIp(model.getIpAddress()))
                .nickname(model.getNickname())
                .isInitial("Y".equals(model.getIsInitial()))
                .statusCode(model.getStatusCode())
                .lastUsedAt(model.getLastUsedAt())
                .registeredVia(model.getRegisteredVia())
                .createdAt(model.getCreatedAt())
                .build();
    }

    /** IPv4: 세 번째 옥텟 마스킹. IPv6: 그대로 반환 */
    private static String maskIp(String ip) {
        if (ip == null) return null;
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + ".*." + parts[3];
        }
        return ip;
    }
}
