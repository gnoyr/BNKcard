package com.bnk.domain.user.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.bnk.domain.application.model.UserCard;

import lombok.Builder;
import lombok.Getter;

/**
 * 보유 카드 단건 상세 응답 (마이페이지 카드 관리 화면용).
 *
 * USER_CARDS 컬럼 중 사용자에게 노출 가능한 항목만 담는다.
 * (encrypted_card_number, card_password_hash 등 민감 컬럼 제외)
 */
@Getter
@Builder
public class OwnedCardDetailResponse {

    private Long userCardId;
    private Long userId;
    private Long versionId;
    private Long creditAppId;
    private Long checkAppId;

    private String maskedCardNumber;

    private LocalDate issueDate;
    private LocalDate expireDate;
    private String    cardStatus;
    private String    usableYn;

    private Long linkedAccountId;

    private Long dailyLimitAmount;
    private Long monthlyLimitAmount;

    private String  cardBrand;
    private Long    cardDesignId;
    private Integer paymentDay;
    private String  combinedTransitYn;
    private String  txAlertType;
    private String  statementMethod;

    private String overseasEnabledYn;
    private String contactlessEnabledYn;

    private String cardNickname;

    private LocalDateTime issuedAt;
    private LocalDateTime updatedAt;

    public static OwnedCardDetailResponse from(UserCard c) {
        return OwnedCardDetailResponse.builder()
                .userCardId(c.getUserCardId())
                .userId(c.getUserId())
                .versionId(c.getVersionId())
                .creditAppId(c.getCreditAppId())
                .checkAppId(c.getCheckAppId())
                .maskedCardNumber(c.getMaskedCardNumber())
                .issueDate(c.getIssueDate())
                .expireDate(c.getExpireDate())
                .cardStatus(c.getCardStatus())
                .usableYn(c.getUsableYn())
                .linkedAccountId(c.getLinkedAccountId())
                .dailyLimitAmount(c.getDailyLimitAmount())
                .monthlyLimitAmount(c.getMonthlyLimitAmount())
                .cardBrand(c.getCardBrand())
                .cardDesignId(c.getCardDesignId())
                .paymentDay(c.getPaymentDay())
                .combinedTransitYn(c.getCombinedTransitYn())
                .txAlertType(c.getTxAlertType())
                .statementMethod(c.getStatementMethod())
                .overseasEnabledYn(c.getOverseasEnabledYn())
                .contactlessEnabledYn(c.getContactlessEnabledYn())
                .cardNickname(c.getCardNickname())
                .issuedAt(c.getIssuedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
