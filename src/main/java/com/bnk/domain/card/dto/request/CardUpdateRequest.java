package com.bnk.domain.card.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

/** 카드 수정 요청 — CardCreateRequest와 필드 동일, changeSummary 필수 */
@Getter @NoArgsConstructor
public class CardUpdateRequest extends CardCreateRequest {
}
