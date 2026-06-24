package com.bnk.domain.application.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class PaymentSnapshotDto {
    private String  cardBrand;          // VISA / MASTER / AMEX / LOCAL
    private String  cardDesignId;       // 선택한 카드 디자인 ID
    private Integer paymentDay;         // 결제일 (1~31)
    private String  combinedTransitYn;  // 후불교통 결합 여부 Y/N
    private String  txAlertType;        // SMS / PUSH / NONE
    private String  statementMethod;    // EMAIL / APP / PAPER
}


/*
payment_snapshot { card_brand, payment_day, combined_transit_yn, tx_alert_type, statement_method }
*/
