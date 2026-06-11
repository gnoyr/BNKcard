package com.bnk.global.util;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * [TODO: Redis 전환 필요] ─────────────────────────────────────────────────────────
 *
 * 기능명세서 RQ-F06, F-21, F-22 에서 이메일 인증 토큰 / 비밀번호 재설정 토큰을
 * Redis TTL 기반으로 관리하도록 명시하고 있다.
 *
 * 현재 구현은 ConcurrentHashMap(인메모리)을 사용하며, 아래 두 가지 운영 위험이 있다:
 *
 *  1) 서버 재시작 시 토큰 전체 소실
 *     → 사용자가 이메일 인증 완료 후 회원가입 시도 시 EMAIL_NOT_VERIFIED 오류 발생
 *
 *  2) 로드밸런서 환경(멀티 인스턴스) 에서 인스턴스 간 토큰 공유 불가
 *     → 인증 요청을 받은 인스턴스 A와 회원가입 요청을 받은 인스턴스 B가 다르면 인증 실패
 *
 * 운영 환경 전환 전 반드시 아래 절차로 Redis 교체 필요:
 *  1. build.gradle에 spring-boot-starter-data-redis 추가
 *  2. application.properties에 spring.data.redis.host/port 설정
 *  3. 이 클래스를 RedisTemplate<String, String> 또는 StringRedisTemplate 기반으로 교체
 *     - set()  → redisTemplate.opsForValue().set(key, value, ttlMinutes, TimeUnit.MINUTES)
 *     - get()  → redisTemplate.opsForValue().get(key)
 *     - delete() → redisTemplate.delete(key)
 *     - cleanUpExpiredTokens() → Redis TTL이 자동 만료하므로 스케줄러 불필요
 *
 * ────────────────────────────────────────────────────────────────────────────────
 */
@Component
public class MemoryTokenStore implements TokenStore {
	
	private final Map<String, TokenWrapper> storage = new ConcurrentHashMap<>();
	
	private static class TokenWrapper {
		final String value;
		final LocalDateTime expiresAt;

		TokenWrapper(String value, long ttlMinutes) {
			this.value = value;
			this.expiresAt = LocalDateTime.now(TimeConstants.KST).plusMinutes(ttlMinutes);
		}

		boolean isExpired() {
			return LocalDateTime.now(TimeConstants.KST).isAfter(expiresAt);
		}
	}

	// 데이터 저장 (set)
	// Redis 전환 시: redisTemplate.opsForValue().set(key, value, ttlMinutes, TimeUnit.MINUTES)
	public void set(String key, String value, long ttlMinutes) {
		storage.put(key, new TokenWrapper(value, ttlMinutes));
	}

	// 데이터 조회 (get)
	// Redis 전환 시: redisTemplate.opsForValue().get(key)
	public String get(String key) {
		TokenWrapper wrapper = storage.get(key);
		if (wrapper == null) {
			return null;
		}
		if (wrapper.isExpired()) {
			storage.remove(key);
			return null;
		}
		return wrapper.value;
	}

	// 데이터 삭제 (delete)
	// Redis 전환 시: redisTemplate.delete(key)
	public void delete(String key) {
		storage.remove(key);
	}

	// 주기적 만료 데이터 청소 (매 5분마다 메모리 누수 방지용 백그라운드 정리)
	// Redis 전환 시: TTL 자동 만료로 이 스케줄러 불필요 → 삭제
	@Scheduled(fixedRate = 300000)
	public void cleanUpExpiredTokens() {
		storage.entrySet().removeIf(entry -> entry.getValue().isExpired());
	}
}