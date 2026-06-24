package com.bnk.domain.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bnk.domain.admin.mapper.WatchlistMapper;
import com.bnk.domain.admin.model.Watchlist;
import com.bnk.domain.user.mapper.UserMapper;
import com.bnk.domain.user.model.User;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CddService {

	private final UserMapper userMapper;
	private final WatchlistMapper watchlistMapper;

	// ================================================================
	// 카드 신청 시 CDD 상태 검증
	// ================================================================
	@Transactional(readOnly = true)
	public void validateCddStatus(Long userId) {
		User user = userMapper.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		String status = user.getCddStatusCode();
		if (status == null || "PENDING".equals(status)) {
			log.warn("[CDD] 검증 미완료 userId={} status={}", userId, status);
			throw new BusinessException(ErrorCode.CDD_NOT_VERIFIED);
		}
		if ("REJECTED".equals(status)) {
			log.warn("[CDD] 검증 거부 userId={} status={}", userId, status);
			throw new BusinessException(ErrorCode.CDD_REJECTED);
		}
		log.info("[CDD] 검증 통과 userId={} status={}", userId, status);
	}

	// ================================================================
	// 회원가입 시 Watchlist 대조
	// SHA-256 해시 → DB 인덱스(ci_value_hash, birth_date_hash) 직접 조회
	// ================================================================
	@Transactional(readOnly = true)
	public void checkWatchlist(String ciValue, String name, String birthDate) {

		// ① CI값 해시로 1차 대조
		if (ciValue != null && !ciValue.isBlank()) {
			watchlistMapper.findByCiValueHash(sha256Hex(ciValue)).ifPresent(w -> {
				log.warn("[Watchlist] CI 일치 차단 name={} watchlistId={}", name, w.getWatchlistId());
				throw new BusinessException(ErrorCode.WATCHLIST_BLOCKED);
			});
		}

		// ② 이름 + 생년월일 해시로 2차 대조
		if (name != null && birthDate != null) {
			List<Watchlist> hits = watchlistMapper.findByNameAndBirthDateHash(name, sha256Hex(birthDate));
			if (!hits.isEmpty()) {
				log.warn("[Watchlist] 이름+생년월일 일치 차단 name={}", name);
				throw new BusinessException(ErrorCode.WATCHLIST_BLOCKED);
			}
		}
	}

	// ================================================================
	// Watchlist 등록 (관리자용)
	// — ciValueHash, birthDateHash를 계산해서 함께 저장
	// ================================================================
	@Transactional
	public void registerWatchlist(Watchlist raw) {
		Watchlist withHash = Watchlist.builder()
				.name(raw.getName())
				.birthDate(raw.getBirthDate())
				.ciValue(raw.getCiValue())
				.ciValueHash(raw.getCiValue()    != null ? sha256Hex(raw.getCiValue())    : null)
				.birthDateHash(raw.getBirthDate() != null ? sha256Hex(raw.getBirthDate()) : null)
				.reason(raw.getReason())
				.riskLevel(raw.getRiskLevel())
				.registeredBy(raw.getRegisteredBy())
				.build();

		watchlistMapper.insert(withHash);
		log.info("[Watchlist] 등록 name={} riskLevel={}", raw.getName(), raw.getRiskLevel());
	}

	// ================================================================
	// CDD 상태 업데이트 (관리자용)
	// ================================================================
	@Transactional
	public void updateCddStatus(Long userId, String newStatus) {
		userMapper.updateCddStatus(userId, newStatus);
		log.info("[CDD] 상태 변경 userId={} → {}", userId, newStatus);
	}

	// ================================================================
	// 회원가입 완료 시 초기 CDD 설정
	// ================================================================
	@Transactional
	public void initializeCdd(Long userId) {
		userMapper.updateCddStatus(userId, "VERIFIED");
	}

	// ================================================================
	// SHA-256 결정론적 해시 (Watchlist 해시 컬럼용)
	// ================================================================
	private String sha256Hex(String value) {
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (Exception e) {
			log.error("[Watchlist] 해시 생성 실패", e);
			throw new IllegalStateException("해시 생성 실패", e);
		}
	}
}