package com.bnk.domain.card.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 카드 이미지 수정 요청 DTO — PUT /api/admin/cards/{cardId}/images
 * 기존 이미지 전체 삭제 후 재INSERT 방식.
 */
@Getter
@NoArgsConstructor
public class ImageUpdateRequest {

    @NotBlank(message = "수정 사유는 필수입니다.")
    @Size(max = 2000)
    private String changeSummary;

    /** null 또는 빈 리스트이면 이미지 전체 삭제 */
    @Valid
    private List<ImageCreateRequest> images;
}
