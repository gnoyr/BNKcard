package com.bnk.domain.card.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bnk.domain.card.model.CardImage;
import com.bnk.domain.card.model2.Card;

@Mapper
public interface CardMapper2 {
	
		// 전체 카드 목록 조회 ---------------------------------------
		List<Card> getCards();
		
		// 카드 1개 조회
	    Card getCardDetail(@Param("cardId") Long cardId);
		
		// 카드 등록
		void insertCard(Card card);
		
		// 카드 수정
		void updateCard(Card card);
		
		// 카드 삭제 (status INACTIVE로 변경)
		void deleteCard(@Param("cardId") Long cardId);
		
		// publish_end_at < SYSTIMESTAMP인 카드 목록 조회
		List<Card> getExpiredCards();
		
		// 기존 getExpiredCards() 아래에 추가

		/** publish_start_at <= SYSTIMESTAMP 이고 APPROVED 상태인 카드 목록 조회 */
		List<Card> getApprovedReadyCards();

		/** 조회된 카드 일괄 PUBLISHED UPDATE */
		void publishCards(@Param("cardIds") List<Long> cardIds);

		/** PUBLISHED 전환 시 버전도 PUBLISHED로 */
		void publishCardVersions(@Param("cardIds") List<Long> cardIds);
		
		
		// 조회된 카드 일괄 EXPIRED UPDATE
		void expireCards(@Param("cardIds") List<Long> cardIds);
		// 카드 일괄 EXPIRED 시 버전도 ARCHIVED로
		void expireCardVersion(@Param("cardIds") List<Long> cardIds);  
		
		// 카드 상태 변경
		void updateCardStatus(@Param("cardId") Long cardId,
							  @Param("cardStatus") String cardStatus);
		
		

}
