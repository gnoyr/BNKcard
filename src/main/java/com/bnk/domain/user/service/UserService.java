package com.bnk.domain.user.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.bnk.domain.user.dto.query.CardApplicationRow;
import com.bnk.domain.user.dto.query.OwnedCardRow;
import com.bnk.domain.user.dto.request.PasswordChangeRequest;
import com.bnk.domain.user.dto.request.UserUpdateRequest;
import com.bnk.domain.user.dto.response.CardStatusResponse;
import com.bnk.domain.user.dto.response.UserResponse;
import com.bnk.domain.user.mapper.UserMapper;
import com.bnk.domain.user.model.User;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.util.audit.AuditLogger;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class UserService {

	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;
	private final AuditLogger auditLogger;

    // ================================================================
    // 내 정보 조회
    // ================================================================
    @Transactional(readOnly = true)
    public UserResponse getMyInfo(Long userId) {
        User user = userMapper.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return UserResponse.from(user);
    }

    // ================================================================
    // 내 정보 수정
    // ================================================================
    @Transactional
    public void updateMyInfo(Long userId, @Valid UserUpdateRequest request) {
        User user = userMapper.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (requiresPasswordVerification(request)) {
            if (request.getCurrentPassword() == null ||
                    !passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                auditLogger.failure(AuditLogger.AUTH, AuditLogger.UPDATE,
                        userId, null, "내 정보 수정 — 비밀번호 검증 실패");
                throw new BusinessException(ErrorCode.INVALID_PASSWORD);
            }
        }

        if (!hasAnyField(request)) {
            return;
        }

        User updated = User.builder()
                .userId(userId)
                .name(request.getName() != null ? request.getName() : user.getName())
                .phone(request.getPhone() != null ? request.getPhone() : user.getPhone())
                .job(request.getJob() != null ? request.getJob() : user.getJob())
                .incomeLevelCode(request.getIncomeLevelCode() != null ? request.getIncomeLevelCode() : user.getIncomeLevelCode())
                .creditScore(request.getCreditScore() != null ? request.getCreditScore() : user.getCreditScore())
                .pushEnabled(request.getPushEnabled() != null ? (request.getPushEnabled() ? "Y" : "N") : user.getPushEnabled())
                .marketingAgree(request.getMarketingAgree() != null ? (request.getMarketingAgree() ? "Y" : "N") : user.getMarketingAgree())
                .build();

        userMapper.updateUser(updated);
        auditLogger.success(AuditLogger.AUTH, AuditLogger.UPDATE, userId, null, null);
    }

    // ================================================================
    // 비밀번호 변경
    // ================================================================
    @Transactional
    public void changePassword(Long userId, @Valid PasswordChangeRequest request) {
        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            auditLogger.failure(AuditLogger.AUTH, AuditLogger.PASSWORD_CHANGE,
                    userId, null, "비밀번호 확인 불일치");
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        User user = userMapper.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            auditLogger.failure(AuditLogger.AUTH, AuditLogger.PASSWORD_CHANGE,
                    userId, null, "현재 비밀번호 불일치");
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        List<String> recentHashes = userMapper.findRecentPasswordHashes(userId, 5);
        boolean recentlyUsed = recentHashes.stream()
                .anyMatch(h -> passwordEncoder.matches(request.getNewPassword(), h));
        if (recentlyUsed) {
            auditLogger.failure(AuditLogger.AUTH, AuditLogger.PASSWORD_CHANGE,
                    userId, null, "최근 사용한 비밀번호 재사용 시도");
            throw new BusinessException(ErrorCode.PASSWORD_RECENTLY_USED);
        }

        String newHash = passwordEncoder.encode(request.getNewPassword());
        userMapper.updatePassword(userId, newHash, LocalDateTime.now());
        userMapper.insertPasswordHistory(userId, newHash);
        userMapper.deleteOldPasswordHistories(userId);
        userMapper.revokeAllSessions(userId);
        auditLogger.success(AuditLogger.AUTH, AuditLogger.PASSWORD_CHANGE,
                userId, null, null);
    }

    // ================================================================
    // 보유 카드 및 신청 현황
    // ================================================================
    @Transactional(readOnly = true)
    public CardStatusResponse getMyCards(Long userId) {
		List<OwnedCardRow> ownedCards = userMapper.selectOwnedCards(userId);
		List<CardApplicationRow> applications = userMapper.selectCardApplications(userId);

        List<CardStatusResponse.OwnedCardDto> ownedDtos = ownedCards.stream()
                .map(r -> CardStatusResponse.OwnedCardDto.builder()
                        .userCardId(r.getUserCardId())
                        .cardId(r.getCardId())
                        .cardName(r.getCardName())
                        .cardImageUrl(r.getCardImageUrl())
                        .issuedAt(r.getIssuedAt())
                        .build())
                .collect(Collectors.toList());

        List<CardStatusResponse.CardApplicationDto> appDtos = applications.stream()
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
                .ownedCards(ownedDtos)
                .applications(appDtos)
                .build();
    }

    // ================================================================
    // 헬퍼
    // ================================================================
    private boolean requiresPasswordVerification(UserUpdateRequest request) {
        return (request.getName() != null && !request.getName().isBlank())
            || (request.getPhone() != null && !request.getPhone().isBlank())
            || request.getJob() != null
            || request.getIncomeLevelCode() != null
            || request.getCreditScore() != null;
    }

    private boolean hasAnyField(UserUpdateRequest request) {
        return requiresPasswordVerification(request)
                || request.getPushEnabled() != null
                || request.getMarketingAgree() != null;
    }
}
