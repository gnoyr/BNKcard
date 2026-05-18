package com.bnk.domain.terms.model;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class Terms {
    private Long termsId;
    private Long termsMasterId;
    private String version;
    private String contentHtml;
    private String requiredYn;
    private String reconsentRequiredYn;
    private String status;              // DRAFT/REVIEW/APPROVED/PUBLISHED/EXPIRED
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String documentHash;
    private String internalNote;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // TERMS_MASTERS JOIN
    private String title;
    private String termsType;
}
