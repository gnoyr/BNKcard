package com.bnk.domain.application.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
public class UserCard {

    // ── 식별자 ──────────────────────────────────────────────
    private Long      userCardId;
    private Long      userId;
    private Long      versionId;
    private Long      creditAppId;          // 신용카드 발급 시. 체크카드 시 null
    private Long      checkAppId;           // 체크카드 발급 시. 신용카드 시 null

    // ── 카드번호 ─────────────────────────────────────────────
    private String    encryptedCardNumber;  // AES 암호화된 16자리 카드번호
    private String    maskedCardNumber;     // 화면 표시용 마스킹 번호

    // ── 유효기간 / 상태 ──────────────────────────────────────
    private LocalDate issueDate;
    private LocalDate expireDate;
    private String    cardStatus;           // ACTIVE / LOST / STOPPED / EXPIRED / REISSUED
    private String    usableYn;
    private String    cardPasswordHash;

    // ── 연결 계좌 ────────────────────────────────────────────
    private Long      linkedAccountId;      // 체크카드 필수, 신용카드 선택

    // ── 한도 ──────────────────────────────────
    private Long      dailyLimitAmount;    // 발급 시 100만원 기본
    private Long      monthlyLimitAmount;  // 신용카드=승인한도, 체크카드=null

    // ── payment_snapshot 항목 ────────────────────────────────
    private String    cardBrand;            // VISA / MASTER / LOCAL / AMEX / UPI
    private Long      cardDesignId;         // FK: CARD_IMAGES
    private Integer   paymentDay;           // 월 결제일 (1~31)
    private String    combinedTransitYn;    // 교통카드 결합 여부 Y/N
    private String    txAlertType;          // SMS / PUSH / NONE
    private String    statementMethod;      // EMAIL / APP / PAPER

    // ── 해외 / 비접촉 설정 ───────────────────────────────────
    private String    overseasEnabledYn;    // DEFAULT 'Y', 발급 후 변경 가능
    private String    contactlessEnabledYn; // DEFAULT 'Y', 발급 후 변경 가능

    // ── 카드 별칭 ────────────────────────────────────────────
    private String    cardNickname;

    // ── 감사(Audit) ──────────────────────────────────────────
    private Long          issuedBy;
    private LocalDateTime issuedAt;
    private LocalDateTime updatedAt;
    private String        deletedYn;
    private LocalDateTime deletedAt;
}