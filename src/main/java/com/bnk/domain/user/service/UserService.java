package com.bnk.domain.user.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
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
import com.bnk.global.util.TimeConstants;
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
    public void updateMyInfo(Long userId, UserUpdateRequest request) {

        // 1. 사용자 조회
        User user = userMapper.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 2. 어떤 종류의 변경인지 판단
        boolean hasPersonalChange = request.getName() != null
                || request.getPhone() != null
                || request.getJob() != null
                || request.getIncomeLevelCode() != null
                || request.getCreditScore() != null;

        boolean hasNotifChange    = request.getPushEnabled() != null
                                 || request.getMarketingAgree() != null;

        // ① — 변경 필드가 하나도 없으면 no-op 얼리 리턴
        //   (정상_변경없음_noOp 통과)
        if (!hasPersonalChange && !hasNotifChange) {
            return;
        }

        // ② — 개인정보(name/phone) 변경 시에만 비밀번호 검증
        //   (정상_알림설정만변경은 이 블록을 타지 않아야 함)
        if (hasPersonalChange) {

            // ③ — null/blank를 passwordEncoder에 넘기기 전에 차단
            //   (실패_phone변경_비밀번호누락, 실패_현재비밀번호불일치 통과)
            String current = request.getCurrentPassword();
            if (!StringUtils.hasText(current)
                    || !passwordEncoder.matches(current, user.getPasswordHash())) {
                throw new BusinessException(ErrorCode.INVALID_PASSWORD);
            }
        }

        // 3. 변경 적용 후 저장
        User updated = user.applyUpdate(request);
        userMapper.updateUser(updated);
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
        userMapper.updatePassword(userId, newHash, LocalDateTime.now(TimeConstants.KST));
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
}
