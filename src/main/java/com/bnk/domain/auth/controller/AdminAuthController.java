package com.bnk.domain.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
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
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
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
     * HttpServletRequest를 Service로 전달하여
     * ip_address / user_agent를 LOGIN_HISTORIES에 기록.
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
     *
     *      errorOnInvalidType 기본값(true)이면 ClassCastException 또는 NPE 발생.
     *      false로 설정하면 타입 불일치 시 null로 안전하게 주입됨.
     *      ad가 null이면 DB revoke를 생략하고 쿠키만 삭제하여 정상 응답.
     *		CookieUtil path 변경 배포 전 발급된 구버전 쿠키(path=/api/auth/refresh)가
     *      브라우저에 잔류할 수 있으므로 로그아웃 시 함께 삭제.
     *      7일(refresh-expiration) 경과 후 구 쿠키가 자연 만료되면 제거 가능.
     *
     *   AuthService.logout(Long userId)이 USER_SESSIONS.revoke_all을 처리하므로
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> adminLogout(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomAdminDetails ad,
            HttpServletResponse response) {

        if (ad != null) {
            authService.adminLogout(ad.getAdminId()); // ★ logout() → adminLogout()
        }

        response.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.deleteAccessCookie().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.deleteAdminRefreshCookie().toString()); // ★ 관리자 path

        return ApiResponse.toNoContent();
    }

    /**
     * 관리자 인증 상태 확인 — header.js에서 사용.
     *
     * 기존: header.js가 GET /api/admin/dashboard 응답 여부로 인증 판단
     *       → 대시보드 API 지연·변경 시 인증 흐름 영향
     *
     * 개선: 전용 /me 엔드포인트로 분리
     *       → 인증 확인 책임을 대시보드와 분리
     *       → @AuthenticationPrincipal 주입 실패(미인증) 시 401 자동 반환
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
    
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> adminRefresh(
            HttpServletRequest  httpRequest,
            HttpServletResponse response) {

        String refreshToken = CookieUtil.extractCookieValue(httpRequest, CookieUtil.REFRESH_TOKEN_COOKIE)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));

        ResponseCookie newAccessCookie = authService.adminRefresh(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, newAccessCookie.toString());

        return ResponseEntity.ok(ApiResponse.message("토큰이 재발급되었습니다."));
    }
}
