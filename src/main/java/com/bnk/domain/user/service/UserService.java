package com.bnk.domain.user.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.bnk.domain.user.dto.request.PasswordChangeRequest;
import com.bnk.domain.user.dto.request.UserUpdateRequest;
import com.bnk.domain.user.dto.response.CardStatusResponse;
import com.bnk.domain.user.dto.response.UserResponse;
import com.bnk.domain.user.mapper.UserMapper;
import com.bnk.domain.user.model.User;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.util.MaskingUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class UserService {

    private final UserMapper      userMapper;
    private final PasswordEncoder passwordEncoder;

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
     * 어떤 필드를 변경하든 currentPassword BCrypt 검증 필수.
     * phone 변경 시 → 포맷 변환(010-XXXX-XXXX) + is_phone_verified='N' 자동 처리.
     */
    @Transactional
    public void updateMyInfo(Long userId, @Valid UserUpdateRequest request) {
        User user = userMapper.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 정보 수정 시 항상 현재 비밀번호 확인
        if (request.getCurrentPassword() == null
                || request.getCurrentPassword().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "정보 수정 시 현재 비밀번호 확인이 필요합니다.");
        }
        if (!passwordEncoder.matches(
                request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        // 전화번호 포맷 변환 (01012345678 → 010-1234-5678)
        String formattedPhone = request.getPhone() != null
                ? MaskingUtil.formatPhone(request.getPhone())
                : null;

        User updateTarget = User.builder()
                .userId(userId)
                .name(request.getName())
                .phone(formattedPhone)
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
        log.info("[내정보수정] userId={} phoneChanged={}", userId, formattedPhone != null);
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
        if (req.getPhone()           != null) sb.append("phone(비밀번호확인완료), ");
        if (req.getJob()             != null) sb.append("job, ");
        if (req.getIncomeLevelCode() != null) sb.append("income_level_code, ");
        if (req.getPushEnabled()     != null) sb.append("push_enabled, ");
        if (req.getMarketingAgree()  != null) sb.append("marketing_agree, ");
        String result = sb.toString().replaceAll(", $", "");
        return result.equals("변경 필드: ") ? "변경 없음" : result;
    }
}
