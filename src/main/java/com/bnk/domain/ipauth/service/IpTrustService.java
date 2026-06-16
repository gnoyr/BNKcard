package com.bnk.domain.ipauth.service;

import com.bnk.domain.ipauth.dto.response.TrustedIpResponse;
import com.bnk.domain.ipauth.mapper.IpTrustMapper;
import com.bnk.domain.ipauth.model.UserTrustedIp;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.util.IpHashUtil;
import com.bnk.global.util.TokenStore;
import com.bnk.global.util.audit.AuditLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * IP 신뢰 기기 관리 서비스
 *
 * [IP 암호화 전략]
 * 저장: IpTrustMapper.xml → typeHandler=AesTypeHandler → AES-256-GCM 자동 암호화
 * 조회: IpHashUtil.hash(plainIp) → ip_address_hash(SHA-256) 컬럼으로 WHERE 조건
 * 반환: resultMap aesTypeHandler 자동 복호화 → 서비스 계층은 평문으로 사용
 *
 * [Redis 키 — 평문 IP 미포함, ipHash 사용]
 * ip:challenge:{userId}:{ipHash}  value=평문IP  TTL 15분
 * ip:verify:email:{userId}        value=코드    TTL 10분
 * ip:approved:{userId}:{ipHash}   value="Y"    TTL 30분
 * ip:ci_fail:{userId}             value=횟수   TTL 30분
 *
 * [TokenStore 시그니처 — 기존 인터페이스 그대로]
 * set(String key, String value, long ttlMinutes)
 * get(String key) → String | null
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IpTrustService {

    private static final int  MAX_TRUSTED_IP_COUNT = 10;
    private static final int  MAX_CI_FAIL_COUNT    = 3;
    private static final long CHALLENGE_TTL_MIN    = 15L;
    private static final long APPROVED_TTL_MIN     = 30L;
    private static final long CI_FAIL_TTL_MIN      = 30L;

    private final IpTrustMapper ipTrustMapper;
    private final TokenStore    tokenStore;
    private final AuditLogger   auditLogger;
    private final IpHashUtil    ipHashUtil;

    // ─── Redis 키 ────────────────────────────────────────────────────

    public static String challengeKey(Long userId, String ipHash) {
        return "ip:challenge:" + userId + ":" + ipHash;
    }

    public static String emailVerifyKey(Long userId) {
        return "ip:verify:email:" + userId;
    }

    private static String approvedKey(Long userId, String ipHash) {
        return "ip:approved:" + userId + ":" + ipHash;
    }

    private static String ciFailKey(Long userId) {
        return "ip:ci_fail:" + userId;
    }

    // ─── checkIp() ───────────────────────────────────────────────────

    /**
     * 로그인 시 현재 IP가 신뢰 목록에 있는지 확인.
     *
     * @return empty    → 신뢰 IP (로그인 정상 진행)
     *         non-empty → challengeToken (미등록 IP)
     */
    @Transactional
    public Optional<String> checkIp(Long userId, String currentIp) {
        String ipHash = ipHashUtil.hash(currentIp);

        Optional<UserTrustedIp> trusted =
                ipTrustMapper.findActiveByUserIdAndIpHash(userId, ipHash);

        if (trusted.isPresent()) {
            ipTrustMapper.updateLastUsedAt(trusted.get().getTrustId());
            log.debug("[IpTrust] userId={} 신뢰 IP hash={}", userId, ipHash);
            return Optional.empty();
        }

        // key=ipHash(평문 IP 미노출), value=평문IP(approvePendingIp에서 사용)
        String challengeToken = challengeKey(userId, ipHash);
        tokenStore.set(challengeToken, currentIp, CHALLENGE_TTL_MIN);

        auditLogger.failure(AuditLogger.AUTH, "IP_CHALLENGE", userId, ipHash, "미등록 IP 접속");
        log.info("[IpTrust] userId={} 미등록 IP 챌린지 발급 hash={}", userId, ipHash);
        return Optional.of(challengeToken);
    }

    // ─── registerInitialIp() ─────────────────────────────────────────

    /**
     * 회원가입 직후 최초 IP 자동 등록.
     * AuthController.signup()에서 httpRequest.getRemoteAddr()를 받아 호출.
     */
    @Transactional
    public void registerInitialIp(Long userId, String plainIp) {
        String ipHash = ipHashUtil.hash(plainIp);

        ipTrustMapper.insert(UserTrustedIp.builder()
                .userId(userId)
                .ipAddress(plainIp)      // XML → aesTypeHandler → AES 암호화 저장
                .ipAddressHash(ipHash)   // SHA-256 hex → 평문 저장
                .nickname("처음 가입한 기기")
                .isInitial("Y")
                .statusCode("ACTIVE")
                .registeredVia("SIGNUP")
                .deletedYn("N")
                .build());

        auditLogger.success(AuditLogger.AUTH, "IP_REGISTER", userId, ipHash, "회원가입 최초 IP 등록");
        log.info("[IpTrust] userId={} 최초 IP 등록 hash={}", userId, ipHash);
    }

    // ─── approvePendingIp() ──────────────────────────────────────────

    /**
     * 이메일 또는 CI 인증 완료 후 신뢰 IP 등록 + 로그인 허가.
     *
     * @param plainIp       평문 IP (challengeToken의 Redis value에서 꺼낸 값)
     * @param registeredVia EMAIL_VERIFY | CI_VERIFY
     * @param nickname      null이면 '내 기기' 기본값 적용
     */
    @Transactional
    public void approvePendingIp(Long userId, String plainIp,
                                 String registeredVia, String nickname) {
        String ipHash = ipHashUtil.hash(plainIp);

        if (ipTrustMapper.countActiveByUserId(userId) >= MAX_TRUSTED_IP_COUNT) {
            throw new BusinessException(ErrorCode.IP_MAX_LIMIT_EXCEEDED);
        }
        if (ipTrustMapper.existsByUserIdAndIpHash(userId, ipHash)) {
            throw new BusinessException(ErrorCode.IP_ALREADY_TRUSTED);
        }

        ipTrustMapper.insert(UserTrustedIp.builder()
                .userId(userId)
                .ipAddress(plainIp)
                .ipAddressHash(ipHash)
                .nickname((nickname == null || nickname.isBlank()) ? "내 기기" : nickname)
                .isInitial("N")
                .statusCode("ACTIVE")
                .registeredVia(registeredVia)
                .deletedYn("N")
                .build());

        tokenStore.set(approvedKey(userId, ipHash), "Y", APPROVED_TTL_MIN);
        tokenStore.delete(challengeKey(userId, ipHash));

        auditLogger.success(AuditLogger.AUTH, "IP_REGISTER", userId, ipHash, registeredVia + " 완료");
        log.info("[IpTrust] userId={} 신뢰 IP 등록 hash={} via={}", userId, ipHash, registeredVia);
    }

    // ─── 마이페이지 CRUD ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TrustedIpResponse> getTrustedIps(Long userId) {
        return ipTrustMapper.findAllByUserId(userId).stream()
                .map(TrustedIpResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateNickname(Long userId, Long trustId, String newNickname) {
        int updated = ipTrustMapper.updateNickname(trustId, userId, newNickname);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.IP_CHALLENGE_EXPIRED);
        }
    }

    /**
     * 신뢰 IP 논리 삭제.
     * is_initial='Y' → IP_INITIAL_DELETE_FORBIDDEN
     */
    @Transactional
    public void deleteTrustedIp(Long userId, Long trustId) {
        UserTrustedIp target = ipTrustMapper.findByTrustIdAndUserId(trustId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IP_CHALLENGE_EXPIRED));

        if (target.isInitialDevice()) {
            throw new BusinessException(ErrorCode.IP_INITIAL_DELETE_FORBIDDEN);
        }

        ipTrustMapper.softDelete(trustId, userId);
        auditLogger.success(AuditLogger.AUTH, "IP_DELETE", userId,
                target.getIpAddressHash(), "trust_id=" + trustId);
        log.info("[IpTrust] userId={} IP 삭제 hash={} trustId={}",
                userId, target.getIpAddressHash(), trustId);
    }

    // ─── 챌린지 토큰 검증 ────────────────────────────────────────────

    /**
     * challengeToken 유효성 확인 → Redis value(평문 IP) 반환.
     * 만료 또는 없으면 IP_CHALLENGE_EXPIRED 예외.
     */
    public String validateChallengeToken(Long userId, String challengeToken) {
        String plainIp = tokenStore.get(challengeToken);
        if (plainIp == null) {
            auditLogger.failure(AuditLogger.AUTH, "IP_CHALLENGE_EXPIRED", userId, "", "챌린지 토큰 만료");
            throw new BusinessException(ErrorCode.IP_CHALLENGE_EXPIRED);
        }
        return plainIp;
    }

    // ─── CI 실패 횟수 ────────────────────────────────────────────────

    /**
     * CI 인증 실패 횟수 증가.
     * TokenStore.get()은 null 반환, increment 없음 → get→parse→set 패턴.
     */
    public void incrementCiFailCount(Long userId) {
        String key     = ciFailKey(userId);
        String current = tokenStore.get(key);
        long   count   = (current == null) ? 1L : Long.parseLong(current) + 1L;
        tokenStore.set(key, String.valueOf(count), CI_FAIL_TTL_MIN);

        log.warn("[IpTrust] userId={} CI 실패 횟수={}", userId, count);
        if (count >= MAX_CI_FAIL_COUNT) {
            auditLogger.failure(AuditLogger.AUTH, "CI_FAIL_LOCKED", userId, "", "CI 실패 " + count + "회");
            throw new BusinessException(ErrorCode.CI_LOCKED);
        }
    }

    public void resetCiFailCount(Long userId) {
        tokenStore.delete(ciFailKey(userId));
    }
}
