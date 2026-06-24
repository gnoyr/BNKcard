package com.bnk.global.migration;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * USER_TRUSTED_IPS 암호화 마이그레이션 전용 Mapper.
 * TypeHandler 없이 평문 그대로 읽고, 암호화된 값 그대로 씀.
 */
@Mapper
public interface TrustedIpMigrationMapper {

    int countPlainIpAddresses();
    List<TrustedIpMigrationRow> findPlainIpAddresses(
            @Param("limit")  int limit,
            @Param("offset") int offset);
    int updateIpAddress(
            @Param("trustId")           Long   trustId,
            @Param("encryptedIpAddress") String encryptedIpAddress);
}