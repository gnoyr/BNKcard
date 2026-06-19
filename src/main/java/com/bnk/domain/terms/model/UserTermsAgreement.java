package com.bnk.domain.terms.model;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class UserTermsAgreement {
    private Long agreementId;
    private Long userId;
    private Long termsId;
    private String agreedYn;                // Y / N
    private String agreementAction;         // AGREE / WITHDRAW / REAGREE
    private String agreedVersion;
    private String agreementChannel;        // WEB / MOBILE / ADMIN / CALL_CENTER
    private String agreementSource;         // SIGNUP / CREDIT_CARD_APPLY / CHECK_CARD_APPLY / EVENT
    private String agreedContentSnapshot;   // CLOB — 법적 증거용 원문 스냅샷
    private LocalDateTime agreedAt;
    private String ipAddress;
    private String userAgent;
    private Long   creditAppId;        // 신용카드 신청 시
    private Long   checkAppId;         // 체크카드 신청 시
}
