package com.bnk.domain.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bnk.domain.auth.dto.request.AdminLoginRequest;
import com.bnk.domain.auth.dto.response.AuthTokenResult;
import com.bnk.domain.auth.service.AuthService;
import com.bnk.global.auth.CustomAdminDetails;
import com.bnk.global.response.ApiResponse;
import com.bnk.global.util.CookieUtil;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthService authService;
    private final CookieUtil  cookieUtil;   // ← 추가

    /** 관리자 로그인 — Access + Refresh 쿠키 발급 */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Void>> adminLogin(
            @RequestBody @Valid AdminLoginRequest request,
            HttpServletResponse response) {
        AuthTokenResult result = authService.adminLogin(request);
        response.addHeader(HttpHeaders.SET_COOKIE, result.getAccessCookie().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, result.getRefreshCookie().toString());
        return ResponseEntity.ok(ApiResponse.message("관리자 로그인에 성공했습니다."));
    }

    /**
     * 관리자 로그아웃 — DB 세션 revoke + 쿠키 삭제.
     * /api/auth/logout 은 CustomUserDetails 를 주입받으므로
     * 관리자 토큰(ADMIN_ACCESS)으로 호출하면 ud == null → NPE 발생.
     * 관리자 전용 엔드포인트를 분리하여 CustomAdminDetails 로 수신.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> adminLogout(
            @AuthenticationPrincipal CustomAdminDetails ad,
            HttpServletResponse response) {
        authService.logout(ad.getAdminId());   // USER_SESSIONS revoke
        response.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.deleteAccessCookie().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.deleteRefreshCookie().toString());
        return ApiResponse.toNoContent();
    }
}