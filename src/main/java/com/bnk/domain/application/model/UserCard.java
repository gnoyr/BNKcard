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
    private Long      versionId;
    private Long      creditAppId;
    private Long      checkAppId;
    private String    cardPasswordHash;
    private String    cardNumber;
    private LocalDate issueDate;
    private LocalDate expireDate;
    private String    cardStatus;
    private String    usableYn;
    private String    overseasEnabledYn;
    private String    contactlessEnabledYn;
    private LocalDateTime issuedAt;
    private LocalDateTime updatedAt;
    private String    deletedYn;
    private LocalDateTime deletedAt;
}