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
     * 생성 방식: SHA-256(이름 + 생년월일(YYMMDD) + 전화번호 + salt) → Base64
     *
     * [변경 이력]
     *  v3: 이름 + 주민번호앞6 + 성별코드 + 주소
     *  v4: 이름 + 생년월일(YYMMDD) + 전화번호  ← 현재
     *      - 주소 제거(주소는 별도 주소록 테이블에서 관리), 전화번호 추가
     *      - 성별코드 제거: USERS에 미저장 → birth_date + phone 만으로 기존 회원 CI 재생성(마이그레이션) 가능
     *
     * @param name        이름
     * @param birthYYMMDD 생년월일 6자리(YYMMDD). 가입/인증 요청의 주민번호 앞 6자리와 동일하며,
     *                    마이그레이션 시에는 birth_date(YYYY-MM-DD)에서 YY+MM+DD로 유도
     * @param phone       전화번호 (숫자만 추출하여 정규화)
     * @return Base64 인코딩된 SHA-256 해시값 (약 44자)
     */
    public String generate(String name, String birthYYMMDD, String phone) {
        if (name == null || birthYYMMDD == null || phone == null) {
            throw new IllegalArgumentException("CI값 생성에 필요한 정보가 누락되었습니다.");
        }

        String nm    = name.trim();
        String birth = birthYYMMDD.replaceAll("[^0-9]", "");
        String ph    = phone.replaceAll("[^0-9]", "");

        if (birth.length() != 6) {
            throw new IllegalArgumentException("생년월일(YYMMDD) 6자리가 올바르지 않습니다.");
        }
        if (ph.length() < 9) {
            throw new IllegalArgumentException("전화번호가 올바르지 않습니다.");
        }

        try {
            // 조합 문자열: 이름|생년월일(YYMMDD)|전화번호(숫자)|salt
            String raw = nm + "|" + birth + "|" + ph + "|" + ciSalt;

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("[CI] 생성 실패", e);
            throw new IllegalStateException("CI값 생성에 실패했습니다.", e);
        }
    }
}