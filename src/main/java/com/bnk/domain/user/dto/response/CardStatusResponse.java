package com.bnk.domain.user.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 보유 카드 및 신청 현황 응답 (RQ-F17)
 */
@Getter
@Builder
public class CardStatusResponse {

	/** 발급 완료 카드 목록 (USER_CARDS.deleted_yn='N') */
	private List<OwnedCardDto> ownedCards;

	/** 신청 진행 현황 (CARD_APPLICATIONS) */
	private List<CardApplicationDto> applications;

	@Getter
	@Builder
	public static class OwnedCardDto {
		private Long userCardId;
		private Long cardId;
		private String cardName;
		private String cardImageUrl; // CARD_IMAGES.image_url (image_type='FRONT')
		private String issuedAt;
	}

	@Getter
	@Builder
	public static class CardApplicationDto {
		private Long applicationId;
		private Long cardId;
		private String cardName;
		private String cardImageUrl; // CARD_IMAGES.image_url (image_type='THUMBNAIL')
		/** REQUESTED | REVIEWING | APPROVED | REJECTED */
		private String applicationStatus;
		private String appliedAt;
	}
}