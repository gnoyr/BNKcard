package com.bnk.global.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 클라이언트 IP 추출/정규화 유틸.
 *
 * IP 신뢰(2단계 인증)는 SHA-256(IP) 해시로 동일 기기를 판별한다.
 * 따라서 같은 단말이라도 IP "문자열 표현"이 요청마다 달라지면
 * 매번 '새 기기'로 오인되어 IP 인증을 반복 요구하게 된다.
 *
 * 대표적으로 로컬/에뮬레이터 환경에서 루프백이
 *   - IPv4            : 127.0.0.1
 *   - IPv6            : 0:0:0:0:0:0:0:1  또는 ::1
 *   - IPv6 매핑 IPv4  : ::ffff:127.0.0.1
 * 처럼 요청마다 섞여 들어와 해시가 달라진다.
 * 이를 하나의 표준형으로 정규화해 동일 기기 판별을 안정화한다.
 *
 * 주의: 보안(스푸핑) 위험이 있는 X-Forwarded-For 등 프록시 헤더는 신뢰하지 않고,
 *       서버가 직접 본 소켓 원격주소(getRemoteAddr)만 사용한다. 표현형만 통일한다.
 */
public final class ClientIpUtil {

    private ClientIpUtil() {}

    private static final String IPV4_LOOPBACK = "127.0.0.1";

    /** HttpServletRequest 의 소켓 원격주소를 추출 후 정규화한다. */
    public static String resolve(HttpServletRequest request) {
        return normalize(request.getRemoteAddr());
    }

    /** IP 문자열을 해시 매칭이 안정적이도록 표준형으로 정규화한다. */
    public static String normalize(String ip) {
        if (ip == null || ip.isBlank()) {
            return ip;
        }
        String v = ip.trim();

        // IPv6 매핑 IPv4 (예: ::ffff:127.0.0.1, ::ffff:192.168.0.5) → 뒤쪽 IPv4 표기만 사용
        if (v.regionMatches(true, 0, "::ffff:", 0, 7) && v.indexOf('.') > 0) {
            v = v.substring(v.lastIndexOf(':') + 1);
        }

        // IPv6 루프백 → IPv4 루프백으로 통일
        if ("::1".equals(v)
                || "0:0:0:0:0:0:0:1".equals(v)
                || "0000:0000:0000:0000:0000:0000:0000:0001".equals(v)) {
            return IPV4_LOOPBACK;
        }
        return v;
    }
}
