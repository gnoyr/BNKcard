package com.bnk.global.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
public class CookieUtil {

    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    /** application.properties: cookie.secure=false (local), true (prod) */
    @Value("${cookie.secure:false}")
    private boolean secure;

    /**
     * HttpOnly Refresh Token 쿠키 생성.
     * path=/api/auth/refresh 로 제한 → Refresh 엔드포인트에만 전송
     */
    public ResponseCookie createRefreshCookie(String token, long maxAgeSec) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(secure)
                .path("/api/auth/refresh")
                .maxAge(maxAgeSec)
                .sameSite("Strict")
                .build();
    }

    /** 쿠키 삭제 — maxAge=0 */
    public ResponseCookie deleteRefreshCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(secure)
                .path("/api/auth/refresh")
                .maxAge(0)
                .sameSite("Strict")
                .build();
    }

    /** 요청에서 특정 이름의 쿠키 값 추출 (static — 필터에서도 사용 가능) */
    public static Optional<String> extractCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    /** Refresh Token 쿠키 추출 */
    public static Optional<String> extractRefreshToken(HttpServletRequest request) {
        return extractCookieValue(request, REFRESH_TOKEN_COOKIE);
    }
}
