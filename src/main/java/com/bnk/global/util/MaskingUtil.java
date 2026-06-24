package com.bnk.global.util;

import org.springframework.stereotype.Component;

@Component
public class MaskingUtil {

    /** ab***@domain.com */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];
        if (local.length() <= 2) return local.charAt(0) + "***@" + domain;
        return local.substring(0, 2) + "*".repeat(local.length() - 2) + "@" + domain;
    }

    /**
     * 전화번호 마스킹
     * 010-1234-5678 또는 01012345678 → 010-****-5678
     */
    public static String maskPhone(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() == 11) {
            return digits.substring(0, 3) + "-****-" + digits.substring(7);
        }
        return phone;
    }

    /**
     * 전화번호 포맷 — DB 저장 전 변환
     * 01012345678  (11자리) → 010-1234-5678
     * 0101234567   (10자리) → 010-123-4567
     * 이미 포맷된 010-1234-5678 → 그대로 반환
     * null / 기타 → 그대로 반환
     */
    public static String formatPhone(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() == 11) {
            // 010-XXXX-XXXX
            return digits.substring(0, 3) + "-"
                 + digits.substring(3, 7) + "-"
                 + digits.substring(7);
        }
        if (digits.length() == 10) {
            // 010-XXX-XXXX
            return digits.substring(0, 3) + "-"
                 + digits.substring(3, 6) + "-"
                 + digits.substring(6);
        }
        return phone; // 알 수 없는 형식은 그대로
    }

    /** 홍길동 → 홍*동, 김철 → 김* */
    public static String maskName(String name) {
        if (name == null || name.length() < 2) return name;
        if (name.length() == 2) return name.charAt(0) + "*";
        return name.charAt(0) + "*".repeat(name.length() - 2) + name.charAt(name.length() - 1);
    }
    
    /**
     * 카드번호 마스킹
     * 1234567890123456 → 1234-56**-****-3456
     */
    public static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() != 16) return cardNumber;
        return cardNumber.substring(0, 4) + "-"
             + cardNumber.substring(4, 6) + "**-"
             + "****-"
             + cardNumber.substring(12);
    }
}
