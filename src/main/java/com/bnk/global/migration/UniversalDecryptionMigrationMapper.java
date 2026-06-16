package com.bnk.global.migration;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 전 테이블 범용 복호화 마이그레이션 Mapper.
 *
 * Oracle USER_TAB_COLUMNS 메타데이터로 모든 VARCHAR2 컬럼을 동적 조회하고,
 * 암호문 패턴(콜론 정확히 1개)이 있는 값만 읽어 복호화 후 덮어씀.
 *
 * TypeHandler 없이 raw String 직접 처리.
 */
@Mapper
public interface UniversalDecryptionMigrationMapper {

    /**
     * Oracle 메타데이터에서 이 스키마의 모든 테이블+VARCHAR2 컬럼 조회.
     * 반환 키: TABLE_NAME, COLUMN_NAME, PK_COLUMN (각 테이블 PK 컬럼명)
     */
    List<Map<String, String>> findAllVarcharColumns();

    /**
     * 특정 테이블·컬럼에서 AES 암호문 패턴(콜론 정확히 1개) 을 가진 행 수 조회.
     * SQL Injection 방지: tableName / columnName은 메타데이터에서 온 값만 사용.
     */
    int countEncryptedRows(
            @Param("tableName")  String tableName,
            @Param("columnName") String columnName,
            @Param("pkColumn")   String pkColumn);

    /**
     * 암호문 패턴 행 배치 조회. pk값 + 암호문 컬럼값 반환.
     */
    List<Map<String, Object>> findEncryptedRows(
            @Param("tableName")  String tableName,
            @Param("columnName") String columnName,
            @Param("pkColumn")   String pkColumn,
            @Param("limit")      int    limit,
            @Param("offset")     int    offset);

    /**
     * 복호화된 값으로 단건 UPDATE.
     */
    int updateDecryptedValue(
            @Param("tableName")    String tableName,
            @Param("columnName")   String columnName,
            @Param("pkColumn")     String pkColumn,
            @Param("pkValue")      Object pkValue,
            @Param("plainValue")   String plainValue);
}