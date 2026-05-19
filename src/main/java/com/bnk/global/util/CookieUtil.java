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

    /** Access Token 쿠키 생성 — 전체 경로, SameSite=Lax */
    public ResponseCookie createAccessCookie(String token, long maxAgeSec) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(maxAgeSec)
                .sameSite("Lax")
                .build();
    }

    /** Refresh Token 쿠키 생성 — 재발급 경로만, SameSite=Strict */
    public ResponseCookie createRefreshCookie(String token, long maxAgeSec) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(secure)
                .path("/api/auth/refresh")
                .maxAge(maxAgeSec)
                .sameSite("Strict")
                .build();
    }

    /** Access Token 쿠키 삭제 */
    public ResponseCookie deleteAccessCookie() {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
    }

    /** Refresh Token 쿠키 삭제 */
    public ResponseCookie deleteRefreshCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(secure)
                .path("/api/auth/refresh")
                .maxAge(0)
                .sameSite("Strict")
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