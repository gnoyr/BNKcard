package com.bnk.domain.deviceauth.service;

import com.bnk.domain.deviceauth.dto.response.TrustedDeviceResponse;
import com.bnk.domain.deviceauth.mapper.DeviceTrustMapper;
import com.bnk.domain.deviceauth.model.UserTrustedDevice;
import com.bnk.domain.notification.service.NotificationService;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.util.IpHashUtil;
import com.bnk.global.util.TokenStore;
import com.bnk.global.util.audit.AuditLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 신뢰 기기 관리 서비스 (기기 식별자 기반)
 *
 * [판정 키]
 * 기존 IP 해시 대신 device_id_hash(SHA-256(클라이언트 기기 UUID))로 신뢰 여부를 판정한다.
 * 모바일 IP 변동(WiFi↔LTE)에도 같은 기기는 재인증하지 않는다.
 *
 * [불투명 챌린지 토큰]
 * 미신뢰 기기는 예측 불가능한 난수 토큰을 발급하고, Redis에 챌린지 컨텍스트를 매핑한다.
 *   Redis 키: device:challenge:{token} → value = "userId|deviceId|deviceName|platform|ip" (U+001F 구분)  TTL 15분
 *   Redis 키: device:verify:email:{userId} → value=코드  TTL 10분
 *   Redis 키: device:ci_fail:{userId}     → value=횟수  TTL 30분
 * 클라이언트에는 내부 키 구조가 노출되지 않는다.
 *
 * [새 IP 알림]
 * 신뢰 기기라도 마지막 접속 IP와 다른 IP면 로그인은 통과시키되 알림을 발송한다(차단 없음).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceTrustService {

    private static final int  MAX_TRUSTED_DEVICE_COUNT = 10;
    private static final int  MAX_CI_FAIL_COUNT        = 3;
    private static final long CHALLENGE_TTL_MIN        = 15L;
    private static final long CI_FAIL_TTL_MIN          = 30L;

    /** 챌린지 직렬화 필드 구분자 (Unit Separator, 사용자 입력에 등장하지 않음) */
    private static final String US = "";

    private final DeviceTrustMapper   deviceTrustMapper;
    private final TokenStore          tokenStore;
    private final AuditLogger         auditLogger;
    private final IpHashUtil          hashUtil;       // 범용 SHA-256 (deviceId / IP 공용)
    private final NotificationService notificationService;

    /** 불투명 챌린지 토큰 생성기 (32바이트 → base64url, 패딩 제거) */
    private final Base64StringKeyGenerator tokenGenerator =
            new Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 32);

    // ─── Redis 키 ────────────────────────────────────────────────────

    private static String challengeKey(String token) {
        return "device:challenge:" + token;
    }

    public static String emailVerifyKey(Long userId) {
        return "device:verify:email:" + userId;
    }

    private static String ciFailKey(Long userId) {
        return "device:ci_fail:" + userId;
    }

    // ─── 챌린지 컨텍스트 ──────────────────────────────────────────────

    /** 미신뢰 기기 챌린지 컨텍스트 (Redis 저장/복원용) */
    public record DeviceChallenge(Long userId, String deviceId, String deviceName,
                                  String platform, String ip) {}

    // ─── checkDevice() ───────────────────────────────────────────────

    /**
     * 로그인 시 현재 기기가 신뢰 목록에 있는지 확인.
     *
     * @return empty     → 신뢰 기기 (로그인 정상 진행)
     *         non-empty → 불투명 challengeToken (미등록 기기)
     */
    @Transactional
    public Optional<String> checkDevice(Long userId, String deviceId, String deviceName,
                                        String platform, String currentIp) {
        if (deviceId == null || deviceId.isBlank()) {
            // 기기 식별자 미전송(구/외부 클라이언트) → 게이트 skip, 정상 로그인 진행.
            log.warn("[DeviceTrust] userId={} deviceId 미전송 — 기기 인증 게이트 skip", userId);
            return Optional.empty();
        }

        String deviceHash = hashUtil.hash(deviceId);
        Optional<UserTrustedDevice> trusted =
                deviceTrustMapper.findActiveByUserIdAndDeviceHash(userId, deviceHash);

        if (trusted.isPresent()) {
            UserTrustedDevice dev = trusted.get();
            String currentIpHash = (currentIp == null || currentIp.isBlank()) ? null : hashUtil.hash(currentIp);

            if (currentIpHash != null && !currentIpHash.equals(dev.getLastIpHash())) {
                // 신뢰 기기 + 새 IP → 통과시키되 알림 발송
                deviceTrustMapper.updateLastIp(dev.getDeviceTrustId(), currentIp, currentIpHash);
                notifyNewIpLogin(userId, dev.getDeviceName(), currentIp);
                log.info("[DeviceTrust] userId={} 신뢰 기기 새 IP 감지 → 알림", userId);
            } else {
                deviceTrustMapper.updateLastUsedAt(dev.getDeviceTrustId());
            }
            log.debug("[DeviceTrust] userId={} 신뢰 기기 hash={}", userId, deviceHash);
            return Optional.empty();
        }

        // 미등록 기기 → 불투명 챌린지 토큰 발급
        String token = tokenGenerator.generateKey();
        tokenStore.set(challengeKey(token),
                serialize(new DeviceChallenge(userId, deviceId, deviceName, platform, currentIp)),
                CHALLENGE_TTL_MIN);

        auditLogger.failure(AuditLogger.AUTH, "DEVICE_CHALLENGE", userId, deviceHash, "미등록 기기 접속");
        log.info("[DeviceTrust] userId={} 미등록 기기 챌린지 발급 hash={}", userId, deviceHash);
        return Optional.of(token);
    }

    // ─── registerInitialDevice() ─────────────────────────────────────

    /**
     * 회원가입 직후 최초 기기 자동 등록.
     * AuthController.signup()에서 호출.
     */
    @Transactional
    public void registerInitialDevice(Long userId, String deviceId, String deviceName,
                                      String platform, String plainIp) {
        if (deviceId == null || deviceId.isBlank()) {
            log.warn("[DeviceTrust] userId={} 회원가입 deviceId 미전송 — 최초 기기 미등록", userId);
            return;
        }
        String deviceHash = hashUtil.hash(deviceId);
        String ipHash     = (plainIp == null || plainIp.isBlank()) ? null : hashUtil.hash(plainIp);

        deviceTrustMapper.insert(UserTrustedDevice.builder()
                .userId(userId)
                .deviceIdHash(deviceHash)
                .deviceName(resolveDeviceName(deviceName, "처음 가입한 기기"))
                .platformCode(normalizePlatform(platform))
                .lastIpAddress(plainIp)
                .lastIpHash(ipHash)
                .isInitial("Y")
                .statusCode("ACTIVE")
                .registeredVia("SIGNUP")
                .deletedYn("N")
                .build());

        auditLogger.success(AuditLogger.AUTH, "DEVICE_REGISTER", userId, deviceHash, "회원가입 최초 기기 등록");
        log.info("[DeviceTrust] userId={} 최초 기기 등록 hash={}", userId, deviceHash);
    }

    // ─── approvePendingDevice() ──────────────────────────────────────

    /**
     * 이메일 또는 CI 인증 완료 후 신뢰 기기 등록.
     *
     * @param ch                 챌린지 컨텍스트 (validateChallengeToken 결과)
     * @param registeredVia      EMAIL_VERIFY | CI_VERIFY
     * @param deviceNameOverride 사용자가 인증 화면에서 지정한 기기명 (null이면 챌린지의 기기명 사용)
     */
    @Transactional
    public void approvePendingDevice(DeviceChallenge ch, String registeredVia, String deviceNameOverride) {
        String deviceHash = hashUtil.hash(ch.deviceId());

        if (deviceTrustMapper.countActiveByUserId(ch.userId()) >= MAX_TRUSTED_DEVICE_COUNT) {
            throw new BusinessException(ErrorCode.DEVICE_MAX_LIMIT_EXCEEDED);
        }
        if (deviceTrustMapper.existsByUserIdAndDeviceHash(ch.userId(), deviceHash)) {
            throw new BusinessException(ErrorCode.DEVICE_ALREADY_TRUSTED);
        }

        String name   = resolveDeviceName(
                (deviceNameOverride != null && !deviceNameOverride.isBlank()) ? deviceNameOverride : ch.deviceName(),
                "내 기기");
        String ipHash = (ch.ip() == null || ch.ip().isBlank()) ? null : hashUtil.hash(ch.ip());

        deviceTrustMapper.insert(UserTrustedDevice.builder()
                .userId(ch.userId())
                .deviceIdHash(deviceHash)
                .deviceName(name)
                .platformCode(normalizePlatform(ch.platform()))
                .lastIpAddress(ch.ip())
                .lastIpHash(ipHash)
                .isInitial("N")
                .statusCode("ACTIVE")
                .registeredVia(registeredVia)
                .deletedYn("N")
                .build());

        auditLogger.success(AuditLogger.AUTH, "DEVICE_REGISTER", ch.userId(), deviceHash, registeredVia + " 완료");
        log.info("[DeviceTrust] userId={} 신뢰 기기 등록 hash={} via={}", ch.userId(), deviceHash, registeredVia);
    }

    // ─── 챌린지 토큰 검증/소비 ───────────────────────────────────────

    /**
     * challengeToken 유효성 확인 → 챌린지 컨텍스트 반환 (삭제하지 않음).
     * 만료 또는 없으면 DEVICE_CHALLENGE_EXPIRED 예외.
     */
    public DeviceChallenge validateChallengeToken(String challengeToken) {
        String raw = tokenStore.get(challengeKey(challengeToken));
        if (raw == null) {
            auditLogger.failure(AuditLogger.AUTH, "DEVICE_CHALLENGE_EXPIRED", null, "", "챌린지 토큰 만료");
            throw new BusinessException(ErrorCode.DEVICE_CHALLENGE_EXPIRED);
        }
        return deserialize(raw);
    }

    /** 인증 완료 후 챌린지 토큰 폐기. */
    public void deleteChallenge(String challengeToken) {
        tokenStore.delete(challengeKey(challengeToken));
    }

    // ─── 마이페이지 CRUD ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TrustedDeviceResponse> getTrustedDevices(Long userId) {
        return deviceTrustMapper.findAllByUserId(userId).stream()
                .map(TrustedDeviceResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateDeviceName(Long userId, Long deviceTrustId, String newName) {
        int updated = deviceTrustMapper.updateDeviceName(deviceTrustId, userId, newName);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_FOUND);
        }
    }

    /**
     * 신뢰 기기 논리 삭제.
     * is_initial='Y' → DEVICE_INITIAL_DELETE_FORBIDDEN
     */
    @Transactional
    public void deleteTrustedDevice(Long userId, Long deviceTrustId) {
        UserTrustedDevice target = deviceTrustMapper.findByDeviceTrustIdAndUserId(deviceTrustId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));

        if (target.isInitialDevice()) {
            throw new BusinessException(ErrorCode.DEVICE_INITIAL_DELETE_FORBIDDEN);
        }

        deviceTrustMapper.softDelete(deviceTrustId, userId);
        auditLogger.success(AuditLogger.AUTH, "DEVICE_DELETE", userId,
                target.getDeviceIdHash(), "deviceTrustId=" + deviceTrustId);
        log.info("[DeviceTrust] userId={} 기기 삭제 hash={} deviceTrustId={}",
                userId, target.getDeviceIdHash(), deviceTrustId);
    }

    // ─── CI 실패 횟수 ────────────────────────────────────────────────

    public void incrementCiFailCount(Long userId) {
        String key     = ciFailKey(userId);
        String current = tokenStore.get(key);
        long   count   = (current == null) ? 1L : Long.parseLong(current) + 1L;
        tokenStore.set(key, String.valueOf(count), CI_FAIL_TTL_MIN);

        log.warn("[DeviceTrust] userId={} CI 실패 횟수={}", userId, count);
        if (count >= MAX_CI_FAIL_COUNT) {
            auditLogger.failure(AuditLogger.AUTH, "CI_FAIL_LOCKED", userId, "", "CI 실패 " + count + "회");
            throw new BusinessException(ErrorCode.CI_LOCKED);
        }
    }

    public void resetCiFailCount(Long userId) {
        tokenStore.delete(ciFailKey(userId));
    }

    // ─── 유틸 ────────────────────────────────────────────────────────

    /** 신뢰 기기 새 IP 로그인 알림 (실패해도 로그인은 계속 진행). */
    private void notifyNewIpLogin(Long userId, String deviceName, String ip) {
        try {
            notificationService.notifyNewDeviceLogin(userId, deviceName, maskIp(ip));
        } catch (Exception e) {
            log.warn("[DeviceTrust] 새 IP 알림 실패 userId={}: {}", userId, e.getMessage());
        }
    }

    private String serialize(DeviceChallenge c) {
        return c.userId()
                + US + nullSafe(c.deviceId())
                + US + sanitize(c.deviceName())
                + US + nullSafe(c.platform())
                + US + nullSafe(c.ip());
    }

    private DeviceChallenge deserialize(String raw) {
        String[] p = raw.split(US, -1);
        Long userId = Long.parseLong(p[0]);
        return new DeviceChallenge(
                userId,
                emptyToNull(p.length > 1 ? p[1] : null),
                emptyToNull(p.length > 2 ? p[2] : null),
                emptyToNull(p.length > 3 ? p[3] : null),
                emptyToNull(p.length > 4 ? p[4] : null));
    }

    private static String resolveDeviceName(String name, String fallback) {
        String n = sanitize(name);
        return (n == null || n.isBlank()) ? fallback : n.substring(0, Math.min(n.length(), 100));
    }

    private static String normalizePlatform(String platform) {
        if (platform == null) return "UNKNOWN";
        String p = platform.trim().toUpperCase();
        return switch (p) {
            case "IOS", "ANDROID", "WEB" -> p;
            default -> "UNKNOWN";
        };
    }

    private static String sanitize(String s) {
        return s == null ? null : s.replace(US, " ").trim();
    }

    private static String nullSafe(String s) { return s == null ? "" : s; }
    private static String emptyToNull(String s) { return (s == null || s.isEmpty()) ? null : s; }

    /** 알림 본문용 간단 IP 마스킹 (192.168.1.10 → 192.168.*.*). */
    private static String maskIp(String ip) {
        if (ip == null) return "알 수 없음";
        if (ip.contains(".") && !ip.contains(":")) {
            String[] parts = ip.split("\\.", -1);
            if (parts.length == 4) return parts[0] + "." + parts[1] + ".*.*";
        }
        int c = ip.indexOf(':');
        if (c > 0) return ip.substring(0, c) + ":*:*:*:*:*:*:*";
        return "*.*.*.*";
    }
}
