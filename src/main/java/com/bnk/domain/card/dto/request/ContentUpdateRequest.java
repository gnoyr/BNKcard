package com.bnk.domain.card.dto.request;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 카드 콘텐츠 수정 요청 DTO — PUT /api/admin/cards/{cardId}/contents
 * 기존 콘텐츠 전체 삭제 후 재INSERT 방식.
 */
@Getter
@NoArgsConstructor
public class ContentUpdateRequest {

    /** null 또는 빈 리스트이면 콘텐츠 전체 삭제 */
    @Valid
    private List<ContentCreateRequest> contents;
}
