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

	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;

	private static final int PASSWORD_HISTORY_LIMIT = 5;

	// ================================================================
	   // F-24 | 내 정보 조회
	// ================================================================
	@Transactional(readOnly = true)
	public UserResponse getMyInfo(Long userId) {
		User user = userMapper.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		return UserResponse.from(user);
	}

	// ================================================================
	// F-25 | 내 정보 수정
	// ================================================================
	@Transactional
	public void updateMyInfo(Long userId, @Valid UserUpdateRequest request) {
		User user = userMapper.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		if (!hasAnyField(request)) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}

		if (requiresPasswordVerification(request)) {
			if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
				throw new BusinessException(ErrorCode.INVALID_INPUT);
			}
			if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
				throw new BusinessException(ErrorCode.INVALID_PASSWORD);
			}
		}

		String formattedPhone = request.getPhone() != null ? MaskingUtil.formatPhone(request.getPhone()) : null;

        userMapper.updateUser(User.builder()
                .userId(userId)
                .name(request.getName())
                .phone(formattedPhone)
                .job(request.getJob())
                .incomeLevelCode(request.getIncomeLevelCode())
                .creditScore(request.getCreditScore())
                .pushEnabled(request.getPushEnabled() != null
                        ? (request.getPushEnabled() ? "Y" : "N") : null)
                .marketingAgree(request.getMarketingAgree() != null
                        ? (request.getMarketingAgree() ? "Y" : "N") : null)
                .build());

		log.info("[내정보수정] userId={}", userId);
	}

	// ================================================================
	// F-26 | 비밀번호 변경 — 재사용 방지 로직 추가
	// ================================================================
	@Transactional
	public void changePassword(Long userId, @Valid PasswordChangeRequest request) {

		// ① 새 비밀번호 확인 일치 검사
		if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
			throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
		}

		// ② 사용자 조회
		User user = userMapper.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		// ③ 현재 비밀번호 검증
		if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
			throw new BusinessException(ErrorCode.INVALID_PASSWORD);
		}

		// ④ 최근 N회 비밀번호 재사용 방지
		List<String> recentHashes = userMapper.findRecentPasswordHashes(userId, PASSWORD_HISTORY_LIMIT);
		boolean isReused = recentHashes.stream()
				.anyMatch(hash -> passwordEncoder.matches(request.getNewPassword(), hash));
		if (isReused) {
			throw new BusinessException(ErrorCode.PASSWORD_RECENTLY_USED);
		}

		// ⑤ 비밀번호 변경
		String newHash = passwordEncoder.encode(request.getNewPassword());
		userMapper.updatePassword(userId, newHash, LocalDateTime.now());

		// ⑥ 이력 저장 + 오래된 이력 정리 (최근 5건만 유지)
		userMapper.insertPasswordHistory(userId, newHash);
		userMapper.deleteOldPasswordHistories(userId);

		// ⑦ 전 기기 세션 무효화
		userMapper.revokeAllSessions(userId);

		log.info("[비밀번호변경] userId={} 변경 완료 (전 기기 로그아웃)", userId);
	}

	// ================================================================
	// RQ-F17 | 보유 카드 및 신청 현황
	// ================================================================
	@Transactional(readOnly = true)
	public CardStatusResponse getMyCards(Long userId) {
		List<UserMapper.OwnedCardRow> ownedCards = userMapper.selectOwnedCards(userId);
		List<UserMapper.CardApplicationRow> applications = userMapper.selectCardApplications(userId);

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
		return request.getName() != null || request.getPhone() != null || request.getJob() != null
				|| request.getIncomeLevelCode() != null || request.getCreditScore() != null;
	}

	private boolean hasAnyField(UserUpdateRequest request) {
		return requiresPasswordVerification(request) || request.getPushEnabled() != null
				|| request.getMarketingAgree() != null;
	}
}