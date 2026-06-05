package com.bnk.domain.user.dto.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 보유 카드 MyBatis 쿼리 결과 매핑 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnedCardRow {
	private Long userCardId;
	private Long cardId;
	private String cardName;
	private String cardImageUrl;
	private String issuedAt;
}
