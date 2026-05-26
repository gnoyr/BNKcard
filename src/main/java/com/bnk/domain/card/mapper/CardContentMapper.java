package com.bnk.domain.card.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bnk.domain.card.model.CardContent;

@Mapper
public interface CardContentMapper {
	List<CardContent> findByCardId(@Param("cardId") Long cardId);
	
	int insertContents(@Param("contentList") List<CardContent> contentList);
    int deleteByCardId(@Param("cardId") Long cardId);
}
