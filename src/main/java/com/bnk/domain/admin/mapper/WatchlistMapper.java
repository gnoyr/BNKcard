package com.bnk.domain.admin.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bnk.domain.admin.model.Watchlist;

@Mapper
public interface WatchlistMapper {

	/**
	 * CI값 SHA-256 해시로 단건 조회. AES-256-GCM은 IV가 매번 달라 암호문 직접 비교 불가 →
	 * ci_value_hash(결정론적 해시) 컬럼으로 인덱스 조회.
	 */
	Optional<Watchlist> findByCiValueHash(@Param("ciValueHash") String ciValueHash);

	/**
	 * 이름 + 생년월일 해시로 조회. birth_date도 AES 암호화 저장 → birth_date_hash 컬럼으로 인덱스 조회. name은
	 * 평문 저장이므로 직접 비교.
	 */
	List<Watchlist> findByNameAndBirthDateHash(@Param("name") String name,
			@Param("birthDateHash") String birthDateHash);

	/** 전체 목록 조회 (관리자 화면용) */
	List<Watchlist> findAll();

	/** 등록 — ci_value_hash, birth_date_hash 함께 저장 */
	int insert(Watchlist watchlist);

	/** 삭제 (논리 삭제) */
	int delete(@Param("watchlistId") Long watchlistId, @Param("registeredBy") Long registeredBy);
}