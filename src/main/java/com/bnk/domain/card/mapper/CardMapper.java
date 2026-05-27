package com.bnk.domain.card.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bnk.domain.card.dto.request.AdminCardSearchRequest;
import com.bnk.domain.card.dto.request.CardSearchRequest;
import com.bnk.domain.card.model.Card;

@Mapper
public interface CardMapper {

    /** 사용자 카드 목록 + 동적 검색 */
    List<Card> findAll(CardSearchRequest request);

    long countAll(CardSearchRequest request);

    Card findById(@Param("cardId") Long cardId);

    List<Card> findTop3ByViewCount();

    List<Card> findTop3ByCategoryId(@Param("categoryId") Long categoryId);

    /** 관리자 다중 조건 동적 검색 (RQ-B13) */
    List<Card> findAdminCards(AdminCardSearchRequest request);

    long countAdminCards(AdminCardSearchRequest request);

    int insertCard(Card card);

    int updateCard(Card card);

    int updateCardFromSnapshot(@Param("cardId") Long cardId,
                               @Param("snapshotJson") String snapshotJson);

    int incrementViewCount(@Param("cardId") Long cardId);

    int incrementApplicationCount(@Param("cardId") Long cardId);

    int updateCardStatus(@Param("cardId") Long cardId,
                         @Param("cardStatus") String cardStatus);
    
    
    
}
