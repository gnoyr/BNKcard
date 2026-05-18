package com.bnk.domain.user.model;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long userId;
    private String email;
    private String passwordHash;        // 응답 DTO에서 반드시 제외
    private String name;
    private String phone;
    private LocalDate birthDate;
    private String ciValue;             // 응답 DTO에서 반드시 제외
    private String job;
    private String incomeLevelCode;
    private Integer creditScore;
    private String statusCode;          // ACTIVE / SUSPENDED / DORMANT / WITHDRAWN
    private Integer loginFailCount;
    private LocalDateTime lockedUntil;
    private LocalDateTime lastLoginAt;
    private LocalDateTime lastPasswordChangedAt;
    private String isEmailVerified;     // Y / N
    private String isPhoneVerified;     // Y / N
    private String pushEnabled;         // Y / N
    private String marketingAgree;      // Y / N
    private String privacyAgree;        // Y / N
    private LocalDateTime dormantAt;
    private LocalDateTime withdrawnAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String deletedYn;
}
