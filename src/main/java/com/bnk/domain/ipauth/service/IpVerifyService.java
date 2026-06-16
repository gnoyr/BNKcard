package com.bnk.domain.ipauth.service;

import com.bnk.domain.user.mapper.UserMapper;
import com.bnk.domain.user.model.User;
import com.bnk.global.email.EmailService;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.util.CiValueGenerator;
import com.bnk.global.util.MaskingUtil;
import com.bnk.global.util.TokenStore;
import com.bnk.global.util.audit.AuditLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class IpVerifyService {

    private static final int    EMAIL_CODE_LENGTH  = 6;
    private static final String CODE_CHARS         = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final long   EMAIL_CODE_TTL_MIN = 10L;

    private final SecureRandom     secureRandom   = new SecureRandom();

    private final TokenStore       tokenStore;
    private final EmailService     emailService;
    private final UserMapper       userMapper;
    private final AuditLogger      auditLogger;
    private final CiValueGenerator ciValueGenerator; // 추가

    // ─── 이메일 인증코드 발송 ─────────────────────────────────────────

    public void sendEmailVerifyCode(Long userId, String challengeToken) {
        if (tokenStore.get(challengeToken) == null) {
            throw new BusinessException(ErrorCode.IP_CHALLENGE_EXPIRED);
        }

        User user = userMapper.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String code = generateCode();
        tokenStore.set(IpTrustService.emailVerifyKey(userId), code, EMAIL_CODE_TTL_MIN);

        emailService.sendIpVerifyCode(user.getEmail(), code);
        log.info("[IpVerify] userId={} IP 인증 이메일 발송", userId);
    }

    // ─── 이메일 인증코드 검증 ─────────────────────────────────────────

    public void verifyEmailCode(Long userId, String code) {
        String stored = tokenStore.get(IpTrustService.emailVerifyKey(userId));

        if (stored == null) {
            auditLogger.failure(AuditLogger.AUTH, "IP_VERIFY_FAIL", userId, "", "이메일 코드 만료");
            throw new BusinessException(ErrorCode.VERIFY_TOKEN_INVALID);
        }
        if (!stored.equals(code.toUpperCase())) {
            auditLogger.failure(AuditLogger.AUTH, "IP_VERIFY_FAIL", userId, "", "이메일 코드 불일치");
            throw new BusinessException(ErrorCode.VERIFY_TOKEN_INVALID);
        }

        tokenStore.delete(IpTrustService.emailVerifyKey(userId));
        log.info("[IpVerify] userId={} 이메일 인증 성공", userId);
    }

    // ─── CI 인증 검증 ─────────────────────────────────────────────────

    /**
     * 이름 + 생년월일 + 전화번호로 CI를 재생성하여 DB 저장값과 비교.
     *
     * [변경 이유]
     * 기존: residentFront + genderCode 단순 연결 → storedCi(SHA-256 Base64)와 절대 불일치
     * 변경: CiValueGenerator.generate()로 동일한 방식의 CI를 생성하여 비교
     *
     * @param name        이름 (회원가입 시 입력한 값)
     * @param birthDate   생년월일 YYYY-MM-DD (회원가입 시 입력한 값)
     * @param phone       전화번호 (포맷 무관 — 내부에서 숫자만 추출)
     */
    public void verifyCi(Long userId, String name, String birthDate, String phone,
                         IpTrustService ipTrustService) {
        User user = userMapper.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 전화번호 포맷 정규화 (MaskingUtil.formatPhone과 동일한 방식)
        String formattedPhone = MaskingUtil.formatPhone(phone);

        // 회원가입과 동일한 로직으로 CI 재생성
        String inputCi  = ciValueGenerator.generate(name, birthDate, formattedPhone);
        String storedCi = user.getCiValue(); // TypeHandler가 AES 복호화한 평문

        if (!storedCi.equals(inputCi)) {
            ipTrustService.incrementCiFailCount(userId);
            auditLogger.failure(AuditLogger.AUTH, "CI_VERIFY_FAIL", userId, "", "CI 불일치");
            throw new BusinessException(ErrorCode.CI_MISMATCH);
        }

        ipTrustService.resetCiFailCount(userId);
        log.info("[IpVerify] userId={} CI 인증 성공", userId);
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