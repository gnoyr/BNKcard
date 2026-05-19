package com.bnk.domain.card.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bnk.domain.card.model2.CardContent;

@Mapper
public interface CardContentMapper {
	List<CardContent> findByCardId(@Param("cardId") Long cardId);
}
