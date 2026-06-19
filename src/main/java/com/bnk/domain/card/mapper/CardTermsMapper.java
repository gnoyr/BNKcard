package com.bnk.domain.card.mapper;

import com.bnk.domain.card.model.CardTerms;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CardTermsMapper {

    /** 카드별 연결된 약관 목록 (PUBLISHED 상태만, display_order 순) */
    List<CardTerms> findByCardId(@Param("cardId") Long cardId);
}