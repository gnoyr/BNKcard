package com.bnk.domain.auth.controller;

import com.bnk.domain.auth.dto.request.*;
import com.bnk.domain.auth.dto.response.AuthTokenResult;
import com.bnk.domain.auth.dto.response.FindIdResponse;
import com.bnk.domain.auth.service.AuthService;
import com.bnk.global.auth.CustomUserDetails;
import com.bnk.global.response.ApiResponse;
import com.bnk.global.util.CookieUtil;
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
    private final CookieUtil cookieUtil;

    /** 회원가입 */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Long>> signup(
            @RequestBody @Valid SignupRequest request) {
        return ApiResponse.toCreated(authService.signup(request));
    }

    /** 이메일 인증 */
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestBody @Valid EmailVerifyRequest request) {
        authService.verifyEmail(request);
        return ApiResponse.toOk(null);
    }

    /** 로그인 — Access + Refresh 쿠키 발급 */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Void>> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletResponse response) {
        AuthTokenResult result = authService.login(request);
        response.addHeader(HttpHeaders.SET_COOKIE, result.getAccessCookie().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, result.getRefreshCookie().toString());
        return ResponseEntity.ok(ApiResponse.message("로그인에 성공했습니다."));
    }

    /** Access Token 재발급 — Refresh 쿠키로 새 Access 쿠키 발급 */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> refresh(
            @CookieValue(name = "refresh_token") String refreshToken,
            HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, authService.refresh(refreshToken).toString());
        return ApiResponse.toOk(null);
    }

    /** 로그아웃 — DB 세션 revoke + Access/Refresh 쿠키 삭제 */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails ud,
            HttpServletResponse response) {
        authService.logout(ud.getUserId());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.deleteAccessCookie().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.deleteRefreshCookie().toString());
        return ApiResponse.toNoContent();
    }

    /** 아이디 찾기 */
    @PostMapping("/find-id")
    public ResponseEntity<ApiResponse<FindIdResponse>> findId(
            @RequestBody @Valid FindIdRequest request) {
        return ApiResponse.toOk(authService.findId(request));
    }

    /** 비밀번호 재설정 링크 요청 */
    @PostMapping("/find-password")
    public ResponseEntity<ApiResponse<Void>> findPassword(
            @RequestBody @Valid FindPasswordRequest request) {
        authService.findPassword(request);
        return ApiResponse.toOk(null);
    }

    /** 비밀번호 재설정 */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestBody @Valid ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.toOk(null);
    }
}