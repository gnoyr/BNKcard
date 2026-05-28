package com.bnk.domain.card.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 카드 혜택 수정 요청 DTO — PUT /api/admin/cards/{cardId}/benefits
 * 기존 혜택 전체 삭제 후 재INSERT 방식.
 */
@Getter
@NoArgsConstructor
public class BenefitUpdateRequest {

    @NotBlank(message = "변경 사유는 필수입니다.")
    @Size(max = 2000)
    private String changeSummary;

    /** null 또는 빈 리스트이면 혜택 전체 삭제 */
    @Valid
    private List<BenefitCreateRequest> benefits;
}
