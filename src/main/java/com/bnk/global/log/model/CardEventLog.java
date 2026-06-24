package com.bnk.global.log.model;

import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CardEventLog {
    private Long   logId;        // 부모 PK와 동일
    private String cardId;
    private String actionDetail;
    private String resultCode;
    private String errorMessage;
}