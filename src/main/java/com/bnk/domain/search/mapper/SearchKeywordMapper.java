package com.bnk.domain.search.mapper;

import com.bnk.domain.search.model.SearchKeyword;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface SearchKeywordMapper {

    /** 추천 검색어: use_yn='Y' ORDER BY display_order ASC LIMIT 10 */
    List<SearchKeyword> findSuggestKeywords();

    List<SearchKeyword> findByKeywordLike(@Param("keyword") String keyword);

    Optional<SearchKeyword> findByKeyword(@Param("keyword") String keyword);

    Optional<SearchKeyword> findById(@Param("keywordId") Long keywordId);

    int insertKeyword(SearchKeyword keyword);

    int updateKeyword(SearchKeyword keyword);

    int deleteKeyword(@Param("keywordId") Long keywordId,
                      @Param("deletedBy") Long deletedBy);

    // ── CARD_KEYWORDS ───────────────────────────────────
    /** UNIQUE(card_id, keyword_id) 중복 무시 INSERT */
    int insertCardKeyword(@Param("cardId") Long cardId,
                         @Param("keywordId") Long keywordId,
                         @Param("createdBy") Long createdBy);

    boolean existsCardKeyword(@Param("cardId") Long cardId,
                              @Param("keywordId") Long keywordId);

    List<Long> findCardIdsByKeywordIds(@Param("keywordIds") List<Long> keywordIds);
}
