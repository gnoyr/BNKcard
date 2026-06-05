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

/**
 * CDD(고객 확인) 서비스.
 *
 * 1. validateCddStatus()  — 카드 신청 시 CDD 상태 검증
 * 2. checkWatchlist()     — 회원가입 시 Watchlist 대조 (미가입 요주의 인물 차단)
 * 3. updateCddStatus()    — 관리자의 CDD 상태 수동 변경
 * 4. initializeCdd()      — 회원가입 완료 시 초기 CDD 설정
 *
 * [Watchlist 대조 설계 결정]
 * ci_value 컬럼은 AES-256-GCM으로 암호화 저장된다.
 * GCM은 매번 랜덤 IV를 사용하므로 동일 평문도 암호문이 달라
 * WHERE ci_value = ? DB 직접 비교가 불가하다.
 *
 * 해결책: ci_value_hash(SHA-256 결정론적 해시) 컬럼을 WATCHLIST에 추가하여
 * DB 인덱스로 O(1) 조회한다. 이름+생년월일 2차 대조도 동일 패턴 적용.
 *
 * DDL: ALTER TABLE WATCHLIST ADD (
 *          ci_value_hash    VARCHAR2(64),
 *          birth_date_hash  VARCHAR2(64)
 *      );
 *      CREATE INDEX IDX_WATCHLIST_CI_HASH ON WATCHLIST(ci_value_hash);
 *      CREATE INDEX IDX_WATCHLIST_NAME_BD ON WATCHLIST(name, birth_date_hash);
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
    // CI값 SHA-256 해시로 1차 DB 인덱스 조회 →
    // 이름 + 생년월일 해시로 2차 DB 인덱스 조회
    // ================================================================
    @Transactional(readOnly = true)
    public void checkWatchlist(String ciValue, String name, String birthDate) {

        // ① CI값 해시로 1차 대조 — DB 인덱스(IDX_WATCHLIST_CI_HASH) 활용
        if (ciValue != null && !ciValue.isBlank()) {
            String ciHash = sha256Hex(ciValue);
            watchlistMapper.findByCiValueHash(ciHash).ifPresent(w -> {
                log.warn("[Watchlist] CI해시 일치 차단 — name={} watchlistId={}",
                        name, w.getWatchlistId());
                throw new BusinessException(ErrorCode.WATCHLIST_BLOCKED);
            });
        }

        // ② 이름 + 생년월일 해시로 2차 대조 — DB 인덱스(IDX_WATCHLIST_NAME_BD) 활용
        if (name != null && birthDate != null) {
            String birthDateHash = sha256Hex(birthDate);
            List<Watchlist> nameList =
                    watchlistMapper.findByNameAndBirthDateHash(name, birthDateHash);
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

    // ================================================================
    // SHA-256 결정론적 해시 (Watchlist 해시 컬럼 비교용)
    // ================================================================
    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.error("[Watchlist] SHA-256 해시 생성 실패", e);
            throw new IllegalStateException("해시 생성 실패", e);
        }
    }
    
	 // ================================================================
	 // Watchlist 등록 (관리자용) — ci_value_hash, birth_date_hash 자동 계산
	 // ================================================================
	 @Transactional
	 public void registerWatchlist(Watchlist watchlist) {
	     String ciHash        = watchlist.getCiValue()   != null
	             ? sha256Hex(watchlist.getCiValue())   : null;
	     String birthDateHash = watchlist.getBirthDate() != null
	             ? sha256Hex(watchlist.getBirthDate()) : null;
	
	     Watchlist withHash = Watchlist.builder()
	             .name(watchlist.getName())
	             .birthDate(watchlist.getBirthDate())
	             .ciValue(watchlist.getCiValue())
	             .ciValueHash(ciHash)
	             .birthDateHash(birthDateHash)
	             .reason(watchlist.getReason())
	             .riskLevel(watchlist.getRiskLevel())
	             .registeredBy(watchlist.getRegisteredBy())
	             .build();
	
	     watchlistMapper.insert(withHash);
	     log.info("[Watchlist] 등록 완료 — name={} riskLevel={}",
	             watchlist.getName(), watchlist.getRiskLevel());
 }
}