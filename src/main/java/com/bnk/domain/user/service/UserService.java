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
     * phone 변경 시 → 포맷 변환(010-XXXX-XXXX) + is_phone_verified='N' 자동 처리(UserMapper.xml).
     */
    @Transactional
    public void updateMyInfo(Long userId, @Valid UserUpdateRequest request) {
        User user = userMapper.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // ── 변경할 필드가 하나도 없으면 early return ────────────────
        if (!hasAnyField(request)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "변경된 내용이 없습니다.");
        }

        // ── 비밀번호 재확인 (모든 수정에 필수) ──────────────────────
        if (request.getCurrentPassword() == null
                || request.getCurrentPassword().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "정보 수정 시 현재 비밀번호 확인이 필요합니다.");
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        // ── 전화번호 포맷 변환 (01012345678 → 010-1234-5678) ────────
        String formattedPhone = request.getPhone() != null
                ? MaskingUtil.formatPhone(request.getPhone())
                : null;

        userMapper.updateUser(User.builder()
                .userId(userId)
                .name(request.getName())
                .phone(formattedPhone)
                .job(request.getJob())
                .incomeLevelCode(request.getIncomeLevelCode())
                .creditScore(request.getCreditScore())                          // ✅ 추가
                .pushEnabled(request.getPushEnabled() != null
                        ? (request.getPushEnabled() ? "Y" : "N") : null)
                .marketingAgree(request.getMarketingAgree() != null
                        ? (request.getMarketingAgree() ? "Y" : "N") : null)
                .build());
    }

    // ================================================================
    // F-25 헬퍼 | 수정 가능한 필드 중 하나라도 있는지 확인
    // ================================================================

    private boolean hasAnyField(UserUpdateRequest request) {
        return request.getName()            != null
            || request.getPhone()           != null
            || request.getJob()             != null
            || request.getIncomeLevelCode() != null
            || request.getCreditScore()     != null
            || request.getPushEnabled()     != null
            || request.getMarketingAgree()  != null;
    }

    // ================================================================
    // F-26 | 비밀번호 변경
    // ================================================================

    /**
     * 검증 순서:
     * ① newPassword == newPasswordConfirm
     * ② findById (USER_NOT_FOUND)
     * ③ currentPassword BCrypt 검증
     * ④ updatePassword + revokeAllSessions (전 기기 로그아웃)
     */
    @Transactional
    public void changePassword(Long userId, @Valid PasswordChangeRequest request) {
        // ① 새 비밀번호 확인 불일치 — DB 조회 전에 차단
        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        User user = userMapper.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // ③ 현재 비밀번호 검증
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        // ④ 비밀번호 업데이트 + 전 기기 세션 파기
        String newHash = passwordEncoder.encode(request.getNewPassword());
        userMapper.updatePassword(userId, newHash, LocalDateTime.now());
        userMapper.revokeAllSessions(userId);
    }

    // ================================================================
    // RQ-F17 | 보유 카드 및 신청 현황
    // ================================================================

    @Transactional(readOnly = true)
    public CardStatusResponse getMyCards(Long userId) {
        userMapper.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

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
}