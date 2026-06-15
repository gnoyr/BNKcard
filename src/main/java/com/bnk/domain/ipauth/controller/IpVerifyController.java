package com.bnk.domain.ipauth.controller;

import java.time.LocalDateTime;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bnk.domain.auth.mapper.UserSessionMapper;
import com.bnk.domain.auth.model.UserSession;
import com.bnk.domain.ipauth.dto.request.IpCiVerifyRequest;
import com.bnk.domain.ipauth.dto.request.IpEmailConfirmRequest;
import com.bnk.domain.ipauth.dto.request.IpEmailSendRequest;
import com.bnk.domain.ipauth.service.IpTrustService;
import com.bnk.domain.ipauth.service.IpVerifyService;
import com.bnk.global.auth.JwtTokenProvider;
import com.bnk.global.response.ApiResponse;
import com.bnk.global.util.CookieUtil;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * IP 인증 컨트롤러
 *
 * [기존 파일 수정 필요 — 기존파일_수정가이드.txt 참고]
 * 1. SecurityConfig: .requestMatchers("/api/auth/ip-verify/**").permitAll() 추가
 * 2. JwtAuthenticationFilter.SKIP_PATHS: "/api/auth/ip-verify/**" 추가
 * 3. RedisRateLimitFilter.POST_RATE_LIMIT_RULES: ip-verify 경로 3개 추가
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/ip-verify")
@RequiredArgsConstructor
public class IpVerifyController {

    private final IpTrustService    ipTrustService;
    private final IpVerifyService   ipVerifyService;
    private final JwtTokenProvider  jwtTokenProvider;
    private final UserSessionMapper userSessionMapper;
    private final CookieUtil        cookieUtil;

    /** POST /api/auth/ip-verify/email/send */
    @PostMapping("/email/send")
    public ResponseEntity<ApiResponse<Void>> sendEmailCode(
            @Valid @RequestBody IpEmailSendRequest req) {
        ipVerifyService.sendEmailVerifyCode(req.getUserId(), req.getChallengeToken());
        return ApiResponse.toOk(null);
    }

    /** POST /api/auth/ip-verify/email/confirm → 인증 성공 시 로그인 완료 */
    @PostMapping("/email/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmEmailCode(
            @Valid @RequestBody IpEmailConfirmRequest req,
            HttpServletResponse response) {

        String plainIp = ipTrustService.validateChallengeToken(req.getUserId(), req.getChallengeToken());
        ipVerifyService.verifyEmailCode(req.getUserId(), req.getCode());
        ipTrustService.approvePendingIp(req.getUserId(), plainIp, "EMAIL_VERIFY", req.getNickname());
        issueLoginCookies(req.getUserId(), response);

        log.info("[IpVerify] userId={} 이메일 인증 완료 → 로그인", req.getUserId());
        return ApiResponse.toOk(null);
    }

    /** POST /api/auth/ip-verify/ci → CI 인증 성공 시 로그인 완료 */
    @PostMapping("/ci")
    public ResponseEntity<ApiResponse<Void>> verifyCi(
            @Valid @RequestBody IpCiVerifyRequest req,
            HttpServletResponse response) {

        String plainIp = ipTrustService.validateChallengeToken(req.getUserId(), req.getChallengeToken());
        ipVerifyService.verifyCi(req.getUserId(), req.getResidentFront(), req.getGenderCode(), ipTrustService);
        ipTrustService.approvePendingIp(req.getUserId(), plainIp, "CI_VERIFY", req.getNickname());
        issueLoginCookies(req.getUserId(), response);

        log.info("[IpVerify] userId={} CI 인증 완료 → 로그인", req.getUserId());
        return ApiResponse.toOk(null);
    }

    /**
     * JWT 쿠키 발급 — 기존 AuthService.login()의 토큰 발급 로직과 동일.
     * AuthService에 issueTokenForUser(Long userId) 메서드가 추가되면 그것으로 교체 가능.
     */
    private void issueLoginCookies(Long userId, HttpServletResponse response) {
        String accessToken  = jwtTokenProvider.generateAccessToken(userId, "ROLE_USER");
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId);

        // USER_SESSIONS INSERT
        LocalDateTime now       = LocalDateTime.now(com.bnk.global.util.TimeConstants.KST);
        LocalDateTime expiresAt = now.plusSeconds(jwtTokenProvider.getRefreshExpirationSec());

        userSessionMapper.insertSession(UserSession.builder()
                .userId(userId)
                .refreshToken(refreshToken)
                .createdAt(now)
                .expiresAt(expiresAt)
                .revokedYn("N")
                .build());

        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieUtil.createAccessCookie(accessToken, jwtTokenProvider.getAccessExpirationSec()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieUtil.createRefreshCookie(refreshToken, jwtTokenProvider.getRefreshExpirationSec()).toString());
    }
}
