package com.bnk.domain.user.dto.response;

import com.bnk.domain.user.model.User;
import com.bnk.global.util.MaskingUtil;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {

    private Long userId;
    private String name;

    // ── 원본 (마이페이지 본인 확인용) ──────────────
    private String email;           // 추가: 원본 이메일
    private String phone;           // 추가: 원본 전화번호

    // ── 마스킹 (아이디 찾기 등 외부 노출용) ────────
    private String maskedEmail;     // ab***@domain.com
    private String maskedPhone;     // 010-****-5678

    private LocalDate birthDate;
    private String job;
    private String incomeLevelCode;
    private Integer creditScore;
    private String statusCode;
    private String isEmailVerified;
    private String isPhoneVerified;
    private String pushEnabled;
    private String marketingAgree;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    // password_hash · ci_value 필드 없음

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())                          // 추가
                .phone(user.getPhone())                          // 추가
                .maskedEmail(MaskingUtil.maskEmail(user.getEmail()))
                .maskedPhone(MaskingUtil.maskPhone(user.getPhone()))
                .birthDate(user.getBirthDate())
                .job(user.getJob())
                .incomeLevelCode(user.getIncomeLevelCode())
                .creditScore(user.getCreditScore())
                .statusCode(user.getStatusCode())
                .isEmailVerified(user.getIsEmailVerified())
                .isPhoneVerified(user.getIsPhoneVerified())
                .pushEnabled(user.getPushEnabled())
                .marketingAgree(user.getMarketingAgree())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
