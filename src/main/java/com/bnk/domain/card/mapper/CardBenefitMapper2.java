package com.bnk.domain.card.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bnk.domain.card.model2.CardBenefit;

@Mapper
public interface CardBenefitMapper2 {
	
		// 카드별 혜택 목록 ---------------------------------------
		List<CardBenefit> getBenefit(@Param("cardId") Long cardId);

	    // 혜택 리스트 등록
		void insertBenefits(@Param("benefitList") List<CardBenefit> benefits);

	    // 혜택 수정
		void updateBenefit(CardBenefit benefit);

	    // 혜택 삭제
	    void deleteBenefit(@Param("benefitId") Long benefitId);

	    // 카드 전체 혜택 삭제 (카드 삭제 시)
	    void deleteBenefitByCardId(@Param("cardId") Long cardId);
	
}
