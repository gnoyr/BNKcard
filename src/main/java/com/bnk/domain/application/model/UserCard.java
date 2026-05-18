package com.bnk.domain.application.model;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class UserCard {
    private Long userCardId;
    private Long userId;
    private Long cardId;
    private Long applicationId;
    private String maskedCardNumber;
    private String cardNickname;
    private LocalDate issueDate;
    private LocalDate expireDate;
    private String cardStatus;          // ACTIVE/LOST/STOPPED/EXPIRED/REISSUED
    private String usableYn;
    private Long dailyLimitAmount;
    private Long monthlyLimitAmount;
    private String overseasEnabledYn;
    private String contactlessEnabledYn;
    private Long issuedBy;
    private LocalDateTime issuedAt;
    private LocalDateTime updatedAt;
    private String deletedYn;
}
