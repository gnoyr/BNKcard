package com.bnk.domain.card.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CardStatusRequest {

    @NotBlank(message = "변경할 상태는 필수입니다.")
    @Pattern(regexp = "APPROVED|PUBLISHED|STOPPED|EXPIRED",
             message = "APPROVED, PUBLISHED, STOPPED, EXPIRED 중 하나여야 합니다.")
    private String cardStatus;

    private String changedReason;  // 상태 변경 사유 (STOPPED 시 필수 권장)
}