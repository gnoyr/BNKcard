package com.bnk.global.util;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MemoryTokenStore {

	private static class TokenWrapper {
		final String value;
		final LocalDateTime expiresAt;

		TokenWrapper(String value, long ttlMinutes) {
			this.value = value;
			this.expiresAt = LocalDateTime.now().plusMinutes(ttlMinutes);
		}

		boolean isExpired() {
			return LocalDateTime.now().isAfter(expiresAt);
		}
	}

	private final Map<String, TokenWrapper> storage = new ConcurrentHashMap<>();

	// 데이터 저장 (set)
	public void set(String key, String value, long ttlMinutes) {
		storage.put(key, new TokenWrapper(value, ttlMinutes));
	}

	// 데이터 조회 (get)
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
	public void delete(String key) {
		storage.remove(key);
	}

	// 주 주기적 만료 데이터 청소 (매 5분마다 메모리 누수 방지용 백그라운드 정리)
	@Scheduled(fixedRate = 300000)
	public void cleanUpExpiredTokens() {
		storage.entrySet().removeIf(entry -> entry.getValue().isExpired());
	}
}