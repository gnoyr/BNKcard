package com.bnk.domain.card.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bnk.domain.card.model2.Card;
import com.bnk.domain.card.model2.CardImage;

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

		// 조회된 카드 일괄 EXPIRED UPDATE
		void expireCards(@Param("cardIds") List<Long> cardIds);
		// 카드 일괄 EXPIRED 시 버전도 ARCHIVED로
		void expireCardVersion(@Param("cardIds") List<Long> cardIds);  
		
		// 카드 상태 변경
		void updateCardStatus(@Param("cardId") Long cardId,
							  @Param("cardStatus") String cardStatus);
		
		
		// 카드 이미지 조회 ---------------------------------------
		List<CardImage> getImage(@Param("cardId") Long cardId);
	
		// 카드 이미지리스트 등록
		void insertImage(@Param("imageList") List<CardImage> images);
		
		// 이미지 수정
		void updateImage(CardImage image);
		
		// 이미지 삭제
		void deleteImage(@Param("imageId") Long imageId);	
		
		// 카드 전체 이미지 삭제 (카드 삭제 시)
	    void deleteImageByCardId(@Param("cardId") Long cardId);

		

}
