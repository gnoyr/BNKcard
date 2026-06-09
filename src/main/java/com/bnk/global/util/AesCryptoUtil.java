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
 * AES-256-GCM 양방향 암호화 유틸. 저장 형식: Base64(IV):Base64(암호문+GCM태그) isEncrypted()로
 * 평문/암호문 구별 → 마이그레이션 이중 암호화 방지
 */
@Slf4j
@Component
public class AesCryptoUtil {

	private static final String ALGORITHM = "AES/GCM/NoPadding";
	private static final int GCM_IV_LEN = 12;
	private static final int GCM_TAG_BITS = 128;

	private final SecretKey secretKey;
	private final SecureRandom secureRandom = new SecureRandom();

	public AesCryptoUtil(@Value("${aes.secret-key}") String rawKey) {
		byte[] keyBytes = rawKey.getBytes(StandardCharsets.UTF_8);
		byte[] normalized = new byte[32];
		System.arraycopy(keyBytes, 0, normalized, 0, Math.min(keyBytes.length, 32));
		this.secretKey = new SecretKeySpec(normalized, "AES");
	}

	public String encrypt(String plainText) {
		if (plainText == null || plainText.isBlank())
			return plainText;
		try {
			byte[] iv = new byte[GCM_IV_LEN];
			this.secureRandom.nextBytes(iv);
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
			byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(encrypted);
		} catch (Exception e) {
			log.error("[AES] 암호화 실패", e);
			throw new IllegalStateException("암호화 실패", e);
		}
	}

	public String decrypt(String encryptedText) {
		if (encryptedText == null || encryptedText.isBlank())
			return encryptedText;
		// ":" 없으면 평문 → 그대로 반환
		if (!encryptedText.contains(":"))
			return encryptedText;
		try {
			String[] parts = encryptedText.split(":", 2);
			byte[] iv = Base64.getDecoder().decode(parts[0]);
			byte[] enc = Base64.getDecoder().decode(parts[1]);
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
			return new String(cipher.doFinal(enc), StandardCharsets.UTF_8);
		} catch (Exception e) {
			// 복호화 실패 시 예외 대신 원문 반환 (URL 등 평문에 ":" 포함된 경우 방어)
			log.warn("[AES] 복호화 실패, 원문 반환 — 평문 컬럼에 AesTypeHandler가 적용된 것으로 의심됨. value={}",
					encryptedText.substring(0, Math.min(30, encryptedText.length())));
			return encryptedText;
		}
	}

	public boolean isEncrypted(String value) {
		return value != null && value.contains(":");
	}
}
