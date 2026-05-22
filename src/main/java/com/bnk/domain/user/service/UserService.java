package com.bnk.domain.user.service;

import com.bnk.domain.user.dto.request.PasswordChangeRequest;
import com.bnk.domain.user.dto.request.UserUpdateRequest;
import com.bnk.domain.user.dto.response.CardStatusResponse;
import com.bnk.domain.user.dto.response.UserResponse;
import com.bnk.domain.user.mapper.UserMapper;
import com.bnk.domain.user.model.User;
import com.bnk.global.email.EmailService;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.util.MaskingUtil;
import com.bnk.global.util.MemoryTokenStore;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class UserService {

    private final UserMapper       userMapper;
    private final PasswordEncoder  passwordEncoder;
    private final EmailService     emailService;
    private final MemoryTokenStore tokenStore;

    private static final String KEY_PHONE_VERIFY = "mypage:phone:verify:";
    private static final long   CODE_TTL_MIN     = 10L;

    // ================================================================
    // F-24 | 내 정보 조회
    // ================================================================

    @Transactional(readOnly = true)
    public UserResponse getMyInfo(Long userId) {
        User user = userMapper.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return UserResponse.from(user);
    }

    // ================================================================
    // F-25 | 내 정보 수정
    // ================================================================

    /**
     * phone 변경 시 MaskingUtil.formatPhone()으로 xxx-xxxx-xxxx 포맷 후 저장.
     * UserMapper.xml updateUser 의 <if test="phone != null"> 조건에서
     * is_phone_verified='N' 자동 처리됨.
     */
    @Transactional
    public void updateMyInfo(Long userId, @Valid UserUpdateRequest request) {
        userMapper.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // phone 포맷 변환 (01012345678 → 010-1234-5678)
        String formattedPhone = request.getPhone() != null
                ? MaskingUtil.formatPhone(request.getPhone())
                : null;

        User updateTarget = User.builder()
                .userId(userId)
                .name(request.getName())
                .phone(formattedPhone)                    // ← 포맷된 번호
                .job(request.getJob())
                .incomeLevelCode(request.getIncomeLevelCode())
                .pushEnabled(boolToYN(request.getPushEnabled()))
                .marketingAgree(boolToYN(request.getMarketingAgree()))
                .build();

        userMapper.updateUser(updateTarget);

        userMapper.insertAuditLog(
                "USER", userId, "USER_UPDATE",
                "USER", userId, buildUpdateDesc(request), null
        );
        log.info("[내정보수정] userId={} formattedPhone={}", userId, formattedPhone);
    }

    // ================================================================
    // F-26 | 비밀번호 변경
    // ================================================================

    @Transactional
    public void changePassword(Long userId, @Valid PasswordChangeRequest request) {
        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        User user = userMapper.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        userMapper.updatePassword(userId,
                passwordEncoder.encode(request.getNewPassword()), LocalDateTime.now());
        userMapper.revokeAllSessions(userId);

        userMapper.insertAuditLog(
                "USER", userId, "PASSWORD_CHANGE",
                "USER", userId, "비밀번호 변경", null
        );
        log.info("[비밀번호변경] userId={}", userId);
    }

    // ================================================================
    // 전화번호 변경 후 이메일 재인증
    // ================================================================

    @Transactional(readOnly = true)
    public void sendPhoneVerifyCode(Long userId) {
        User user = userMapper.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String code = UUID.randomUUID()
                .toString().replaceAll("-", "").substring(0, 6).toUpperCase();

        tokenStore.set(KEY_PHONE_VERIFY + userId, code, CODE_TTL_MIN);
        emailService.sendVerificationEmail(user.getEmail(), code);

        log.info("[전화번호변경 이메일인증] 코드 발송 userId={}", userId);
    }

    @Transactional
    public void confirmPhoneVerifyCode(Long userId, String code) {
        String savedCode = tokenStore.get(KEY_PHONE_VERIFY + userId);

        if (savedCode == null) {
            throw new BusinessException(ErrorCode.VERIFY_TOKEN_INVALID,
                    "인증 코드가 만료되었거나 존재하지 않습니다. 다시 발송해주세요.");
        }
        if (!savedCode.equalsIgnoreCase(code)) {
            throw new BusinessException(ErrorCode.VERIFY_TOKEN_INVALID,
                    "인증 코드가 일치하지 않습니다.");
        }

        tokenStore.delete(KEY_PHONE_VERIFY + userId);
        userMapper.updatePhoneVerified(userId, "Y");

        userMapper.insertAuditLog(
                "USER", userId, "PHONE_VERIFY_BY_EMAIL",
                "USER", userId, "이메일 인증으로 전화번호 변경 확인 완료", null
        );
        log.info("[전화번호변경 이메일인증] 확인 완료 userId={}", userId);
    }

    // ================================================================
    // RQ-F17 | 보유 카드 및 신청 현황
    // ================================================================

    @Transactional(readOnly = true)
    public CardStatusResponse getMyCards(Long userId) {
        List<CardStatusResponse.OwnedCardDto> ownedCards =
                userMapper.selectOwnedCards(userId).stream()
                        .map(r -> CardStatusResponse.OwnedCardDto.builder()
                                .userCardId(r.getUserCardId())
                                .cardId(r.getCardId())
                                .cardName(r.getCardName())
                                .cardImageUrl(r.getCardImageUrl())
                                .issuedAt(r.getIssuedAt())
                                .build())
                        .collect(Collectors.toList());

        List<CardStatusResponse.CardApplicationDto> applications =
                userMapper.selectCardApplications(userId).stream()
                        .map(r -> CardStatusResponse.CardApplicationDto.builder()
                                .applicationId(r.getApplicationId())
                                .cardId(r.getCardId())
                                .cardName(r.getCardName())
                                .cardImageUrl(r.getCardImageUrl())
                                .applicationStatus(r.getApplicationStatus())
                                .appliedAt(r.getAppliedAt())
                                .build())
                        .collect(Collectors.toList());

        return CardStatusResponse.builder()
                .ownedCards(ownedCards)
                .applications(applications)
                .build();
    }

    // ================================================================
    // private 헬퍼
    // ================================================================

    private String boolToYN(Boolean value) {
        if (value == null) return null;
        return value ? "Y" : "N";
    }

    private String buildUpdateDesc(UserUpdateRequest req) {
        StringBuilder sb = new StringBuilder("변경 필드: ");
        if (req.getName()            != null) sb.append("name, ");
        if (req.getPhone()           != null) sb.append("phone(재인증필요), ");
        if (req.getJob()             != null) sb.append("job, ");
        if (req.getIncomeLevelCode() != null) sb.append("income_level_code, ");
        if (req.getPushEnabled()     != null) sb.append("push_enabled, ");
        if (req.getMarketingAgree()  != null) sb.append("marketing_agree, ");
        String result = sb.toString().replaceAll(", $", "");
        return result.equals("변경 필드: ") ? "변경 없음" : result;
    }
}
