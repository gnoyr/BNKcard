package com.bnk.domain.ai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.bnk.domain.ai.model.CardVector;

@Mapper
public interface CardVectorMapper {
	List<CardVector> findAllCardsWithBenefits();
	// CardVectorMapper.java 에 추가
	CardVector findCardWithBenefitsById(Long cardId);
}
