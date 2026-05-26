package com.bnk.domain.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bnk.domain.auth.dto.request.AdminLoginRequest;
import com.bnk.domain.auth.dto.response.AdminMeResponse;
import com.bnk.domain.auth.dto.response.AuthTokenResult;
import com.bnk.domain.auth.service.AuthService;
import com.bnk.global.auth.CustomAdminDetails;
import com.bnk.global.response.ApiResponse;
import com.bnk.global.util.CookieUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthService authService;
    private final CookieUtil  cookieUtil;

    /**
     * 관리자 로그인 — Access + Refresh 쿠키 발급.
     * HttpServletRequest 를 Service 로 전달하여
     * ip_address / user_agent 를 LOGIN_HISTORIES 에 기록.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Void>> adminLogin(
            @RequestBody @Valid AdminLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {

        AuthTokenResult result = authService.adminLogin(request, httpRequest);
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

        authService.logout(ad.getAdminId());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.deleteAccessCookie().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.deleteRefreshCookie().toString());
        return ApiResponse.toNoContent();
    }

    /**
     * [신규] 관리자 인증 상태 확인 — header.js 에서 사용
     *
     * 기존: header.js 가 GET /api/admin/dashboard 응답 여부로 인증 판단
     *       → 대시보드 API 지연·변경 시 인증 흐름 영향
     *
     * 개선: 전용 /me 엔드포인트로 분리
     *       → 인증 확인 책임을 대시보드와 분리
     *       → @AuthenticationPrincipal 주입 실패(미인증) 시 401 자동 반환
     *
     * SecurityConfig: /api/admin/** → hasAnyRole(...) 이미 적용되어 있으므로
     *                 별도 permitAll 추가 없이 동작.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AdminMeResponse>> getAdminMe(
            @AuthenticationPrincipal CustomAdminDetails ad) {

        return ApiResponse.toOk(
            AdminMeResponse.builder()
                .adminId(ad.getAdminId())
                .name(ad.getUsername())
                .roles(ad.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .toList())
                .build()
        );
    }
}
