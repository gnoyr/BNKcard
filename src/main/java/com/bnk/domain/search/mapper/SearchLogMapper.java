package com.bnk.domain.search.mapper;

import com.bnk.domain.search.dto.request.SearchStatsRequest;
import com.bnk.domain.search.model.SearchLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface SearchLogMapper {

    int insertSearchLog(SearchLog searchLog);

    /** 최근 7일 인기 검색어 TOP N */
    List<Map<String, Object>> findPopularKeywords(@Param("topN") int topN);

    /** 관리자 검색 통계 */
    List<Map<String, Object>> findSearchStats(SearchStatsRequest request);
}
