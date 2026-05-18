package com.bnk.domain.card.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.bnk.domain.card.model2.CardStatusHistory;

@Mapper
public interface CardStatusHistoryMapper2 {
	
	// 상태 변경 시마다 INSERT
    void insertCardStatusHistory(CardStatusHistory history);
}
