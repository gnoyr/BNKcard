package com.bnk.domain.application.service;

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

/**
 * CDD(고객 확인) 서비스.
 *
 * 1. validateCddStatus()    — 카드 신청 시 CDD 상태 검증
 * 2. checkWatchlist()       — 회원가입 시 Watchlist 대조 (미가입 요주의 인물 차단)
 * 3. updateCddStatus()      — 관리자의 CDD 상태 수동 변경
 */
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
        User user = userMapper.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

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
    // CI값 우선 비교 → 없으면 이름+생년월일 비교
    // ================================================================
    @Transactional(readOnly = true)
    public void checkWatchlist(String ciValue, String name, String birthDate) {

        // CI값으로 1차 대조 (가장 정확)
        if (ciValue != null && !ciValue.isBlank()) {
            List<Watchlist> ciList = watchlistMapper.findAll().stream()
                    .filter(w -> ciValue.equals(w.getCiValue()))   // TypeHandler 복호화 후 비교
                    .toList();
            if (!ciList.isEmpty()) {
                log.warn("[Watchlist] CI값 일치 차단 — name={}", name);
                throw new BusinessException(ErrorCode.WATCHLIST_BLOCKED);
            }
        }

        // 이름 + 생년월일로 2차 대조 (CI값 없는 경우 fallback)
        if (name != null && birthDate != null) {
            List<Watchlist> nameList = watchlistMapper.findByNameAndBirthDate(name, birthDate)
                    .stream()
                    .filter(w -> birthDate.equals(w.getBirthDate()))  // TypeHandler 복호화 후 비교
                    .toList();
            if (!nameList.isEmpty()) {
                log.warn("[Watchlist] 이름+생년월일 일치 차단 — name={}", name);
                throw new BusinessException(ErrorCode.WATCHLIST_BLOCKED);
            }
        }
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
    // 회원가입 완료 시 초기 CDD 설정 (AuthService에서 호출)
    // ================================================================
    @Transactional
    public void initializeCdd(Long userId) {
        userMapper.updateCddStatus(userId, "VERIFIED");
        log.info("[CDD] 초기화 완료 userId={} → VERIFIED", userId);
    }
}