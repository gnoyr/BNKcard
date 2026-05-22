package com.bnk.domain.terms.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class TermsAdminResponse {
    private Long          termsId;
    private Long          termsMasterId;
    private String        title;           // TERMS_MASTERS.title JOIN
    private String        termsType;       // TERMS_MASTERS.terms_type JOIN
    private String        version;
    private String        status;          // DRAFT/REVIEW/APPROVED/PUBLISHED/EXPIRED
    private String        requiredYn;
    private String        reconsentRequiredYn;
    private LocalDate     effectiveFrom;
    private LocalDate     effectiveTo;
    private LocalDateTime createdAt;
    private List<TermsFileResponse> files; // 상세 조회 시만 포함
}