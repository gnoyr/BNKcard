package com.bnk.domain.user.dto.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 카드 신청 현황 MyBatis 쿼리 결과 매핑 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardApplicationRow {
	private Long applicationId;
	private Long cardId;
	private String cardName;
	private String cardImageUrl;
	private String applicationStatus;
	private LocalDateTime appliedAt;
}
