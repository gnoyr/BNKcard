package com.bnk.global.migration;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * WATCHLIST 암호화 마이그레이션 전용 Mapper.
 * TypeHandler 없이 평문 그대로 읽고, 암호화된 값 그대로 씀.
 */
@Mapper
public interface WatchlistMigrationMapper {

    int countPlainCiValueWatchlists();
    List<WatchlistMigrationRow> findPlainCiValueWatchlists(
            @Param("limit")  int limit,
            @Param("offset") int offset);
    int updateWatchlistCiValue(
            @Param("watchlistId")      Long   watchlistId,
            @Param("encryptedCiValue") String encryptedCiValue);

    int countPlainBirthDateWatchlists();
    List<WatchlistMigrationRow> findPlainBirthDateWatchlists(
            @Param("limit")  int limit,
            @Param("offset") int offset);
    int updateWatchlistBirthDate(
            @Param("watchlistId")          Long   watchlistId,
            @Param("encryptedBirthDate")   String encryptedBirthDate);
}