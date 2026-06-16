package com.bnk.global.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Mock CI(연계정보) 값 생성 유틸.
 *
 * 실제 금융 시스템에서 CI값은 행정안전부 또는 이동통신사의 본인인증을 통해
 * 발급받는 88자리 고유값(SHA-256 기반)이다.
 * 이 프로젝트에서는 외부 연동 없이 아래 정보를 조합하여 Mock CI를 생성한다.
 *
 * 생성 방식: SHA-256(이름 + 생년월일 + 전화번호 + salt) → Base64 인코딩
 * 특징:
 *  - 동일 입력 → 항상 동일 CI값 (재가입 방지 등에 활용 가능)
 *  - 단방향 해시 → 원본 복원 불가
 *  - Watchlist 대조 시: 가입 시도자의 CI와 Watchlist의 CI를 비교
 *
 * ※ 실제 서비스에서는 PASS, 카카오 인증 등 외부 본인인증으로 대체할 것
 */
@Slf4j
@Component
public class CiValueGenerator {

    /** CI 생성 시 사용하는 서버 고유 salt — application.properties에서 주입 */
    private final String ciSalt;

    public CiValueGenerator(@Value("${ci.salt}") String ciSalt) {
        this.ciSalt = ciSalt;
    }

    /**
     * Mock CI값 생성.
     *
     * @param name      이름
     * @param birthDate 생년월일 (YYMMDD)
     * @param phone     전화번호 (포맷 무관, 숫자만 사용)
     * @return Base64 인코딩된 SHA-256 해시값 (약 44자)
     */
    public String generate(String name, String birthDate, String phone) {
        if (name == null || birthDate == null || phone == null) {
            throw new IllegalArgumentException("CI값 생성에 필요한 정보가 누락되었습니다.");
        }
        try {
            // 전화번호에서 숫자만 추출
            String phoneDigits = phone.replaceAll("[^0-9]", "");

            // 조합 문자열
            String raw = name + "|" + birthDate + "|" + phoneDigits + "|" + ciSalt;

            // SHA-256 해시
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("[CI] 생성 실패", e);
            throw new IllegalStateException("CI값 생성에 실패했습니다.", e);
        }
    }
}