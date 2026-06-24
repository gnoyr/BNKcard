package com.bnk.domain.card.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카드 상태 강제 변경 요청 DTO — PATCH /api/admin/cards/{cardId}/status
 * 스케줄러 외 수동 처리 및 긴급 중지용.
 */
@Getter
@NoArgsConstructor
public class CardStatusRequest {

    @NotBlank(message = "변경할 카드 상태는 필수입니다.")
    @Pattern(regexp = "DRAFT|REVIEW|APPROVED|PUBLISHED|STOPPED|EXPIRED",
             message = "카드 상태는 DRAFT, REVIEW, APPROVED, PUBLISHED, STOPPED, EXPIRED 중 하나여야 합니다.")
    private String cardStatus;

    @Size(max = 500, message = "변경 사유는 500자 이하여야 합니다.")
    private String changedReason;
}
