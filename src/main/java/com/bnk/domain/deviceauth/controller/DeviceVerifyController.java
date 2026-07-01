package com.bnk.domain.deviceauth.controller;

import java.time.LocalDateTime;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bnk.domain.auth.mapper.UserSessionMapper;
import com.bnk.domain.auth.model.UserSession;
import com.bnk.domain.deviceauth.dto.request.DeviceCiVerifyRequest;
import com.bnk.domain.deviceauth.dto.request.DeviceEmailConfirmRequest;
import com.bnk.domain.deviceauth.dto.request.DeviceEmailSendRequest;
import com.bnk.domain.deviceauth.service.DeviceTrustService;
import com.bnk.domain.deviceauth.service.DeviceTrustService.DeviceChallenge;
import com.bnk.domain.deviceauth.service.DeviceVerifyService;
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
 * 새 기기 인증 컨트롤러.
 *
 * 미신뢰 기기 로그인 시 발급된 불투명 challengeToken 으로 이메일/CI 인증을 수행하고,
 * 성공하면 해당 기기를 신뢰 등록한 뒤 로그인 쿠키를 발급한다.
 * userId는 challengeToken(서버 Redis 매핑)에서 도출하며 클라이언트가 임의로 지정할 수 없다.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/device-verify")
@RequiredArgsConstructor
public class DeviceVerifyController {

    private final DeviceTrustService  deviceTrustService;
    private final DeviceVerifyService deviceVerifyService;
    private final JwtTokenProvider    jwtTokenProvider;
    private final UserSessionMapper   userSessionMapper;
    private final CookieUtil          cookieUtil;

    /** POST /api/auth/device-verify/email/send */
    @PostMapping("/email/send")
    public ResponseEntity<ApiResponse<Void>> sendEmailCode(
            @Valid @RequestBody DeviceEmailSendRequest req) {
        DeviceChallenge ch = deviceTrustService.validateChallengeToken(req.getChallengeToken());
        deviceVerifyService.sendEmailVerifyCode(ch.userId());
        return ApiResponse.toOk(null);
    }

    /** POST /api/auth/device-verify/email/confirm → 인증 성공 시 로그인 완료 */
    @PostMapping("/email/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmEmailCode(
            @Valid @RequestBody DeviceEmailConfirmRequest req,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {

        DeviceChallenge ch = deviceTrustService.validateChallengeToken(req.getChallengeToken());
        deviceVerifyService.verifyEmailCode(ch.userId(), req.getCode());
        deviceTrustService.approvePendingDevice(ch, "EMAIL_VERIFY", req.getDeviceName());
        deviceTrustService.deleteChallenge(req.getChallengeToken());
        issueLoginCookies(ch.userId(), httpRequest, response);

        log.info("[DeviceVerify] userId={} 이메일 인증 완료 → 로그인", ch.userId());
        return ApiResponse.toOk(null);
    }

    /** POST /api/auth/device-verify/ci → CI 인증 성공 시 로그인 완료 */
    @PostMapping("/ci")
    public ResponseEntity<ApiResponse<Void>> verifyCi(
            @Valid @RequestBody DeviceCiVerifyRequest req,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {

        DeviceChallenge ch = deviceTrustService.validateChallengeToken(req.getChallengeToken());
        deviceVerifyService.verifyCi(ch.userId(), req.getName(), req.getResidentFront(), req.getPhone());
        deviceTrustService.approvePendingDevice(ch, "CI_VERIFY", req.getDeviceName());
        deviceTrustService.deleteChallenge(req.getChallengeToken());
        issueLoginCookies(ch.userId(), httpRequest, response);

        log.info("[DeviceVerify] userId={} CI 인증 완료 → 로그인", ch.userId());
        return ApiResponse.toOk(null);
    }

    /** JWT 쿠키 발급 + 세션 기록 */
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
