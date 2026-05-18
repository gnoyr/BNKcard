package com.bnk.domain.card.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bnk.domain.card.model.CardCategory;

@Mapper
public interface CardCategoryMapper2 {
    
    // 전체 카테고리 ---------------------------------------
    List<CardCategory> getAllCategories();

    // 카테고리 단건 조회
    CardCategory getCategory(@Param("categoryId") Long categoryId);

    // 카테고리 등록
    void insertCategory(CardCategory category);

    // 카테고리 수정
    void updateCategory(CardCategory category);

    // 카테고리 삭제
    void deleteCategory(@Param("categoryId") Long categoryId);
}
