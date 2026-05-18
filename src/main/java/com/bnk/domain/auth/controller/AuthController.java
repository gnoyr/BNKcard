package com.bnk.domain.auth.controller;

import com.bnk.domain.auth.dto.request.*;
import com.bnk.domain.auth.dto.response.FindIdResponse;
import com.bnk.domain.auth.dto.response.TokenResponse;
import com.bnk.domain.auth.service.AuthService;
import com.bnk.global.auth.CustomUserDetails;
import com.bnk.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 회원가입 */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Long>> signup(
            @RequestBody @Valid SignupRequest request) {
        Long userId = authService.signup(request);
        return ApiResponse.toCreated(userId);
    }

    /** 이메일 인증 */
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestBody @Valid EmailVerifyRequest request) {
        authService.verifyEmail(request);
        return ApiResponse.toOk(null);
    }

    /**
     * 로그인.
     * Access Token → 응답 바디.
     * Refresh Token → HttpOnly 쿠키(Set-Cookie).
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletResponse response) {

        var result = authService.login(request);          // Pair<TokenResponse, ResponseCookie>
        response.addHeader(HttpHeaders.SET_COOKIE, result.getCookie().toString());
        return ApiResponse.toOk(result.getToken());
    }

    /**
     * Access Token 재발급.
     * @CookieValue 로 HttpOnly 쿠키에서 Refresh Token 직접 수신.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @CookieValue(name = "refresh_token") String refreshToken,
            HttpServletResponse response) {

        var result = authService.refresh(refreshToken);   // Pair<TokenResponse, ResponseCookie>
        response.addHeader(HttpHeaders.SET_COOKIE, result.getCookie().toString());
        return ApiResponse.toOk(result.getToken());
    }

    /**
     * 로그아웃.
     * DB USER_SESSIONS revoke + 쿠키 삭제.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails ud,
            HttpServletResponse response) {

        var deleteCookie = authService.logout(ud.getUserId());
        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
        return ApiResponse.toNoContent();
    }

    /** 아이디 찾기 (이름 + 휴대폰) */
    @PostMapping("/find-id")
    public ResponseEntity<ApiResponse<FindIdResponse>> findId(
            @RequestBody @Valid FindIdRequest request) {
        return ApiResponse.toOk(authService.findId(request));
    }

    /** 비밀번호 재설정 링크 발송 (Redis UUID 30분 TTL) */
    @PostMapping("/find-password")
    public ResponseEntity<ApiResponse<Void>> findPassword(
            @RequestBody @Valid FindPasswordRequest request) {
        authService.findPassword(request);
        return ApiResponse.toOk(null);
    }

    /** 비밀번호 재설정 (Redis 토큰 검증 + BCrypt + 전세션 revoke) */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestBody @Valid ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.toOk(null);
    }
}
