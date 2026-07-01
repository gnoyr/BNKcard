package com.bnk.domain.deviceauth.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Service;

import com.bnk.domain.user.mapper.UserMapper;
import com.bnk.domain.user.model.User;
import com.bnk.global.email.EmailService;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.util.CiValueGenerator;
import com.bnk.global.util.TokenStore;
import com.bnk.global.util.audit.AuditLogger;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 새 기기 인증 검증 서비스.
 *
 * 이메일 코드 발송/검증, CI(본인확인) 검증을 담당한다.
 * 성공 시 신뢰 기기 등록(DeviceTrustService.approvePendingDevice)은 컨트롤러에서 이어서 수행.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceVerifyService {

    private static final int    EMAIL_CODE_LENGTH  = 6;
    private static final String CODE_CHARS         = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final long   EMAIL_CODE_TTL_MIN = 10L;

    private final SecureRandom     secureRandom = new SecureRandom();

    private final TokenStore        tokenStore;
    private final EmailService      emailService;
    private final UserMapper        userMapper;
    private final AuditLogger       auditLogger;
    private final CiValueGenerator  ciValueGenerator;
    private final DeviceTrustService deviceTrustService;

    // ─── 이메일 인증코드 발송 ─────────────────────────────────────────

    public void sendEmailVerifyCode(Long userId) {
        User user = userMapper.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String code = generateCode();
        tokenStore.set(DeviceTrustService.emailVerifyKey(userId), code, EMAIL_CODE_TTL_MIN);

        emailService.sendDeviceVerifyCode(user.getEmail(), code);
        log.info("[DeviceVerify] userId={} 기기 인증 이메일 발송", userId);
    }

    // ─── 이메일 인증코드 검증 ─────────────────────────────────────────

    public void verifyEmailCode(Long userId, String code) {
        String stored = tokenStore.get(DeviceTrustService.emailVerifyKey(userId));

        if (stored == null) {
            auditLogger.failure(AuditLogger.AUTH, "DEVICE_VERIFY_FAIL", userId, "", "이메일 코드 만료");
            throw new BusinessException(ErrorCode.VERIFY_TOKEN_INVALID);
        }
        if (!stored.equals(code.toUpperCase())) {
            auditLogger.failure(AuditLogger.AUTH, "DEVICE_VERIFY_FAIL", userId, "", "이메일 코드 불일치");
            throw new BusinessException(ErrorCode.VERIFY_TOKEN_INVALID);
        }

        tokenStore.delete(DeviceTrustService.emailVerifyKey(userId));
        log.info("[DeviceVerify] userId={} 이메일 인증 성공", userId);
    }

    // ─── CI 인증 검증 ─────────────────────────────────────────────────

    /**
     * CI 기반 본인 확인.
     *
     * @param residentFront 주민번호 앞 6자리 (= 생년월일 YYMMDD)
     */
    public void verifyCi(Long userId, String name, String residentFront, String phone) {
        User user = userMapper.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 회원가입과 동일한 로직으로 CI 재생성 (이름 + 생년월일 + 전화번호)
        String inputCi  = ciValueGenerator.generate(name, residentFront, phone);
        String storedCi = user.getCiValue(); // TypeHandler가 AES 복호화한 평문

        if (!storedCi.equals(inputCi)) {
            deviceTrustService.incrementCiFailCount(userId);
            auditLogger.failure(AuditLogger.AUTH, "CI_VERIFY_FAIL", userId, "", "CI 불일치");
            throw new BusinessException(ErrorCode.CI_MISMATCH);
        }

        deviceTrustService.resetCiFailCount(userId);
        log.info("[DeviceVerify] userId={} CI 인증 성공", userId);
    }

    // ─── 유틸 ────────────────────────────────────────────────────────

    private String generateCode() {
        StringBuilder sb = new StringBuilder(EMAIL_CODE_LENGTH);
        for (int i = 0; i < EMAIL_CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(secureRandom.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
