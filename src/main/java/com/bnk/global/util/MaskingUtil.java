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

    /** 01012345678 → 010-****-5678 */
    public static String maskPhone(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() == 11) {
            return digits.substring(0, 3) + "-****-" + digits.substring(7);
        }
        return phone;
    }

    /** 홍길동 → 홍*동, 김철 → 김* */
    public static String maskName(String name) {
        if (name == null || name.length() < 2) return name;
        if (name.length() == 2) return name.charAt(0) + "*";
        return name.charAt(0) + "*".repeat(name.length() - 2) + name.charAt(name.length() - 1);
    }
}
