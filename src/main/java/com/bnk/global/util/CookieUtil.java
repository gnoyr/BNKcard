package com.bnk.global.util;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class CookieUtil {

    public static final String ACCESS_TOKEN_COOKIE  = "access_token";
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    @Value("${cookie.secure:false}")
    private boolean secure;

    /**
     * Access Token 쿠키 생성 — 전체 경로, SameSite=Lax
     */
    public ResponseCookie createAccessCookie(String token, long maxAgeSec) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(maxAgeSec)
                .sameSite("Lax")
                .build();
    }

    /**
     * Refresh Token 쿠키 생성.
     *
     *   - 일반 재발급: POST /api/auth/refresh    → /api/auth 하위 
     *   - 관리자 재발급: POST /api/admin/auth/refresh  → 별도 처리 필요
     *   - 관리자 로그아웃: POST /api/admin/auth/logout 
     *
     *   SameSite: "Strict" → "Lax"
     *   이유: Strict는 외부 링크를 통해 사이트 진입 시 쿠키를 전송하지 않아
     *         정상적인 재발급 흐름이 차단되는 케이스 발생.
     */
    public ResponseCookie createRefreshCookie(String token, long maxAgeSec) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(secure)
                .path("/api/auth")    
                .maxAge(maxAgeSec)
                .sameSite("Lax")            // 수정: Strict → Lax
                .build();
    }

    /**
     * Access Token 쿠키 삭제.
     * createAccessCookie()와 path/sameSite 동일해야 브라우저에서 정상 삭제됨.
     */
    public ResponseCookie deleteAccessCookie() {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
    }

    /**
     * Refresh Token 쿠키 삭제.
     *   발급 path와 삭제 path가 다르면 브라우저가 삭제 Set-Cookie를 무시함.
     */
    public ResponseCookie deleteRefreshCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(secure)
                .path("/api/auth")          // 수정: createRefreshCookie path와 동일하게
                .maxAge(0)
                .sameSite("Lax")
                .build();
    }

    /**
     * ("/api/auth/refresh")로 발급된 Refresh 쿠키 삭제용.
     * path 변경 배포 직후 과도기 동안 브라우저에 구 path 쿠키가 남아있을 수 있으므로
     * 로그아웃 시 함께 전송하여 잔류 쿠키를 제거.
     * 구 쿠키가 모두 만료된 후(refresh-expiration 7일 경과) 제거 가능.
     */
    public ResponseCookie deleteLegacyRefreshCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(secure)
                .path("/api/auth/refresh")  // 구 path 명시
                .maxAge(0)
                .sameSite("Lax")
                .build();
    }

    public static Optional<String> extractCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
