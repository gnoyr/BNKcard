package com.bnk.global.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-CBC 암복호화 유틸.
 * application.properties: aes.secret-key= (32바이트 키)
 */
@Component
public class AesUtil {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int IV_SIZE = 16;

    private final SecretKeySpec keySpec;

    public AesUtil(@Value("${aes.secret-key}") String secretKey) {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        // 32바이트(256bit)로 맞춤
        byte[] key = new byte[32];
        System.arraycopy(keyBytes, 0, key, 0, Math.min(keyBytes.length, 32));
        this.keySpec = new SecretKeySpec(key, "AES");
    }

    /**
     * 암호화: "Base64(IV) + ":" + Base64(암호문)" 형태로 저장
     */
    public String encrypt(String plain) {
        try {
            byte[] iv = new byte[IV_SIZE];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

            String ivBase64 = Base64.getEncoder().encodeToString(iv);
            String encBase64 = Base64.getEncoder().encodeToString(encrypted);
            return ivBase64 + ":" + encBase64;
        } catch (Exception e) {
            throw new IllegalStateException("AES 암호화 실패", e);
        }
    }

    /**
     * 복호화: "Base64(IV) + ":" + Base64(암호문)" 분리 후 복호
     */
    public String decrypt(String cipher) {
        try {
            String[] parts = cipher.split(":");
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] encrypted = Base64.getDecoder().decode(parts[1]);

            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher c = Cipher.getInstance(ALGORITHM);
            c.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decrypted = c.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("AES 복호화 실패", e);
        }
    }
}
