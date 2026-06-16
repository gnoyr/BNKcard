package com.bnk.domain.ipauth.dto.response;

import com.bnk.domain.ipauth.model.UserTrustedIp;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * GET /api/users/me/trusted-ips 응답 DTO
 *
 * [IP 마스킹 규칙]
 *  IPv4 : 앞 두 옥텟만 표시, 나머지 마스킹
 *         예) 192.168.1.100 → 192.168.*.*
 *  IPv6 : 앞 두 그룹만 표시, 나머지 마스킹
 *         예) 2001:db8:85a3::1 → 2001:db8:*:*:*:*:*:*
 *  null : null 반환
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrustedIpResponse {

    private Long          trustId;
    private String        ipAddressMasked;
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

    /**
     * IP 마스킹 유틸.
     *
     * IPv4: 앞 두 옥텟 표시, 세 번째·네 번째 마스킹
     *   192.168.1.100 → 192.168.*.*
     *
     * IPv6: 앞 두 그룹 표시, 나머지 마스킹 (8그룹 기준)
     *   2001:db8::1   → 2001:db8:*:*:*:*:*:*
     *   ::1           → *:*:*:*:*:*:*:*  (루프백도 마스킹)
     *
     * 기타(알 수 없는 형식): 전체 마스킹
     */
    private static String maskIp(String ip) {
        if (ip == null) return null;

        // IPv4 판별: 점(.)이 3개이고 콜론 없음
        if (ip.contains(".") && !ip.contains(":")) {
            String[] parts = ip.split("\\.", -1);
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + ".*.*";
            }
        }

        // IPv6 판별: 콜론 포함
        if (ip.contains(":")) {
            // :: 축약 표현을 8그룹으로 완전 전개
            String expanded = expandIPv6(ip);
            if (expanded != null) {
                String[] groups = expanded.split(":", -1);
                if (groups.length == 8) {
                    return groups[0] + ":" + groups[1] + ":*:*:*:*:*:*";
                }
            }
            // 전개 실패 시 첫 번째 그룹만 표시
            int firstColon = ip.indexOf(':');
            if (firstColon > 0) {
                return ip.substring(0, firstColon) + ":*:*:*:*:*:*:*";
            }
        }

        // 알 수 없는 형식 → 전체 마스킹
        return "*.*.*.*";
    }

    /**
     * IPv6 :: 축약 표현을 8그룹 완전 표현으로 전개.
     * 예) 2001:db8::1 → 2001:0db8:0000:0000:0000:0000:0000:0001
     * 전개 실패 시 null 반환.
     */
    private static String expandIPv6(String ip) {
        try {
            // :: 가 있으면 빠진 그룹을 0000으로 채움
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
            // :: 없으면 그대로
            String[] parts = ip.split(":", -1);
            return parts.length == 8 ? ip : null;
        } catch (Exception e) {
            return null;
        }
    }
}