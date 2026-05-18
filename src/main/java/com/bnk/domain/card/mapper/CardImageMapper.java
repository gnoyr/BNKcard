package com.bnk.domain.card.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.bnk.domain.card.model.CardImage;
@Mapper
public interface CardImageMapper {

    /** 카드 단건의 이미지 전체 조회 (sortOrder ASC) */
    List<CardImage> findByCardId(Long cardId);
 
    /** 카드 단건의 특정 타입 이미지 조회 (FRONT / BACK / THUMBNAIL / DETAIL) */
    CardImage findByCardIdAndType(Long cardId, String imageType);
 
    /** 여러 카드의 FRONT 이미지 한 번에 조회 (배너/목록 최적화용) */
    List<CardImage> findFrontImagesByCardIds(List<Long> cardIds);
}
