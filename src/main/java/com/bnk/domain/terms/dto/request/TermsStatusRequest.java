package com.bnk.domain.terms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TermsStatusRequest {

    @NotBlank(message = "변경할 상태는 필수입니다.")
    @Pattern(
        regexp = "^(REVIEW|APPROVED|PUBLISHED|EXPIRED)$",
        message = "상태는 REVIEW, APPROVED, PUBLISHED, EXPIRED 중 하나여야 합니다."
    )
    private String newStatus;

    @Size(max = 1000)
    private String changedReason;
}
