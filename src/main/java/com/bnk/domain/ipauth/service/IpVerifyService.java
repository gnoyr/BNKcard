package com.bnk.domain.ipauth.service;

import com.bnk.domain.user.mapper.UserMapper;
import com.bnk.domain.user.model.User;
import com.bnk.global.email.EmailService;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
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

    private final TokenStore  tokenStore;
    /**
     * 기존 com.bnk.global.email.EmailService 구체 클래스 주입.
     * sendIpVerifyCode() 메서드를 EmailService에 추가 필요 (기존파일_수정가이드.txt 참고).
     */
    private final EmailService emailService;
    /**
     * UserMapper를 직접 주입하여 email/ciValue 조회.
     * UserService에 해당 메서드가 없으므로 Mapper 직접 사용.
     */
    private final UserMapper   userMapper;
    private final AuditLogger  auditLogger;

    // ─── 이메일 인증코드 발송 ─────────────────────────────────────────

    public void sendEmailVerifyCode(Long userId, String challengeToken) {
        if (tokenStore.get(challengeToken) == null) {
            throw new BusinessException(ErrorCode.IP_CHALLENGE_EXPIRED);
        }

        User user = userMapper.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String code = generateCode();
        tokenStore.set(IpTrustService.emailVerifyKey(userId), code, EMAIL_CODE_TTL_MIN);

        // EmailService에 sendIpVerifyCode() 추가 필요 — 기존파일_수정가이드.txt 참고
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
     * CI 값 검증.
     * UserMapper resultMap → typeHandler=aesTypeHandler → ciValue 자동 복호화.
     */
    public void verifyCi(Long userId, String residentFront, String genderCode,
                         IpTrustService ipTrustService) {
        User user = userMapper.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // ciValue는 TypeHandler가 자동 복호화한 평문값
        String storedCi = user.getCiValue();
        String inputCi  = residentFront + genderCode;

        if (!storedCi.equals(inputCi)) {
            ipTrustService.incrementCiFailCount(userId);
            throw new BusinessException(ErrorCode.CI_MISMATCH);
        }

        ipTrustService.resetCiFailCount(userId);
        log.info("[IpVerify] userId={} CI 인증 성공", userId);
    }

    // ─── 유틸 ────────────────────────────────────────────────────────

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(EMAIL_CODE_LENGTH);
        for (int i = 0; i < EMAIL_CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
