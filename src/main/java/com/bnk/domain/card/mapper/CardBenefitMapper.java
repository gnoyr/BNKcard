package com.bnk.domain.card.mapper;

import com.bnk.domain.card.model.CardBenefit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CardBenefitMapper {

    /** 카드별 혜택 목록 (CARD_CATEGORIES JOIN, visible_yn='Y') */
    List<CardBenefit> findByCardId(@Param("cardId") Long cardId);

    /** 카드 ID 목록 일괄 조회 (비교·시뮬레이션 기능) */
    List<CardBenefit> findByCardIds(@Param("cardIds") List<Long> cardIds);

    int insertBenefit(CardBenefit benefit);

    /** 카드 재등록 시 기존 혜택 전체 삭제 후 재INSERT */
    int deleteByCardId(@Param("cardId") Long cardId);
}
