package com.bnk.domain.admin.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bnk.domain.admin.model.Watchlist;

@Mapper
public interface WatchlistMapper {

	/** CI값으로 Watchlist 조회 (가장 정확) */
	Optional<Watchlist> findByCiValue(@Param("ciValue") String ciValue);

	/** 이름 + 생년월일로 조회 (CI값 없는 경우 fallback) */
	List<Watchlist> findByNameAndBirthDate(@Param("name") String name, @Param("birthDate") String birthDate);

	/** 전체 목록 조회 (관리자) */
	List<Watchlist> findAll();

	/** 등록 */
	int insert(Watchlist watchlist);

	/** 삭제 (논리 삭제) */
	int delete(@Param("watchlistId") Long watchlistId, @Param("registeredBy") Long registeredBy);
}
