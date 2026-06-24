package com.bnk.global.util;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis 기반 토큰 저장소.
 * MemoryTokenStore 를 대체하며 TTL 은 Redis 내부에서 자동 처리된다.
 *
 * 키 규칙 (AuthService 기존 상수 그대로 사용):
 *   email:verify:{email}   — 인증코드  TTL 10분
 *   email:verified:{email} — 인증완료  TTL 30분
 *   pw:reset:{uuid}        — 비밀번호 재설정 TTL 30분
 */
@Slf4j
@Primary          // MemoryTokenStore 보다 우선 등록
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true")
public class RedisTokenStore implements TokenStore {

    private final StringRedisTemplate redisTemplate;

    /** 값 저장 (TTL 분 단위) */
    @Override
    public void set(String key, String value, long ttlMinutes) {
        redisTemplate.opsForValue().set(key, value, ttlMinutes, TimeUnit.MINUTES);
        log.debug("[RedisTokenStore] SET key={} ttl={}min", key, ttlMinutes);
    }

    /** 값 조회 — 없거나 만료 시 null */
    @Override
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /** 키 삭제 */
    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
        log.debug("[RedisTokenStore] DEL key={}", key);
    }
}