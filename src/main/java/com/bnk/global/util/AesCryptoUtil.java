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
 *
 * [저장 형식] Base64(IV):Base64(암호문+GCM태그)
 *
 */
@Slf4j
@Component
public class AesCryptoUtil {

	private static final String ALGORITHM = "AES/GCM/NoPadding";
	private static final int GCM_IV_LEN  = 12;   // 96-bit IV
	private static final int GCM_TAG_BITS = 128;  // 16-byte GCM tag
	/** 암호문 최소 길이: GCM 태그(16) + 평문 최소 1byte = 17 */
	private static final int MIN_CIPHER_BYTES = GCM_TAG_BITS / 8 + 1;

	private final SecretKey   secretKey;
	private final SecureRandom secureRandom = new SecureRandom();

	public AesCryptoUtil(@Value("${aes.secret-key}") String rawKey) {
		byte[] keyBytes   = rawKey.getBytes(StandardCharsets.UTF_8);
		byte[] normalized = new byte[32];
		System.arraycopy(keyBytes, 0, normalized, 0, Math.min(keyBytes.length, 32));
		this.secretKey = new SecretKeySpec(normalized, "AES");
	}

	// ─────────────────────────────────────────────────────────────────────────
	// 암호화
	// ─────────────────────────────────────────────────────────────────────────
	public String encrypt(String plainText) {
		if (plainText == null || plainText.isBlank()) return plainText;
		try {
			byte[] iv = new byte[GCM_IV_LEN];
			secureRandom.nextBytes(iv);
			Cipher cipher = Cipher.getInstance(ALGORITHM);
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

		// ★ 핵심 변경: 엄격한 포맷 검사 먼저 수행
		if (!isEncrypted(encryptedText)) {
			// 평문(URL, 일반 텍스트 등)은 그대로 반환 — WARN 로그 없음
			return encryptedText;
		}

		try {
			String[] parts = encryptedText.split(":", 2);
			byte[]   iv    = Base64.getDecoder().decode(parts[0]);
			byte[]   enc   = Base64.getDecoder().decode(parts[1]);
			Cipher   cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
			return new String(cipher.doFinal(enc), StandardCharsets.UTF_8);
		} catch (Exception e) {
			// 여기까지 오면 진짜 오류 (키 불일치, 데이터 손상 등) → ERROR 레벨
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

		// ':' 개수가 정확히 1개인지 검사 (URL은 https://로 2개 이상)
		int colonIdx = value.indexOf(':');
		if (colonIdx < 0) return false;                      // ':' 없음
		if (value.indexOf(':', colonIdx + 1) >= 0) return false; // ':' 2개 이상

		String ivPart  = value.substring(0, colonIdx);
		String encPart = value.substring(colonIdx + 1);

		try {
			byte[] ivBytes  = Base64.getDecoder().decode(ivPart);
			byte[] encBytes = Base64.getDecoder().decode(encPart);
			// IV: 반드시 12바이트, 암호문: 최소 17바이트(태그 16 + 평문 1)
			return ivBytes.length == GCM_IV_LEN && encBytes.length >= MIN_CIPHER_BYTES;
		} catch (IllegalArgumentException e) {
			// Base64 디코딩 실패 → 평문
			return false;
		}
	}
}
