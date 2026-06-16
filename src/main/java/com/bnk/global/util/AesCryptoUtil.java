package com.bnk.global.util;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * AES-256-GCM 양방향 암호화 유틸.
 */
@Slf4j
@Component
public class AesCryptoUtil {

    /**
     *
     * AES/GCM/NoPadding 선택 이유:
     *  - AES-256: NIST 승인 대칭키 알고리즘, 현재 가장 안전한 표준.
     *  - GCM(Galois/Counter Mode): 인증된 암호화(AEAD) 제공.
     *    암호문 무결성·인증 태그(128-bit)를 함께 생성하므로
     *    ECB/CBC 대비 변조 탐지 가능.
     *  - NoPadding: GCM은 스트림 모드이므로 패딩 불필요.
     *
     */
    private static final String AES_ALGORITHM     = "AES/GCM/NoPadding"; // Compliant: 상수화된 승인 알고리즘
    private static final String SECRET_KEY_ALGORITHM = "AES";            // SecretKeySpec 알고리즘

    private static final int GCM_IV_LEN       = 12;  // 96-bit IV (NIST 권장)
    private static final int GCM_TAG_BITS     = 128; // 16-byte 인증 태그
    private static final int AES_KEY_BYTES    = 32;  // AES-256 = 32바이트

    /** 암호문 최소 길이: GCM 태그(16) + 평문 최소 1byte = 17 */
    private static final int MIN_CIPHER_BYTES = GCM_TAG_BITS / 8 + 1;

    private final SecretKey    secretKey;

    private final SecureRandom secureRandom = new SecureRandom(); // Compliant: SecureRandom 사용

    public AesCryptoUtil(@Value("${aes.secret-key}") String rawKey) {
        byte[] keyBytes   = rawKey.getBytes(StandardCharsets.UTF_8);
        byte[] normalized = new byte[AES_KEY_BYTES];
        System.arraycopy(keyBytes, 0, normalized, 0, Math.min(keyBytes.length, AES_KEY_BYTES));
        this.secretKey = new SecretKeySpec(normalized, SECRET_KEY_ALGORITHM);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 암호화
    // ─────────────────────────────────────────────────────────────────────────
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) return plainText;
        try {
            byte[] iv = new byte[GCM_IV_LEN];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM); 
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(iv)
                + ":"
                + Base64.getEncoder().encodeToString(encrypted);

        } catch (Exception e) {
            log.error("[AES] 암호화 실패", e);
            throw new IllegalStateException("암호화 실패", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 복호화
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * 암호문을 복호화한다.
     * isEncrypted() = false 이면 평문으로 판단하여 원문 반환 (WARN 로그 없음).
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isBlank()) return encryptedText;

        if (!isEncrypted(encryptedText)) {
            return encryptedText;
        }

        try {
            String[] parts = encryptedText.split(":", 2);
            byte[]   iv    = Base64.getDecoder().decode(parts[0]);
            byte[]   enc   = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(enc), StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("[AES] 복호화 실패 — 키 불일치 또는 데이터 손상 의심. value(앞30자)={}",
                encryptedText.substring(0, Math.min(30, encryptedText.length())), e);
            return encryptedText;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 암호화 포맷 판별
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * 값이 AES-256-GCM 암호문 포맷인지 엄격하게 검사한다.
     *
     * 조건:
     *  1) null·빈값 → false
     *  2) ':' 가 정확히 1개
     *  3) 앞부분(IV)이 유효한 Base64이며 디코딩 결과가 정확히 12바이트
     *  4) 뒷부분(암호문)이 유효한 Base64이며 디코딩 결과가 최소 17바이트
     *
     * URL(https://...), 이메일(a@b.com), 날짜(2024-01-01) 등은 모두 false.
     */
    public boolean isEncrypted(String value) {
        if (value == null || value.isBlank()) return false;

        int colonIdx = value.indexOf(':');
        if (colonIdx < 0) return false;
        if (value.indexOf(':', colonIdx + 1) >= 0) return false;

        String ivPart  = value.substring(0, colonIdx);
        String encPart = value.substring(colonIdx + 1);

        try {
            byte[] ivBytes  = Base64.getDecoder().decode(ivPart);
            byte[] encBytes = Base64.getDecoder().decode(encPart);
            return ivBytes.length == GCM_IV_LEN && encBytes.length >= MIN_CIPHER_BYTES;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 직접 복호화 (예외 throw 버전 — 마이그레이션 서비스 전용)
    // ─────────────────────────────────────────────────────────────────────────
    public String decryptDirect(String encryptedText) throws Exception {
        if (encryptedText == null || encryptedText.isBlank()) return encryptedText;

        int colonIdx = encryptedText.indexOf(':');
        if (colonIdx < 0) throw new IllegalArgumentException("콜론 없음 — 암호문 아님");
        if (encryptedText.indexOf(':', colonIdx + 1) >= 0)
            throw new IllegalArgumentException("콜론 2개 이상 — 암호문 아님");

        String ivPart  = encryptedText.substring(0, colonIdx);
        String encPart = encryptedText.substring(colonIdx + 1);

        byte[] iv  = Base64.getDecoder().decode(ivPart);
        byte[] enc = Base64.getDecoder().decode(encPart);

        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
        return new String(cipher.doFinal(enc), StandardCharsets.UTF_8);
    }
}