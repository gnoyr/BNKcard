package com.bnk.domain.application.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
public class UserCard {
    private Long      userCardId;
    private Long      userId;
    private Long      cardId;
    private Long      creditAppId;              // 신용카드 발급 시. 체크카드면 NULL
    private Long      checkAppId;               // 체크카드 발급 시. 신용카드면 NULL
    private String    cardPasswordHash;         // 카드 결제 비밀번호 해시
    private String    maskedCardNumber;
    private String    cardNickname;
    private LocalDate issueDate;
    private LocalDate expireDate;
    private String    cardStatus;               // ACTIVE/LOST/STOPPED/EXPIRED/REISSUED
    private String    usableYn;
    private Long      dailyLimitAmount;
    private Long      monthlyLimitAmount;
    private String    overseasEnabledYn;
    private String    contactlessEnabledYn;
    private Long      issuedBy;
    private LocalDateTime issuedAt;
    private LocalDateTime updatedAt;
    private String    deletedYn;
    private LocalDateTime deletedAt;
}