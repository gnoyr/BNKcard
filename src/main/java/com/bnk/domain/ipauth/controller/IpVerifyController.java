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
import com.bnk.global.util.ClientIpUtil;
import com.bnk.global.util.CookieUtil;
import com.bnk.global.util.TimeConstants;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * IP 인증 컨트롤러
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
            HttpServletRequest httpRequest,
            HttpServletResponse response) {

        String plainIp = ipTrustService.validateChallengeToken(req.getUserId(), req.getChallengeToken());
        ipVerifyService.verifyEmailCode(req.getUserId(), req.getCode());
        ipTrustService.approvePendingIp(req.getUserId(), plainIp, "EMAIL_VERIFY", req.getNickname());
        issueLoginCookies(req.getUserId(), httpRequest, response);

        log.info("[IpVerify] userId={} 이메일 인증 완료 → 로그인", req.getUserId());
        return ApiResponse.toOk(null);
    }

    /**
     * POST /api/auth/ip-verify/ci → CI 인증 성공 시 로그인 완료
     */
    @PostMapping("/ci")
    public ResponseEntity<ApiResponse<Void>> verifyCi(
            @Valid @RequestBody IpCiVerifyRequest req,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {

        String plainIp = ipTrustService.validateChallengeToken(req.getUserId(), req.getChallengeToken());

        ipVerifyService.verifyCi(
        	    req.getUserId(),
        	    req.getName(),
        	    req.getResidentFront(),
        	    req.getGenderCode(),
        	    req.getAddress(),
        	    ipTrustService
        	);

        ipTrustService.approvePendingIp(req.getUserId(), plainIp, "CI_VERIFY", req.getNickname());
        issueLoginCookies(req.getUserId(), httpRequest, response);

        log.info("[IpVerify] userId={} CI 인증 완료 → 로그인", req.getUserId());
        return ApiResponse.toOk(null);
    }

    /**
     * JWT 쿠키 발급
     */
    private void issueLoginCookies(Long userId, HttpServletRequest httpRequest, HttpServletResponse response) {
        String accessToken   = jwtTokenProvider.generateAccessToken(userId, "ROLE_USER");
        String refreshToken  = jwtTokenProvider.generateRefreshToken(userId);
        long   refreshExpSec = jwtTokenProvider.getRefreshExpirationSec();

        String ipAddress  = ClientIpUtil.normalize(httpRequest.getRemoteAddr());
        String userAgent  = httpRequest.getHeader("User-Agent");
        String deviceInfo = (userAgent != null && userAgent.length() > 100)
                ? userAgent.substring(0, 100) : userAgent;

        LocalDateTime now       = LocalDateTime.now(TimeConstants.KST);
        LocalDateTime expiresAt = now.plusSeconds(refreshExpSec);

        userSessionMapper.insertSession(UserSession.builder()
                .userId(userId)
                .refreshToken(refreshToken)
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .createdAt(now)
                .expiresAt(expiresAt)
                .revokedYn("N")
                .build());

        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieUtil.createAccessCookie(accessToken, jwtTokenProvider.getAccessExpirationSec()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieUtil.createRefreshCookie(refreshToken, refreshExpSec).toString());
    }
}