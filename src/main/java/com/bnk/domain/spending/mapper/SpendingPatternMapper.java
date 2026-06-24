package com.bnk.domain.spending.mapper;

import com.bnk.domain.spending.model.SpendingPattern;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SpendingPatternMapper {

    /** 사용자 소비패턴 전체 조회 (CARD_CATEGORIES JOIN) */
    List<SpendingPattern> findByUserId(@Param("userId") Long userId);

    /** 우수회원 추천용 — 최대 소비 카테고리 ID */
    Long findTopCategoryIdByUserId(@Param("userId") Long userId);

    /** MERGE INTO UPSERT (user_id + category_id 기준) */
    int upsertPattern(SpendingPattern pattern);
}
