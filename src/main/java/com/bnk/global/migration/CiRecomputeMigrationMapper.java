package com.bnk.global.migration;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 기존 회원 CI 재생성 마이그레이션 Mapper.
 * XML: resources/mappers/migration/CiRecomputeMigrationMapper.xml
 */
@Mapper
public interface CiRecomputeMigrationMapper {

    /** 전체 회원의 (userId, name, 복호화된 birth_date/phone) 조회 */
    List<CiMigrationRow> selectAllForCi();
}
