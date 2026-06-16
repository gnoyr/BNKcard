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
 * 생성 방식: SHA-256(이름 + 주민번호(앞6자리+성별코드) + 주소 + salt) → Base64 인코딩
 *
 * [변경 이력]
 *  v1: 이름 + 생년월일 + 전화번호 → v2: 이름 + 주민번호(front+genderCode) + 주소
 *  변경 이유: identity-verify.js에서 실제 수집하는 데이터와 CI 생성 입력값 일치
 *
 * 특징:
 *  - 동일 입력 → 항상 동일 CI값 (재가입 방지 등에 활용 가능)
 *  - 단방향 해시 → 원본 복원 불가
 *  - residentFront + genderCode = 주민번호 앞 7자리 (뒷 6자리는 수집 안 함)
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
     * @param name          이름
     * @param residentFront 주민번호 앞 6자리 (YYMMDD)
     * @param genderCode    주민번호 뒷자리 첫 번째 (성별코드: 1~4 또는 7~8)
     * @param address       주소 (도로명 또는 지번, 공백 정규화 후 사용)
     * @return Base64 인코딩된 SHA-256 해시값 (약 44자)
     */
    public String generate(String name, String residentFront, String genderCode, String address) {
        if (name == null || residentFront == null || genderCode == null || address == null) {
            throw new IllegalArgumentException("CI값 생성에 필요한 정보가 누락되었습니다.");
        }

        // 주민번호 앞 6자리는 숫자만, 성별코드는 trim
        String front   = residentFront.replaceAll("[^0-9]", "");
        String gender  = genderCode.trim();
        // 주소는 연속 공백 제거 + trim (입력값 정규화)
        String addr    = address.trim().replaceAll("\\s+", " ");

        if (front.length() != 6) {
            throw new IllegalArgumentException("주민번호 앞 6자리가 올바르지 않습니다.");
        }
        if (!gender.matches("[1-4789]")) {
            throw new IllegalArgumentException("성별코드가 올바르지 않습니다. (1~4, 7, 8 중 하나)");
        }
        if (addr.isBlank()) {
            throw new IllegalArgumentException("주소가 올바르지 않습니다.");
        }

        try {
            // 조합 문자열: 이름|주민번호앞6자리+성별코드|주소|salt
            String raw = name + "|" + front + gender + "|" + addr + "|" + ciSalt;

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("[CI] 생성 실패", e);
            throw new IllegalStateException("CI값 생성에 실패했습니다.", e);
        }
    }
}