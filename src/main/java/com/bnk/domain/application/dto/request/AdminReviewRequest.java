package com.bnk.domain.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminReviewRequest {

    @NotBlank(message = "심사 결과는 필수입니다.")
    private String applicationStatus;   // APPROVED / REJECTED

    private Long approvedLimit;

    @Size(max = 1000)
    private String rejectionReason;     // REJECTED 시 필수 (서비스단 검증)
}
