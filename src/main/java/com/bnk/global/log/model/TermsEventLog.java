package com.bnk.global.log.model;

import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TermsEventLog {
    private Long   logId;
    private Long   termsId;
    private String actionDetail;
    private String errorMessage;
}