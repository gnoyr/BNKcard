package com.bnk.domain.deviceauth.dto.response;

import com.bnk.domain.deviceauth.model.UserTrustedDevice;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * GET /api/users/me/trusted-devices 응답 DTO
 *
 * 마지막 접속 IP는 마스킹하여 표시한다.
 *  IPv4 : 192.168.1.100 → 192.168.*.*
 *  IPv6 : 2001:db8::1   → 2001:db8:*:*:*:*:*:*
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrustedDeviceResponse {

    private Long          deviceTrustId;
    private String        deviceName;
    private String        platformCode;
    private String        lastIpMasked;
    private boolean       isInitial;
    private String        statusCode;
    private LocalDateTime lastUsedAt;
    private String        registeredVia;
    private LocalDateTime createdAt;

    public static TrustedDeviceResponse from(UserTrustedDevice model) {
        return TrustedDeviceResponse.builder()
                .deviceTrustId(model.getDeviceTrustId())
                .deviceName(model.getDeviceName())
                .platformCode(model.getPlatformCode())
                .lastIpMasked(maskIp(model.getLastIpAddress()))
                .isInitial("Y".equals(model.getIsInitial()))
                .statusCode(model.getStatusCode())
                .lastUsedAt(model.getLastUsedAt())
                .registeredVia(model.getRegisteredVia())
                .createdAt(model.getCreatedAt())
                .build();
    }

    /**
     * IP 마스킹 유틸.
     * IPv4: 앞 두 옥텟 표시 → 192.168.*.*
     * IPv6: 앞 두 그룹 표시 → 2001:db8:*:*:*:*:*:*
     * 기타/알 수 없는 형식 → 전체 마스킹
     */
    private static String maskIp(String ip) {
        if (ip == null) return null;

        // IPv4 판별: 점(.) 포함, 콜론 없음
        if (ip.contains(".") && !ip.contains(":")) {
            String[] parts = ip.split("\\.", -1);
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + ".*.*";
            }
        }

        // IPv6 판별: 콜론 포함
        if (ip.contains(":")) {
            String expanded = expandIPv6(ip);
            if (expanded != null) {
                String[] groups = expanded.split(":", -1);
                if (groups.length == 8) {
                    return groups[0] + ":" + groups[1] + ":*:*:*:*:*:*";
                }
            }
            int firstColon = ip.indexOf(':');
            if (firstColon > 0) {
                return ip.substring(0, firstColon) + ":*:*:*:*:*:*:*";
            }
        }

        return "*.*.*.*";
    }

    private static String expandIPv6(String ip) {
        try {
            if (ip.contains("::")) {
                String[] halves = ip.split("::", -1);
                String left  = halves.length > 0 && !halves[0].isEmpty() ? halves[0] : "";
                String right = halves.length > 1 && !halves[1].isEmpty() ? halves[1] : "";

                String[] leftParts  = left.isEmpty()  ? new String[0] : left.split(":",  -1);
                String[] rightParts = right.isEmpty() ? new String[0] : right.split(":", -1);

                int missing = 8 - leftParts.length - rightParts.length;
                if (missing < 0) return null;

                StringBuilder sb = new StringBuilder();
                for (String p : leftParts)  { if (sb.length() > 0) sb.append(':'); sb.append(p); }
                for (int i = 0; i < missing; i++) { if (sb.length() > 0) sb.append(':'); sb.append("0000"); }
                for (String p : rightParts) { if (sb.length() > 0) sb.append(':'); sb.append(p); }
                return sb.toString();
            }
            String[] parts = ip.split(":", -1);
            return parts.length == 8 ? ip : null;
        } catch (Exception e) {
            return null;
        }
    }
}
